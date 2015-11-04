/*
 * Copyright 1998-2015 Linux.org.ru
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

import java.util

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticClient, SearchType}
import org.elasticsearch.search.aggregations.bucket.significant.SignificantTerms
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import ru.org.linux.search.ElasticsearchIndexService.MessageIndexTypes
import ru.org.linux.section.Section
import ru.org.linux.topic.TagTopicListController

import scala.collection.JavaConverters._
import scala.collection.immutable.SortedMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Service
class TagService @Autowired () (tagDao:TagDao, elastic:ElasticClient) {
  import ru.org.linux.tag.TagService._

  /**
   * Получение идентификационного номера тега по названию.
   *
   * @param tag название тега
   * @return идентификационный номер
   */
  @throws(classOf[TagNotFoundException])
  def getTagId(tag: String) = tagDao.getTagId(tag).getOrElse(throw new TagNotFoundException)

  @throws(classOf[TagNotFoundException])
  def getTagInfo(tag: String, skipZero: Boolean): TagInfo = {
    val tagId = tagDao.getTagId(tag, skipZero).getOrElse(throw new TagNotFoundException())

    tagDao.getTagInfo(tagId)
  }

  def countTagTopics(tag: String): Future[Long] = {
    Future.successful(elastic) flatMap {
      _ execute {
        search in MessageIndexTypes searchType SearchType.Count query
          filteredQuery.query(matchAllQuery).filter(must(termFilter("is_comment", "false"), termFilter("tag", tag)))
      }
    } map {
      _.getHits.getTotalHits
    }
  }

  def getNewTags(tags:util.List[String]):util.List[String] =
    tags.asScala.filterNot(tag ⇒ tagDao.getTagId(tag, skipZero = true).isDefined).asJava

  def getRelatedTags(tag: String): Future[Seq[TagRef]] = {
    Future.successful(elastic) flatMap {
      val sigterms = agg sigTerms "related" field "tag" backgroundFilter termFilter("is_comment", "false")

      sigterms.builder.exclude(Array(tag))

      _ execute {
        search in MessageIndexTypes searchType SearchType.Count query
          filteredQuery
            .query(matchAllQuery)
            .filter(must(termFilter("is_comment", "false"), termFilter("tag", tag))) aggs sigterms
      }
    } map { r ⇒
      (for {
        bucket <- r.getAggregations.get[SignificantTerms]("related").asScala
      } yield {
        tagRef(bucket.getKey)
      }).toSeq.sorted
    }
  }

  def getActiveTopTags(section: Section): Future[Seq[TagRef]] = {
    Future.successful(elastic) flatMap {
      _ execute {
        search in MessageIndexTypes searchType SearchType.Count query
          filteredQuery
            .query(matchAllQuery)
            .filter(must(
              termFilter("is_comment", "false"),
              termFilter("section", section.getUrlName),
              rangeFilter("postdate").gte("now/d-1y"))
            ) aggs (agg terms "active" field "tag" minDocCount 10)
      }
    } map { r ⇒
      (for {
        bucket <- r.getAggregations.get[Terms]("active").getBuckets.asScala
      } yield {
        tagRef(bucket.getKey)
      }).toSeq.sorted
    }
  }

  /**
   * Получить список популярных тегов по префиксу.
   *
   * @param prefix     префикс
   * @param count      количество тегов
   * @return список тегов по первому символу
   */
  def suggestTagsByPrefix(prefix: String, count: Int): util.List[String] =
    tagDao.getTopTagsByPrefix(prefix, 2, count).asJava

  /**
   * Получить уникальный список первых букв тегов.
   *
   * @return список первых букв тегов
   */
  def getFirstLetters: util.List[String] = tagDao.getFirstLetters.asJava

  /**
   * Получить список тегов по префиксу.
   *
   * @param prefix     префикс
   * @return список тегов по первому символу
   */
  def getTagsByPrefix(prefix: String, threshold: Int): util.Map[TagRef, Integer] = {
    val result = for (
      info <- tagDao.getTagsByPrefix(prefix, threshold)
    ) yield TagService.tagRef(info) -> (info.topicCount:java.lang.Integer)

    SortedMap(result: _*).asJava
  }
}

object TagService {
  def tagRef(tag: TagInfo) = TagRef(tag.name,
    if (TagName.isGoodTag(tag.name)) {
      Some(TagTopicListController.tagListUrl(tag.name))
    } else {
      None
    })

  def tagRef(name: String) = TagRef(name,
    if (TagName.isGoodTag(name)) {
      Some(TagTopicListController.tagListUrl(name))
    } else {
      None
    })

  def namesToRefs(tags:java.util.List[String]):java.util.List[TagRef] = tags.asScala.map(tagRef).asJava

  def tagsToString(tags: util.Collection[String]): String = tags.asScala.mkString(",")
}
