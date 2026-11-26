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

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import com.typesafe.scalalogging.StrictLogging
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.{CircuitBreaker, CircuitBreakerOpenException}
import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch.OpenSearchAsyncClient
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.aggregations.TermsExclude
import org.opensearch.client.opensearch._types.query_dsl.{BoolQuery, Query, RangeQuery, TermQuery}
import org.opensearch.client.opensearch.core.{CountRequest, SearchRequest}
import org.springframework.stereotype.Service
import ru.org.linux.group.{Group, GroupService}
import ru.org.linux.scalikejdbc.Transaction
import ru.org.linux.search.OpenSearchIndexService.{COLUMN_TOPIC_AWAITS_COMMIT, MessageIndex}
import ru.org.linux.section.{Section, SectionController, SectionService}
import ru.org.linux.topic.TagTopicListController
import ru.org.linux.topic.TopicListController.ForumFilter
import ru.org.linux.util.RichFuture.withTimeout

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import scala.collection.immutable.SortedMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Deadline, DurationInt}
import scala.concurrent.{Future, TimeoutException}
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*
import scala.util.Success

@Service
class TagService(
    tagDao: TagDao,
    searchClient: OpenSearchAsyncClient,
    actorSystem: ActorSystem,
    sectionService: SectionService,
    groupService: GroupService)
    extends StrictLogging:
  private given ActorSystem = actorSystem

  import ru.org.linux.tag.TagService.*

  // Свежие кеши отдают попадания без обращения к OpenSearch; stale — фолбэк при сбое поиска,
  // чтобы блоки тегов не исчезали на время падения OpenSearch (по образцу MoreLikeThisService).
  private[tag] val activeTopTagsCache: Cache[ActiveTopTags, Seq[TagRef]] =
    Caffeine.newBuilder().maximumSize(TopTagsCacheSize).expireAfterWrite(TtlMinutes, TimeUnit.MINUTES).build()

  private[tag] val activeTopTagsStaleCache: Cache[ActiveTopTags, Seq[TagRef]] =
    Caffeine.newBuilder().maximumSize(TopTagsCacheSize).expireAfterWrite(StaleTtlHours, TimeUnit.HOURS).build()

  private[tag] val relatedTagsCache: Cache[String, Seq[TagRef]] =
    Caffeine.newBuilder().maximumSize(TagCacheSize).expireAfterWrite(TtlMinutes, TimeUnit.MINUTES).build()

  private[tag] val relatedTagsStaleCache: Cache[String, Seq[TagRef]] =
    Caffeine.newBuilder().maximumSize(TagCacheSize).expireAfterWrite(StaleTtlHours, TimeUnit.HOURS).build()

  private[tag] val tagTopicCountCache: Cache[TagTopicCount, Option[Long]] =
    Caffeine.newBuilder().maximumSize(TagCacheSize).expireAfterWrite(TtlMinutes, TimeUnit.MINUTES).build()

  private[tag] val tagTopicCountStaleCache: Cache[TagTopicCount, Option[Long]] =
    Caffeine.newBuilder().maximumSize(TagCacheSize).expireAfterWrite(StaleTtlHours, TimeUnit.HOURS).build()

  // breaker отсекает doomed-запросы при лежащем OpenSearch: после серии сбоев промах кеша отдаёт stale
  // мгновенно, не тратя дедлайн страницы на обречённый запрос (параметры как у MoreLikeThisService)
  private val breaker =
    new CircuitBreaker(
      scheduler = actorSystem.scheduler,
      maxFailures = 5,
      callTimeout = 2.seconds,
      resetTimeout = 1.minute)

  breaker.onOpen {
    logger.warn("Tag search disabled")
  }
  breaker.onClose {
    logger.warn("Tag search enabled")
  }

  /** Общий каркас запроса к OpenSearch с кешированием: при попадании в свежий кеш ответ отдаётся мгновенно; при промахе
    * запрос выполняется через circuit breaker с дедлайном, успешный ответ фиксируется в свежем и stale кешах. При
    * сбое, таймауте или открытом breaker отдаётся stale значение, а если его нет — исключение пробрасывается
    * вызывающему для его фолбэка.
    */
  private def cachedOrStale[K <: AnyRef, V <: AnyRef](
      fresh: Cache[K, V],
      stale: Cache[K, V],
      key: K,
      deadline: Deadline)(load: => Future[V]): Future[V] =
    val hit = fresh.getIfPresent(key)

    if hit != null then
      Future.successful(hit)
    else
      breaker
        .withCircuitBreaker(load)
        .andThen:
          case Success(value) =>
            fresh.put(key, value)
            stale.put(key, value)
        .withTimeout(deadline.timeLeft)
        .recoverWith:
          case ex =>
            val fromStale = Option(stale.getIfPresent(key))

            if fromStale.isDefined then
              logger.debug(s"Serving stale cached value for $key after search failure: $ex")
              Future.successful(fromStale.get)
            else
              Future.failed(ex)

  private lazy val sectionForum: Section = sectionService.getSection(Section.Forum)
  private lazy val NonTechNames: Seq[String] =
    groupService.getGroups(sectionForum).filter(g => SectionController.NonTech.contains(g.id)).map(_.urlName).toSeq

  /** Получение идентификационного номера тега по названию.
    *
    * @param tag
    *   название тега
    * @return
    *   идентификационный номер
    */
  @throws(classOf[TagNotFoundException])
  def getTagId(tag: String, skipZero: Boolean = false): Int =
    tagDao.getTagId(tag, skipZero).getOrElse(throw new TagNotFoundException)

  def getTagIdOpt(tag: String): Option[Int] = tagDao.getTagId(tag)

  def getTagIdOptWithSynonym(tag: String): Option[Int] = tagDao.getTagId(tag).orElse(tagDao.getTagSynonymId(tag))

  /** Получение идентификационного номера тега по названию, либо создание нового тега.
    *
    * @param tagName
    *   название тега
    * @return
    *   идентификационный номер тега
    */
  def getOrCreateTag(tagName: String)(using Transaction): Int =
    getTagIdOptWithSynonym(tagName).getOrElse(tagDao.createTag(tagName))

  def getTagInfo(tag: String, skipZero: Boolean): Option[TagInfo] =
    if TagName.isGoodTag(tag) then
      tagDao.getTagId(tag, skipZero).map(tagDao.getTagInfo)
    else
      None

  def getTagBySynonym(tagName: String): Option[TagRef] =
    tagDao.getTagSynonymId(tagName).map(tagDao.getTagInfo).map(i => tagRef(i, threshold = 0))

  def countTagTopics(tag: String, section: Option[Section], deadline: Deadline): Future[Option[Long]] =
    cachedOrStale(tagTopicCountCache, tagTopicCountStaleCache, TagTopicCount(tag, section), deadline) {
      val sectionFilter = section.map(s =>
        Query.of(q => q.term(TermQuery.of(t => t.field("section").value(FieldValue.of(s.getUrlName))))))

      val filters =
        Seq(
          Query.of(q => q.term(TermQuery.of(t => t.field("is_comment").value(FieldValue.of("false"))))),
          Query.of(q => q.term(TermQuery.of(t => t.field("tag").value(FieldValue.of(tag))))),
          Query.of(q => q.term(TermQuery.of(t => t.field(COLUMN_TOPIC_AWAITS_COMMIT).value(FieldValue.of("false")))))
        ) ++ sectionFilter

      val request = new CountRequest.Builder()
        .index(MessageIndex)
        .query(Query.of(q => q.bool(BoolQuery.of(b => b.filter(filters.asJava)))))
        .build()

      searchClient.count(request).asScala.map(r => Some(r.count))
    }.recover {
      case _: CircuitBreakerOpenException =>
        // не WARN: открытие breaker'а уже отрапортовано onOpen
        logger.debug("Tag topics count skipped, tag search circuit breaker is open")
        None
      case ex: TimeoutException =>
        logger.warn(s"Tag topics count timed out (${ex.getMessage})")
        None
      case ex =>
        logger.warn("Unable to count tag topics", ex)
        None
    }

  def getNewTags(tags: Seq[String]): Seq[String] =
    tags.filterNot { tag =>
      tagDao.getTagId(tag, skipZero = true).isDefined || tagDao.getTagSynonymId(tag).isDefined
    }

  def getRelatedTags(tag: String, deadline: Deadline): Future[Seq[TagRef]] =
    cachedOrStale(relatedTagsCache, relatedTagsStaleCache, tag, deadline) {
      val request = new SearchRequest.Builder()
        .index(MessageIndex)
        .size(0)
        .query(
          Query.of(q =>
            q.bool(
              BoolQuery.of(b =>
                b.filter(
                  Seq(
                    Query.of(q => q.term(TermQuery.of(t => t.field("is_comment").value(FieldValue.of("false"))))),
                    Query.of(q => q.term(TermQuery.of(t => t.field("tag").value(FieldValue.of(tag)))))
                  ).asJava)))))
        .aggregations(
          "related",
          a =>
            a.significantTerms(st =>
              st.field("tag")
                .backgroundFilter(
                  Query.of(q => q.term(TermQuery.of(t => t.field("is_comment").value(FieldValue.of("false")))))))
        )
        .build()

      searchClient
        .search(request, classOf[Void])
        .asScala
        .map { r =>
          val buckets = r.aggregations.get("related").sigsterms().buckets.array().asScala
          buckets.map(b => tagRef(b.key)).toVector.sorted.filterNot(_.name == tag)
        }
    }

  def getActiveTopTags(
      section: Section,
      group: Option[Group],
      filter: Option[ForumFilter],
      deadline: Deadline): Future[Seq[TagRef]] =
    if group.exists(g => g.id == 4068) then
      Future.successful(Seq.empty)
    else
      cachedOrStale(activeTopTagsCache, activeTopTagsStaleCache, ActiveTopTags(section, group, filter), deadline) {
        calcActiveTopTags(section, group, filter)
      }.recover {
        case _: CircuitBreakerOpenException =>
          // не WARN: открытие breaker'а уже отрапортовано onOpen
          logger.debug("Active top tags skipped, tag search circuit breaker is open")
          Seq.empty
        case ex: TimeoutException =>
          logger.warn(
            s"Active top tags for ${section.getUrlName} / ${group.map(_.urlName)} / $filter search timed out (${ex
                .getMessage})")
          Seq.empty
        case ex =>
          logger.warn("Unable to find active top tags", ex)
          Seq.empty
      }

  private def calcActiveTopTags(
      section: Section,
      group: Option[Group],
      filter: Option[ForumFilter]): Future[Seq[TagRef]] =
    val groupFilter = group.map(g =>
      Query.of(q => q.term(TermQuery.of(t => t.field("group").value(FieldValue.of(g.urlName))))))

    val additionalFilter = filter.collect {
      case ForumFilter.Tech =>
        Query.of(q =>
          q.bool(
            BoolQuery.of(b =>
              b.filter(
                  Query.of(q =>
                    q.term(TermQuery.of(t => t.field("section").value(FieldValue.of(sectionForum.getUrlName))))))
                .mustNot(
                  NonTechNames
                    .map(n => Query.of(qq => qq.term(TermQuery.of(tt => tt.field("group").value(FieldValue.of(n))))))
                    .asJava))))
      case ForumFilter.NoTalks =>
        Query.of(q =>
          q.bool(
            BoolQuery.of(b =>
              b.mustNot(Query.of(q => q.term(TermQuery.of(t => t.field("group").value(FieldValue.of("talks")))))))))
    }

    val dateOneYearAgo = LocalDate
      .now()
      .minus(1, java.time.temporal.ChronoUnit.YEARS)
      .format(DateTimeFormatter.ISO_LOCAL_DATE)

    val filters =
      Seq(
        Query.of(q => q.term(TermQuery.of(t => t.field("is_comment").value(FieldValue.of("false"))))),
        Query.of(q => q.term(TermQuery.of(t => t.field("section").value(FieldValue.of(section.getUrlName))))),
        Query.of(q => q.range(RangeQuery.of(r => r.field("postdate").gte(JsonData.of(dateOneYearAgo)))))
      ) ++ groupFilter ++ additionalFilter

    val request = new SearchRequest.Builder()
      .index(MessageIndex)
      .size(0)
      .query(Query.of(q => q.bool(BoolQuery.of(b => b.filter(filters.asJava)))))
      .aggregations(
        "active",
        a =>
          a.terms(st =>
            st.field("tag")
              .size(MaxActiveTags)
              .minDocCount(10)
              .exclude(TermsExclude.of(e => e.terms(ExcludedActiveTags.asJava))))
      )
      .build()

    searchClient
      .search(request, classOf[Void])
      .asScala
      .map: r =>
        val buckets = r.aggregations.get("active").sterms().buckets.array().asScala
        val tags = buckets.map(b => tagRef(b.key, section)).toVector.sorted

        if tags.sizeIs >= MinActiveTags then tags else Seq.empty

  /** Получить список популярных тегов по префиксу.
    *
    * @param prefix
    *   префикс
    * @param count
    *   количество тегов
    * @return
    *   список тегов по первому символу
    */
  def suggestTagsByPrefix(prefix: String, count: Int): Seq[String] = tagDao.getTopTagsByPrefix(prefix, 2, count)

  /** Получить уникальный список первых букв тегов.
    *
    * @return
    *   список первых букв тегов
    */
  def getFirstLetters: Seq[String] = tagDao.getFirstLetters

  /** Получить список тегов по префиксу.
    *
    * @param prefix
    *   префикс
    * @return
    *   список тегов по первому символу
    */
  def getTagsByPrefix(prefix: String, threshold: Int): Map[TagRef, Int] =
    tagDao
      .getTagsByPrefix(prefix, threshold)
      .view
      .map { info =>
        TagService.tagRef(info, threshold) -> info.topicCount
      }
      .to(SortedMap)

  def getSynonymsFor(tagId: Int): Seq[TagRef] = tagDao.getSynonymsFor(tagId).map(name => TagRef(name, None))

object TagService:
  private val TtlMinutes = 15
  private val StaleTtlHours = 48
  private val TopTagsCacheSize = 1000
  private val TagCacheSize = 10000

  /** Теги, активные в любом разделе, — в списке активных они лишние */
  private val ExcludedActiveTags: Seq[String] = Seq("linux")

  /** Меньше этого числа блок «Активные теги» выглядит случайным набором */
  private val MinActiveTags = 4

  /** Больше этого числа тегов у OpenSearch не запрашиваем */
  private val MaxActiveTags = 12

  def tagRef(tag: TagInfo, threshold: Int = 2): TagRef =
    TagRef(
      tag.name,
      if TagName.isGoodTag(tag.name) && tag.topicCount >= threshold then
        Some(TagTopicListController.tagListUrl(tag.name))
      else
        None)

  def tagRef(name: String): TagRef =
    TagRef(
      name,
      if TagName.isGoodTag(name) then
        Some(TagTopicListController.tagListUrl(name))
      else
        None)

  def tagRef(name: String, section: Section): TagRef =
    TagRef(
      name,
      if TagName.isGoodTag(name) then
        Some(TagTopicListController.tagListUrl(name, section))
      else
        None)

  def namesToRefs(tags: java.util.List[String]): java.util.List[TagRef] = tags.asScala.map(tagRef).asJava

  def tagsToString(tags: Seq[String]): String = tags.mkString(",")

  private[tag] case class ActiveTopTags(section: Section, group: Option[Group], filter: Option[ForumFilter])

  private[tag] case class TagTopicCount(tag: String, section: Option[Section])
