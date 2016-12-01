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

package ru.org.linux.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.searches.{QueryDefinition, RichSearchResponse}
import ru.org.linux.search.ElasticsearchIndexService.MessageIndexTypes

import scala.concurrent.Await
import scala.concurrent.duration._

class SearchViewer(query:SearchRequest, elastic: ElasticClient) {
  import ru.org.linux.search.SearchViewer._

  private def processQueryString(queryText: String) = {
    if (queryText.isEmpty) {
      matchAllQuery
    } else {
      bool {
        must(
          should(
            commonQuery("title") query queryText lowFreqMinimumShouldMatch 2,
            commonQuery("message") query queryText lowFreqMinimumShouldMatch 2)
        ) should matchPhraseQuery("message", queryText)
      }
    }
  }

  private def boost(query: QueryDefinition) = {
    functionScoreQuery(query) scorers(
        weightScore(TopicBoost) filter termQuery("is_comment", "false"),
        weightScore(RecentBoost) filter rangeQuery("postdate").gte("now/d-3y"))
  }

  private def wrapQuery(q:QueryDefinition, filters:Seq[QueryDefinition]) = {
    if (filters.nonEmpty) {
      bool { must(q) filter filters }
    } else {
      q
    }
  }

  def performSearch: RichSearchResponse = {
    val typeFilter = Option(query.getRange.getValue) map { value ⇒
      termQuery(query.getRange.getColumn, value)
    }

    val dateFilter = Option(query.getInterval.getRange) map { range ⇒
      rangeQuery(query.getInterval.getColumn) from range
    }

    val userFilter = Option(query.getUser) map { user ⇒
      if (query.isUsertopic) {
        termQuery("topic_author", user.getNick)
      } else {
        termQuery("author", user.getNick)
      }
    }

    val queryFilters = (typeFilter ++ dateFilter ++ userFilter).toSeq

    val esQuery = wrapQuery(boost(processQueryString(query.getQ)), queryFilters)

    val sectionFilter = Option(query.getSection) filter (_.nonEmpty) map { section ⇒
      termQuery("section", this.query.getSection)
    }

    val groupFilter = Option(query.getGroup) filter (_.nonEmpty) map { group ⇒
      termQuery("group", this.query.getGroup)
    }

    val postFilters = (sectionFilter ++ groupFilter).toSeq

    val future = elastic execute {
      search in MessageIndexTypes fields (
          Fields: _*
        ) query esQuery sort (
          field sort query.getSort.getColumn order query.getSort.order
        ) aggs(
          agg filter "sections" query matchAllQuery aggs (
            agg terms "sections" field "section" size 0 aggs (
              agg terms "groups" field "group" size 0
            )
          ),
          agg sigTerms "tags" field "tag" minDocCount 30
        ) highlighting(
          options encoder "html" preTags "<em class=search-hl>" postTags "</em>" requireFieldMatch false,
          highlight field "title" numberOfFragments 0,
          highlight field "topicTitle" numberOfFragments 0,
          highlight field "message" numberOfFragments 1 fragmentSize MessageFragment
        ) size SearchRows from this.query.getOffset postFilter andFilters(postFilters) timeout SearchTimeout
    }

    Await.result(future, SearchHardTimeout)
  }

  private def andFilters(filters: Seq[QueryDefinition]) = {
    filters match {
      case Seq()       ⇒ matchAllQuery
      case Seq(single) ⇒ single
      case other       ⇒ must(other)
    }
  }
}

object SearchViewer {
  val SearchRows = 25
  val MessageFragment = 500
  val TopicBoost = 3
  val RecentBoost = 2
  val SearchTimeout = 1.minute
  val SearchHardTimeout = SearchTimeout + 10.seconds

  private val Fields = Seq("title", "topic_title", "author", "postdate", "topic_id",
    "section", "message", "group", "is_comment", "tag")
}
