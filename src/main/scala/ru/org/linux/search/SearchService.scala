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

package ru.org.linux.search

import com.google.common.base.Strings
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.searches.aggs.responses.bucket.{TermBucket, Terms}
import com.sksamuel.elastic4s.requests.searches.aggs.responses.{Aggregations, FilterAggregationResult}
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.requests.searches.queries.funcscorer.WeightScore
import com.sksamuel.elastic4s.requests.searches.queries.matches.MatchQuery
import com.sksamuel.elastic4s.requests.searches.sort.SortOrder
import com.sksamuel.elastic4s.requests.searches.{SearchHit, SearchResponse}
import org.joda.time.DateTimeZone
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import ru.org.linux.group.GroupDao
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.spring.SiteConfig
import ru.org.linux.tag.{TagRef, TagService}
import ru.org.linux.user.UserService
import ru.org.linux.util.StringUtil

import java.time.Instant
import java.util.Date
import scala.concurrent.Await
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

@Service
class SearchService(elastic: ElasticClient, userService: UserService, siteConfig: SiteConfig,
                    sectionService: SectionService, groupDao: GroupDao) {
  import ru.org.linux.search.SearchService.*

  private def processQueryString(queryText: String) = {
    if (queryText.isEmpty) {
      matchAllQuery()
    } else {
      boolQuery()
        .must(
          should(
            MatchQuery("title", queryText).minimumShouldMatch("2"),
            MatchQuery("message", queryText).minimumShouldMatch("2")))
        .should(
          matchPhraseQuery("message", queryText),
          matchPhraseQuery("title", queryText),
          MatchQuery("message.raw", queryText).minimumShouldMatch("2")
        ).minimumShouldMatch(0)
    }
  }

  private def boost(query: Query) = {
    functionScoreQuery(query).functions(
      WeightScore(RecentBoost).filter(rangeQuery("postdate").gte("now/d-3y"))
    )
  }

  private def wrapQuery(q: Query, filters: Seq[Query]) = {
    if (filters.nonEmpty) {
      boolQuery().must(q).filter(filters)
    } else {
      q
    }
  }

  def performSearch(query: SearchServiceRequest, tz: DateTimeZone): SearchServiceResponse = {
    val typeFilter = Option(query.getRange.getValue) map { value =>
      termQuery(query.getRange.getColumn, value)
    }
    val selectedDateFilter = rangeQuery(query.getInterval.getColumn) gte query.atStartOfDaySelected(tz) lte query.atEndOfDaySelected(tz)

    val dateFilter = Option(query.getInterval.getRange) map { range =>
      rangeQuery(query.getInterval.getColumn) gt range
    }

    val userFilter = Option(query.getUser) map { user =>
      if (query.isUsertopic) {
        termQuery("topic_author", user.nick)
      } else {
        termQuery("author", user.nick)
      }
    }

    val queryFilters = (typeFilter ++ (if (query.isDateSelected) Option(selectedDateFilter) else dateFilter) ++ userFilter).toSeq

    val esQuery = wrapQuery(boost(processQueryString(query.getQ)), queryFilters)

    val sectionFilter = Option(query.getSection) filter (_.nonEmpty) map { section =>
      termQuery("section", section)
    }

    val groupFilter = Option(query.getGroup) filter (_.nonEmpty) map { group =>
      termQuery("group", group)
    }

    val postFilters = (sectionFilter ++ groupFilter).toSeq

    val order = query.getSort match {
      case SearchOrder.Relevance =>
        Seq(scoreSort(SortOrder.DESC), fieldSort("postdate") order SortOrder.DESC)
      case SearchOrder.Date =>
        Seq(fieldSort("postdate") order SortOrder.DESC)
      case SearchOrder.DateReverse =>
        Seq(fieldSort("postdate") order SortOrder.DESC)
    }

    val future = elastic execute {
      search(OpenSearchIndexService.MessageIndex).fetchSource(true).sourceInclude(Fields).query(esQuery).sortBy(order).aggs(
        filterAgg("sections", matchAllQuery()) subAggregations (
          termsAgg("sections", "section") size 50 subAggregations (
            termsAgg("groups", "group") size 50
            )
          ),
          sigTermsAggregation("tags") field "tag" minDocCount 30
        ).highlighting(
          highlightOptions() preTags "<em class=search-hl>" postTags "</em>" requireFieldMatch false,
          highlight("title") numberOfFragments 0,
          highlight("topicTitle") numberOfFragments 0,
          highlight("message") numberOfFragments 1 fragmentSize MessageFragment highlighterType "fvh"
        ).size(SearchRows).from(query.getOffset).postFilter(andFilters(postFilters)).timeout(SearchTimeout)
        .trackTotalHits(true)
    }

    buildResponse(query, Await.result(future, SearchHardTimeout).result)
  }

  private def buildResponse(query: SearchServiceRequest, response: SearchResponse): SearchServiceResponse = {
    var sectionFacetResponse: Option[Seq[FacetItem]] = None
    var groupFacetResponse: Option[Seq[FacetItem]] = None
    var foundTagsResponse: Option[Seq[TagRef]] = None

    if (response.aggregations != null) {
      val countFacet = response.aggregations.filter("sections")
      val sectionsFacet = countFacet.terms("sections")

      if (sectionsFacet.buckets.size > 1 || !Strings.isNullOrEmpty(query.getSection)) {
        sectionFacetResponse = Some(buildSectionFacet(countFacet, Option.apply(Strings.emptyToNull(query.getSection))))

        if (!Strings.isNullOrEmpty(query.getSection)) {
          val selectedSection = sectionsFacet.bucketOpt(query.getSection)

          if (!Strings.isNullOrEmpty(query.getGroup)) {
            groupFacetResponse = buildGroupFacet(selectedSection, Some(query.getSection -> query.getGroup))
          } else {
            groupFacetResponse = buildGroupFacet(selectedSection, None)
          }
        }


      } else if (Strings.isNullOrEmpty(query.getSection) && sectionsFacet.buckets.size == 1) {
        val onlySection = sectionsFacet.buckets.head

        query.setSection(onlySection.key)

        groupFacetResponse = buildGroupFacet(Some(onlySection), None)
      }

      foundTagsResponse = Some(foundTags(response.aggregations))
    }

    SearchServiceResponse(
      hits = response.hits.hits.view.map(prepare).toVector,
      sectionFacet = sectionFacetResponse,
      groupFacet = groupFacetResponse,
      foundTags = foundTagsResponse,
      totalHits = response.totalHits,
      took = response.took)
  }

  private def andFilters(filters: Seq[Query]) = {
    filters match {
      case Seq()       => matchAllQuery()
      case Seq(single) => single
      case other       => must(other)
    }
  }

  private def prepare(doc: SearchHit): SearchItem = {
    val author = userService.getUserCached(doc.sourceAsMap("author").asInstanceOf[String])

    val postdate = Instant.parse(doc.sourceAsMap("postdate").asInstanceOf[String])

    val comment = doc.sourceAsMap("is_comment").asInstanceOf[Boolean]

    val tags = if (comment) {
      Seq.empty
    } else {
      if (doc.sourceAsMap.contains("tag")) {
        doc.sourceAsMap("tag").asInstanceOf[Seq[String]].map(
          tag => TagService.tagRef(tag))
      } else {
        Seq.empty
      }
    }

    SearchItem(
      title = getTitle(doc),
      postdate = Date.from(postdate),
      user = author,
      url = getUrl(doc),
      score = doc.score,
      comment = comment,
      message = getMessage(doc),
      tags = tags.asJava
    )
  }

  private def getTitle(doc: SearchHit): String = {
    val itemTitle = doc.highlight.get("title").flatMap(_.headOption)
      .orElse(doc.sourceAsMap.get("title") map { v => StringUtil.escapeHtml(v.asInstanceOf[String]) } )

    itemTitle.filter(_.trim.nonEmpty).orElse(
        doc.highlight.get("topic_title").flatMap(_.headOption))
      .getOrElse(StringUtil.escapeHtml(doc.sourceAsMap("topic_title").asInstanceOf[String]))
  }

  private def getMessage(doc: SearchHit): String = {
    val html = doc.highlight.get("message").flatMap(_.headOption) getOrElse {
      doc.sourceAsMap("message").asInstanceOf[String].take(SearchService.MessageFragment)
    }

    Jsoup.clean(html, siteConfig.getSecureUrl, TextSafelist)
  }

  private def getUrl(doc: SearchHit): String = {
    val section = doc.sourceAsMap("section").asInstanceOf[String]
    val msgid = doc.id

    val comment = doc.sourceAsMap("is_comment").asInstanceOf[Boolean]
    val topic = doc.sourceAsMap("topic_id").asInstanceOf[Int]
    val group = doc.sourceAsMap("group").asInstanceOf[String]

    if (comment) {
      val builder = UriComponentsBuilder.fromPath("/{section}/{group}/{msgid}?cid={cid}")
      builder.buildAndExpand(section, group, Integer.valueOf(topic), msgid).toUriString
    } else {
      val builder = UriComponentsBuilder.fromPath("/{section}/{group}/{msgid}")
      builder.buildAndExpand(section, group, Integer.valueOf(topic)).toUriString
    }
  }

  private def buildSectionFacet(sectionFacet: FilterAggregationResult, selected:Option[String]): Seq[FacetItem] = {
    def mkItem(urlName: String, count: Long) = {
      val name = sectionService.nameToSection.get(urlName).map(_.getName).getOrElse(urlName).toLowerCase
      FacetItem(urlName, s"$name ($count)")
    }

    val agg = sectionFacet.result[Terms]("sections")

    val items = for (entry <- agg.buckets) yield {
      mkItem(entry.key, entry.docCount)
    }

    val missing = selected.filter(key => items.forall(_.key !=key)).map(mkItem(_, 0)).toSeq

    val all = FacetItem("", s"все (${sectionFacet.docCount})")

    all +: (missing ++ items)
  }

  private def buildGroupFacet(maybeSection: Option[TermBucket], selected:Option[(String, String)]): Option[Seq[FacetItem]] = {
    def mkItem(section:Section, groupUrlName:String, count:Long) = {
      val group = groupDao.getGroup(section, groupUrlName)
      val name = group.title.toLowerCase
      FacetItem(groupUrlName, s"$name ($count)")
    }

    val facetItems = for {
      selectedSection <- maybeSection.toSeq
      groups = selectedSection.result[Terms]("groups")
      section = sectionService.getSectionByName(selectedSection.key)
      entry <- groups.buckets
    } yield {
      mkItem(section, entry.key, entry.docCount)
    }

    val missing = selected.filter(key => facetItems.forall(_.key != key._2)).map(p =>
      mkItem(sectionService.getSectionByName(p._1), p._2, 0)
    ).toSeq

    val items = facetItems ++ missing

    if (items.size > 1 || selected.isDefined) {
      val all = FacetItem("", s"все (${maybeSection.map(_.docCount).getOrElse(0)})")

      Some(all +: items)
    } else {
      None
    }
  }

  private def foundTags(agg: Aggregations): Seq[TagRef] = {
    val tags = agg.significantTerms("tags")

    tags.buckets.map(bucket => TagService.tagRef(bucket.key))
  }
}

object SearchService {
  val SearchRows = 25
  private val MessageFragment = 16384 // 0 not supported here!
  private val RecentBoost = 2
  private val SearchTimeout: FiniteDuration = 1.minute
  private val SearchHardTimeout: FiniteDuration = SearchTimeout + 10.seconds

  private val Fields = Seq("title", "topic_title", "author", "postdate", "topic_id",
    "section", "message", "group", "is_comment", "tag")

  private val TextSafelist: Safelist = Safelist.relaxed().addAttributes(":all", "class")
}
