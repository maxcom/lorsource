/*
 * Copyright 1998-2023 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.tag

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.{ElasticClient, ElasticDate}
import com.typesafe.scalalogging.StrictLogging
import org.springframework.stereotype.Service
import ru.org.linux.group.{Group, GroupDao}
import ru.org.linux.search.ElasticsearchIndexService.{COLUMN_TOPIC_AWAITS_COMMIT, MessageIndex}
import ru.org.linux.section.{Section, SectionController, SectionService}
import ru.org.linux.topic.TagTopicListController
import ru.org.linux.topic.TopicListController.ForumFilter
import ru.org.linux.util.RichFuture.RichFuture

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util
import scala.collection.immutable.SortedMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, TimeoutException}
import scala.concurrent.duration.Deadline
import scala.jdk.CollectionConverters.*

@Service
class TagService(tagDao: TagDao, elastic: ElasticClient, actorSystem: ActorSystem,
                 sectionService: SectionService, groupDao: GroupDao) extends StrictLogging {
  private implicit val akka: ActorSystem = actorSystem

  import ru.org.linux.tag.TagService.*

  private val sectionForum: Section = sectionService.getSection(Section.SECTION_FORUM)
  private val NonTechNames: Seq[String] =
    groupDao.getGroups(sectionForum).asScala.filter(g => SectionController.NonTech.contains(g.id)).map(_.urlName).toSeq

  /**
   * Получение идентификационного номера тега по названию.
   *
   * @param tag название тега
   * @return идентификационный номер
   */
  @throws(classOf[TagNotFoundException])
  def getTagId(tag: String, skipZero: Boolean = false): Int =
    tagDao.getTagId(tag, skipZero).getOrElse(throw new TagNotFoundException)

  def getTagIdOpt(tag: String): Option[Int] = tagDao.getTagId(tag)

  /**
   * Получение идентификационного номера тега по названию, либо создание нового тега.
   *
   * @param tagName название тега
   * @return идентификационный номер тега
   */
  def getOrCreateTag(tagName: String): Int = {
    tagDao.getTagId(tagName).orElse(tagDao.getTagSynonymId(tagName)).getOrElse(tagDao.createTag(tagName))
  }

  def getTagInfo(tag: String, skipZero: Boolean): Option[TagInfo] = {
    if (TagName.isGoodTag(tag)) {
      tagDao.getTagId(tag, skipZero).map(tagDao.getTagInfo)
    } else {
      None
    }
  }

  def getTagBySynonym(tagName: String): Option[TagRef] =
    tagDao.getTagSynonymId(tagName).map(tagDao.getTagInfo).map(i => tagRef(i, threshold = 0))

  def countTagTopics(tag: String, section: Option[Section], deadline: Deadline): Future[Option[Long]] = {
    val sectionFilter = section.map(s => termQuery("section", s.getUrlName))

    Future.successful(elastic).flatMap {
      _ execute {
        count(MessageIndex).query(
          boolQuery().filter(
            Seq(termQuery("is_comment", "false"),
            termQuery("tag", tag),
            termQuery(COLUMN_TOPIC_AWAITS_COMMIT, "false")) ++ sectionFilter))
      }
    }.map { r =>
      Some(r.result.count)
    }.withTimeout(deadline.timeLeft)
      .recover {
        case ex: TimeoutException =>
          logger.warn(s"Tag topics count timed out (${ex.getMessage})")
          None
        case ex =>
          logger.warn("Unable to count tag topics", ex)
          None
      }
  }

  def getNewTags(tags: Seq[String]): Seq[String] =
    tags.filterNot { tag =>
      tagDao.getTagId(tag, skipZero = true).isDefined || tagDao.getTagSynonymId(tag).isDefined
    }

  def getRelatedTags(tag: String, deadline: Deadline): Future[Seq[TagRef]] = Future.successful(elastic).flatMap {
    _ execute {
      search(MessageIndex) size 0 query
        boolQuery().filter(termQuery("is_comment", "false"), termQuery("tag", tag)) aggs (
        sigTermsAggregation("related") field "tag" backgroundFilter
          termQuery("is_comment", "false") /* broken in 6.x tcp client: includeExclude(Seq.empty, Seq(tag))*/)
    }
  }.map { r =>
    (for {
      bucket <- r.result.aggregations.significantTerms("related").buckets
    } yield {
      tagRef(bucket.key)
    }).sorted.filterNot(_.name == tag) // filtering in query is broken in elastic4s-tcp 6.2.x
  }.withTimeout(deadline.timeLeft)

  def getActiveTopTags(section: Section, group: Option[Group], filter: Option[ForumFilter],
                       deadline: Deadline): Future[Seq[TagRef]] = {
    if (group.exists(g => g.id == 4068)) {
      Future.successful(Seq.empty)
    } else {
      val groupFilter = group.map(g => termQuery("group", g.urlName))

      val additionalFilter = filter.collect {
        case ForumFilter.Tech =>
          boolQuery()
            .filter(termQuery("section", sectionForum.getUrlName))
            .not(termsQuery("group", NonTechNames))
        case ForumFilter.NoTalks =>
          boolQuery()
            .not(termQuery("group", "talks"))
      }

      val filters = Seq(
        termQuery("is_comment", "false"),
        termQuery("section", section.getUrlName),
        rangeQuery("postdate").gte(ElasticDate(LocalDate.now().atStartOfDay().minus(1, ChronoUnit.YEARS).toLocalDate))
      ) ++ groupFilter ++ additionalFilter

      Future.successful(elastic).flatMap {
        _.execute {
          search(MessageIndex).size(0).query(
            boolQuery().filter(filters)).aggs {
            sigTermsAggregation("active").size(15).field("tag").minDocCount(5).backgroundFilter(
              boolQuery().filter(
                termQuery("is_comment", "false"),
                termQuery("section", section.getUrlName),
                rangeQuery("postdate").gte(
                  ElasticDate(LocalDate.now().atStartOfDay().minus(2, ChronoUnit.YEARS).toLocalDate))))
          }
        }
      }.map { r =>
        (for {
          bucket <- r.result.aggregations.significantTerms("active").buckets
        } yield {
          tagRef(bucket.key, section)
        }).sorted
      }.withTimeout(deadline.timeLeft).recover {
        case ex: TimeoutException =>
          logger.warn(s"Active top tags search timed out (${ex.getMessage})")
          Seq.empty
        case ex =>
          logger.warn("Unable to find active top tags", ex)
          Seq.empty
      }
    }
  }

  /**
   * Получить список популярных тегов по префиксу.
   *
   * @param prefix префикс
   * @param count  количество тегов
   * @return список тегов по первому символу
   */
  def suggestTagsByPrefix(prefix: String, count: Int): Seq[String] =
    tagDao.getTopTagsByPrefix(prefix, 2, count)

  /**
   * Получить уникальный список первых букв тегов.
   *
   * @return список первых букв тегов
   */
  def getFirstLetters: Seq[String] = tagDao.getFirstLetters

  /**
   * Получить список тегов по префиксу.
   *
   * @param prefix префикс
   * @return список тегов по первому символу
   */
  def getTagsByPrefix(prefix: String, threshold: Int): Map[TagRef, Int] = {
    tagDao.getTagsByPrefix(prefix, threshold).view.map { info =>
      TagService.tagRef(info, threshold) -> info.topicCount
    }.to(SortedMap)
  }

  def getSynonymsFor(tagId: Int): Seq[TagRef] = tagDao.getSynonymsFor(tagId).map(name => TagRef(name, None))
}
object TagService {
  def tagRef(tag: TagInfo, threshold: Int = 2): TagRef = TagRef(tag.name,
    if (TagName.isGoodTag(tag.name) && tag.topicCount >= threshold) {
      Some(TagTopicListController.tagListUrl(tag.name))
    } else {
      None
    })

  def tagRef(name: String): TagRef = TagRef(name,
    if (TagName.isGoodTag(name)) {
      Some(TagTopicListController.tagListUrl(name))
    } else {
      None
    })

  def tagRef(name: String, section: Section): TagRef = TagRef(name,
    if (TagName.isGoodTag(name)) {
      Some(TagTopicListController.tagListUrl(name, section))
    } else {
      None
    })

  def namesToRefs(tags:java.util.List[String]):java.util.List[TagRef] = tags.asScala.map(tagRef).asJava

  def tagsToString(tags: util.Collection[String]): String = tags.asScala.mkString(",")
}
