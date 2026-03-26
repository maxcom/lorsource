/*
 * Copyright 1998-2026 Linux.org.ru
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

import org.apache.pekko.actor.ActorSystem
import com.typesafe.scalalogging.StrictLogging
import org.opensearch.client.opensearch.OpenSearchAsyncClient
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch._types.query_dsl.{BoolQuery, Query, RangeQuery, TermQuery}
import org.opensearch.client.opensearch.core.{CountRequest, SearchRequest}
import org.springframework.stereotype.Service
import ru.org.linux.group.{Group, GroupDao}
import ru.org.linux.search.OpenSearchIndexService.{COLUMN_TOPIC_AWAITS_COMMIT, MessageIndex}
import ru.org.linux.section.{Section, SectionController, SectionService}
import ru.org.linux.topic.TagTopicListController
import ru.org.linux.topic.TopicListController.ForumFilter
import ru.org.linux.util.RichFuture.RichFuture

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.collection.immutable.SortedMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Deadline
import scala.concurrent.{Future, TimeoutException}
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters._

@Service
class TagService(tagDao: TagDao, elastic: OpenSearchAsyncClient, actorSystem: ActorSystem,
                 sectionService: SectionService, groupDao: GroupDao) extends StrictLogging {
  private implicit val pekko: ActorSystem = actorSystem

  import ru.org.linux.tag.TagService.*

  private val sectionForum: Section = sectionService.getSection(Section.Forum)
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

  def getTagIdOptWithSynonym(tag: String): Option[Int] = tagDao.getTagId(tag).orElse(tagDao.getTagSynonymId(tag))

  /**
   * Получение идентификационного номера тега по названию, либо создание нового тега.
   *
   * @param tagName название тега
   * @return идентификационный номер тега
   */
  def getOrCreateTag(tagName: String): Int = getTagIdOptWithSynonym(tagName).getOrElse(tagDao.createTag(tagName))

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
    val sectionFilter = section.map(s => Query.of(q => q.term(TermQuery.of(t => t.field("section").value(FieldValue.of(s.getUrlName))))))

    val filters = Seq(
      Query.of(q => q.term(TermQuery.of(t => t.field("is_comment").value(FieldValue.of("false"))))),
      Query.of(q => q.term(TermQuery.of(t => t.field("tag").value(FieldValue.of(tag))))),
      Query.of(q => q.term(TermQuery.of(t => t.field(COLUMN_TOPIC_AWAITS_COMMIT).value(FieldValue.of("false")))))
    ) ++ sectionFilter

    val request = new CountRequest.Builder()
      .index(MessageIndex)
      .query(Query.of(q => q.bool(BoolQuery.of(b => b.filter(filters.asJava)))))
      .build()

    elastic.count(request)
      .asScala
      .map(r => Some(r.count))
      .withTimeout(deadline.timeLeft)
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

  def getRelatedTags(tag: String, deadline: Deadline): Future[Seq[TagRef]] = {
    val request = new SearchRequest.Builder()
      .index(MessageIndex)
      .size(0)
      .query(Query.of(q => q.bool(BoolQuery.of(b => b
        .filter(Seq(
          Query.of(q => q.term(TermQuery.of(t => t.field("is_comment").value(FieldValue.of("false"))))),
          Query.of(q => q.term(TermQuery.of(t => t.field("tag").value(FieldValue.of(tag))))))
        .asJava)
      ))))
      .aggregations("related", a => a
        .significantTerms(st => st
          .field("tag")
          .backgroundFilter(Query.of(q => q.term(TermQuery.of(t => t.field("is_comment").value(FieldValue.of("false"))))))
        ))
      .build()

    elastic.search(request, classOf[Void])
      .asScala
      .map { r =>
        val buckets = r.aggregations.get("related").sigsterms().buckets.array().asScala
        buckets.map(b => tagRef(b.key)).toVector.sorted.filterNot(_.name == tag)
      }
      .withTimeout(deadline.timeLeft)
  }

  def getActiveTopTags(section: Section, group: Option[Group], filter: Option[ForumFilter],
                       deadline: Deadline): Future[Seq[TagRef]] = {
    if (group.exists(g => g.id == 4068)) {
      Future.successful(Seq.empty)
    } else {
      val groupFilter = group.map(g => Query.of(q => q.term(TermQuery.of(t => t.field("group").value(FieldValue.of(g.urlName))))))

      val additionalFilter = filter.collect {
        case ForumFilter.Tech =>
          Query.of(q => q.bool(BoolQuery.of(b => b
            .filter(Query.of(q => q.term(TermQuery.of(t => t.field("section").value(FieldValue.of(sectionForum.getUrlName))))))
            .mustNot(NonTechNames.map(n => Query.of(qq => qq.term(TermQuery.of(tt => tt.field("group").value(FieldValue.of(n)))))).asJava)
          )))
        case ForumFilter.NoTalks =>
          Query.of(q => q.bool(BoolQuery.of(b => b
            .mustNot(Query.of(q => q.term(TermQuery.of(t => t.field("group").value(FieldValue.of("talks"))))))
          )))
      }

      val dateTwoYearsAgo = LocalDate.now().minus(2, java.time.temporal.ChronoUnit.YEARS).format(DateTimeFormatter.ISO_LOCAL_DATE)
      val dateOneYearAgo = LocalDate.now().minus(1, java.time.temporal.ChronoUnit.YEARS).format(DateTimeFormatter.ISO_LOCAL_DATE)

      val filters = Seq(
        Query.of(q => q.term(TermQuery.of(t => t.field("is_comment").value(FieldValue.of("false"))))),
        Query.of(q => q.term(TermQuery.of(t => t.field("section").value(FieldValue.of(section.getUrlName))))),
        Query.of(q => q.range(RangeQuery.of(r => r.field("postdate").gte(JsonData.of(dateOneYearAgo)))))
      ) ++ groupFilter ++ additionalFilter

      val bgFilters = Seq(
        Query.of(q => q.term(TermQuery.of(t => t.field("is_comment").value(FieldValue.of("false"))))),
        Query.of(q => q.term(TermQuery.of(t => t.field("section").value(FieldValue.of(section.getUrlName))))),
        Query.of(q => q.range(RangeQuery.of(r => r.field("postdate").gte(JsonData.of(dateTwoYearsAgo)))))
      )

      val request = new SearchRequest.Builder()
        .index(MessageIndex)
        .size(0)
        .query(Query.of(q => q.bool(BoolQuery.of(b => b.filter(filters.asJava)))))
        .aggregations("active", a => a
          .significantTerms(st => st
            .field("tag")
            .size(15)
            .minDocCount(5)
            .backgroundFilter(Query.of(q => q.bool(BoolQuery.of(bb => bb.filter(bgFilters.asJava)))))
          ))
        .build()

      elastic.search(request, classOf[Void])
        .asScala
        .map { r =>
          val buckets = r.aggregations.get("active").sigsterms().buckets.array().asScala
          buckets.map(b => tagRef(b.key, section)).toVector.sorted
        }
        .withTimeout(deadline.timeLeft)
        .recover {
          case ex: TimeoutException =>
            logger.warn(s"Active top tags for ${section.getUrlName} / ${group.map(_.urlName)} / $filter search timed out (${ex.getMessage})")
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

  def tagsToString(tags: Seq[String]): String = tags.mkString(",")
}
