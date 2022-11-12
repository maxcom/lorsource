/*
 * Copyright 1998-2022 Linux.org.ru
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

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.requests.searches.queries.funcscorer.WeightScore

import scala.concurrent.Await
import scala.concurrent.duration.*

class SearchViewer(query: SearchRequest, elastic: ElasticClient) {
  import ru.org.linux.search.SearchViewer.*

  private def processQueryString(queryText: String) = {
    if (queryText.isEmpty) {
      matchAllQuery
    } else {
      boolQuery.
        should(
          commonTermsQuery("title") query queryText lowFreqMinimumShouldMatch 2,
          commonTermsQuery("message") query queryText lowFreqMinimumShouldMatch 2,
          matchPhraseQuery("message", queryText)).minimumShouldMatch(1)
    }
  }

  private def boost(query: Query) = {
    functionScoreQuery(query) functions(
      WeightScore(TopicBoost).filter(termQuery("is_comment", "false")),
      WeightScore(RecentBoost).filter(rangeQuery("postdate").gte("now/d-3y"))
    )
  }

  private def wrapQuery(q: Query, filters: Seq[Query]) = {
    if (filters.nonEmpty) {
      boolQuery.must(q).filter(filters)
    } else {
      q
    }
  }

  def performSearch: SearchResponse = {
    val typeFilter = Option(query.getRange.getValue) map { value =>
      termQuery(query.getRange.getColumn, value)
    }

    val dateFilter = Option(query.getInterval.getRange) map { range =>
      rangeQuery(query.getInterval.getColumn) gt range
    }

    val userFilter = Option(query.getUser) map { user =>
      if (query.isUsertopic) {
        termQuery("topic_author", user.getNick)
      } else {
        termQuery("author", user.getNick)
      }
    }

    val queryFilters = (typeFilter ++ dateFilter ++ userFilter).toSeq

    val esQuery = wrapQuery(boost(processQueryString(query.getQ)), queryFilters)

    val sectionFilter = Option(query.getSection) filter (_.nonEmpty) map { section =>
      termQuery("section", section)
    }

    val groupFilter = Option(query.getGroup) filter (_.nonEmpty) map { group =>
      termQuery("group", group)
    }

    val postFilters = (sectionFilter ++ groupFilter).toSeq

    val future = elastic execute {
      search(ElasticsearchIndexService.MessageIndex) fetchSource true sourceInclude Fields query esQuery sortBy query.getSort.order aggs(
        filterAggregation("sections") query matchAllQuery subAggregations (
            termsAggregation("sections") field "section" size 50 subAggregations (
              termsAggregation("groups") field "group" size 50
            )
          ),
          sigTermsAggregation("tags") field "tag" minDocCount 30
        ) highlighting(
          highlightOptions() encoder "html" preTags "<em class=search-hl>" postTags "</em>" requireFieldMatch false,
          highlight("title") numberOfFragments 0,
          highlight("topicTitle") numberOfFragments 0,
          highlight("message") numberOfFragments 1 fragmentSize MessageFragment highlighterType "fvh"
        ) size SearchRows from this.query.getOffset postFilter andFilters(postFilters) timeout SearchTimeout
    }

    Await.result(future, SearchHardTimeout).result
  }

  private def andFilters(filters: Seq[Query]) = {
    filters match {
      case Seq()       => matchAllQuery
      case Seq(single) => single
      case other       => must(other)
    }
  }
}

object SearchViewer {
  val SearchRows = 25
  val MessageFragment = 500
  val TopicBoost = 3
  val RecentBoost = 2
  val SearchTimeout: FiniteDuration = 1.minute
  val SearchHardTimeout: FiniteDuration = SearchTimeout + 10.seconds

  private val Fields = Seq("title", "topic_title", "author", "postdate", "topic_id",
    "section", "message", "group", "is_comment", "tag")
}
