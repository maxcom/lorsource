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
import ru.org.linux.group.Group
import ru.org.linux.search.ElasticsearchIndexService.{COLUMN_TOPIC_AWAITS_COMMIT, MessageIndex}
import ru.org.linux.section.Section
import ru.org.linux.topic.TagTopicListController
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
class TagService(tagDao: TagDao, elastic: ElasticClient, actorSystem: ActorSystem) extends StrictLogging {
  private implicit val akka: ActorSystem = actorSystem

  import ru.org.linux.tag.TagService.*

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

  def getTagInfo(tag: String, skipZero: Boolean): Option[TagInfo] =
    tagDao.getTagId(tag, skipZero).map(tagDao.getTagInfo)

  def getTagBySynonym(tagName: String): Option[TagRef] =
    tagDao.getTagSynonymId(tagName).map(tagDao.getTagInfo).map(i => tagRef(i, threshold = 0))

  def countTagTopics(tag: String): Future[Long] = {
    Future.successful(elastic) flatMap {
      _ execute {
        count(MessageIndex).query(
          boolQuery().filter(
            termQuery("is_comment", "false"),
            termQuery("tag", tag),
            termQuery(COLUMN_TOPIC_AWAITS_COMMIT, "false")))
      }
    } map {
      _.result.count
    }
  }

  def getNewTags(tags: util.List[String]): util.List[String] =
    tags.asScala.filterNot { tag =>
      tagDao.getTagId(tag, skipZero = true).isDefined || tagDao.getTagSynonymId(tag).isDefined
    }.asJava

  def getRelatedTags(tag: String): Future[Seq[TagRef]] = Future.successful(elastic).flatMap {
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
  }

  def getActiveTopTags(section: Section, group: Option[Group], deadline: Deadline): Future[Seq[TagRef]] = {
    val groupFilter = group.map(g => termQuery("group", g.getUrlName))

    val filters = Seq(
      termQuery("is_comment", "false"),
      termQuery("section", section.getUrlName),
      rangeQuery("postdate").gte("now/d-1y")
    ) ++ groupFilter

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
        tagRef(bucket.key)
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

  def namesToRefs(tags:java.util.List[String]):java.util.List[TagRef] = tags.asScala.map(tagRef).asJava

  def tagsToString(tags: util.Collection[String]): String = tags.asScala.mkString(",")
}
