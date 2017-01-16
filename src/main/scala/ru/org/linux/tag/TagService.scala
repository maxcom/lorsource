/*
 * Copyright 1998-2016 Linux.org.ru
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
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.searches.queries.BoolQueryDefinition
import org.elasticsearch.search.aggregations.bucket.significant.SignificantTerms
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.search.aggregations.bucket.terms.support.IncludeExclude
import org.springframework.stereotype.Service
import ru.org.linux.search.ElasticsearchIndexService.MessageIndexTypes
import ru.org.linux.section.Section
import ru.org.linux.topic.TagTopicListController

import scala.collection.JavaConverters._
import scala.collection.immutable.SortedMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Service
class TagService(tagDao: TagDao, elastic: ElasticClient) {
  import ru.org.linux.tag.TagService._

  /**
   * Получение идентификационного номера тега по названию.
   *
   * @param tag название тега
   * @return идентификационный номер
   */
  @throws(classOf[TagNotFoundException])
  def getTagId(tag: String): Int = tagDao.getTagId(tag).getOrElse(throw new TagNotFoundException)

  def getTagIdOpt(tag: String): Option[Int] = tagDao.getTagId(tag)

  /**
    * Получение идентификационного номера тега по названию, либо создание нового тега.
    *
    * @param tagName название тега
    * @return идентификационный номер тега
    */
  def getOrCreateTag(tagName: String): Int = tagDao.getTagId(tagName).getOrElse(tagDao.createTag(tagName))

  @throws(classOf[TagNotFoundException])
  def getTagInfo(tag: String, skipZero: Boolean): TagInfo = {
    val tagId = tagDao.getTagId(tag, skipZero).getOrElse(throw new TagNotFoundException())

    tagDao.getTagInfo(tagId)
  }

  def countTagTopics(tag: String): Future[Long] = {
    Future.successful(elastic) flatMap {
      _ execute {
        search(MessageIndexTypes) size 0 query
          new BoolQueryDefinition().filter(termQuery("is_comment", "false"), termQuery("tag", tag))
      }
    } map {
      _.totalHits
    }
  }

  def getNewTags(tags: util.List[String]): util.List[String] =
    tags.asScala.filterNot(tag ⇒ tagDao.getTagId(tag, skipZero = true).isDefined).asJava

  def getRelatedTags(tag: String): Future[Seq[TagRef]] = Future.successful(elastic) flatMap {
    val sigterms = sigTermsAggregation("related") field "tag" backgroundFilter termQuery("is_comment", "false")

    sigterms.builder.includeExclude(new IncludeExclude(null, Array(tag)))

    _ execute {
      search(MessageIndexTypes) size 0 query
        new BoolQueryDefinition()
          .filter(termQuery("is_comment", "false"), termQuery("tag", tag)) aggs sigterms
    }
  } map { r ⇒
    (for {
      bucket <- r.aggregations.getAs[SignificantTerms]("related").asScala
    } yield {
      tagRef(bucket.getKeyAsString)
    }).toSeq.sorted
  }

  def getActiveTopTags(section: Section): Future[Seq[TagRef]] = {
    Future.successful(elastic) flatMap {
      _ execute {
        search(MessageIndexTypes) size 0 query
          boolQuery().filter(
              termQuery("is_comment", "false"),
              termQuery("section", section.getUrlName),
              rangeQuery("postdate").gte("now/d-1y")
          ) aggs {
            sigTermsAggregation("active") field "tag" minDocCount 5 backgroundFilter termQuery("is_comment", "false")
          }
      }
    } map { r ⇒
      (for {
        bucket <- r.aggregations.getAs[SignificantTerms]("active").getBuckets.asScala
      } yield {
        tagRef(bucket.getKeyAsString)
      }).sorted
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
