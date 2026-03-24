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
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.{FieldValue, SortOptions, SortOrder}
import org.opensearch.client.opensearch._types.aggregations.{Aggregate, SignificantTermsAggregation, StringTermsBucket,
  TermsAggregation}
import org.opensearch.client.opensearch._types.query_dsl.*
import org.opensearch.client.opensearch.core.{SearchRequest, SearchResponse}
import org.opensearch.client.opensearch.core.search.{HighlightField, Hit}
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import ru.org.linux.group.GroupDao
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.spring.SiteConfig
import ru.org.linux.tag.{TagRef, TagService}
import ru.org.linux.user.UserService
import ru.org.linux.util.StringUtil

import java.time.{Instant, ZoneId}
import java.util
import java.util.Date
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

@Service
class SearchService(elastic: OpenSearchClient, userService: UserService, siteConfig: SiteConfig,
                    sectionService: SectionService, groupDao: GroupDao) {
  import ru.org.linux.search.SearchService.*

  private def processQueryString(queryText: String): Query = {
    if (queryText.isEmpty) {
      Query.of(q => q.matchAll(m => m))
    } else {
      val shouldInMustQueries = Seq(
        Query.of(q3 => q3.`match`(MatchQuery.of(mq => mq.field("title").query(qv => qv.stringValue(queryText)).minimumShouldMatch("2")))),
        Query.of(q3 => q3.`match`(MatchQuery.of(mq => mq.field("message").query(qv => qv.stringValue(queryText)).minimumShouldMatch("2"))))
      )

      val shouldQueries = Seq(
        Query.of(q3 => q3.matchPhrase(MatchPhraseQuery.of(mpq => mpq.field("message").query(queryText)))),
        Query.of(q3 => q3.matchPhrase(MatchPhraseQuery.of(mpq => mpq.field("title").query(queryText)))),
        Query.of(q3 => q3.`match`(MatchQuery.of(mq => mq.field("message.raw").query(qv => qv.stringValue(queryText)).minimumShouldMatch("2"))))
      )

      val mustInner = Query.of(q => q.bool(BoolQuery.of(b => b.should(shouldInMustQueries.asJava).minimumShouldMatch("1"))))

      Query.of(q => q
        .bool(BoolQuery.of(b => b
          .must(mustInner)
          .should(shouldQueries.asJava)
          .minimumShouldMatch("0")
        ))
      )
    }
  }

  private def boost(query: Query): Query = {
    Query.of(q => q
      .functionScore(FunctionScoreQuery.of(fsq => fsq
        .query(query)
        .functions(f => f
          .weight(RecentBoost.toFloat:java.lang.Float)
          .filter(r => r.range(RangeQuery.of(r2 => r2.field("postdate").gte(JsonData.of("now/d-3y")))))
        )
      ))
    )
  }

  private def wrapQuery(q: Query, filters: Seq[Query]): Query = {
    if (filters.nonEmpty) {
      Query.of(qq => qq.bool(BoolQuery.of(b => b.must(q).filter(filters.asJava))))
    } else {
      q
    }
  }

  def performSearch(query: SearchServiceRequest, tz: ZoneId): SearchServiceResponse = {
    val typeFilter = Option(query.getRange.getValue) map { value =>
      Query.of(q => q.term(TermQuery.of(t => t.field(query.getRange.getColumn).value(FieldValue.of(value)))))
    }
    val selectedDateFilter = Query.of(q => q.range(RangeQuery.of(r => r
      .field(query.getInterval.getColumn)
      .gte(JsonData.of(query.atStartOfDaySelected(tz).toString))
      .lt(JsonData.of(query.atEndOfDaySelected(tz).toString))
    )))

    val dateFilter = Option(query.getInterval.getRange) map { range =>
      Query.of(q => q.range(RangeQuery.of(r => r.field(query.getInterval.getColumn).gt(JsonData.of(range)))))
    }

    val userFilter = Option(query.getUser) map { user =>
      if (query.isUsertopic) {
        Query.of(q => q.term(TermQuery.of(t => t.field("topic_author").value(FieldValue.of(user.nick)))))
      } else {
        Query.of(q => q.term(TermQuery.of(t => t.field("author").value(FieldValue.of(user.nick)))))
      }
    }

    val queryFilters = (typeFilter ++ (if (query.isDateSelected) Option(selectedDateFilter) else dateFilter) ++ userFilter).toSeq

    val esQuery = wrapQuery(boost(processQueryString(query.getQ)), queryFilters)

    val sectionFilter = Option(query.getSection) filter (_.nonEmpty) map { section =>
      Query.of(q => q.term(TermQuery.of(t => t.field("section").value(FieldValue.of(section)))))
    }

    val groupFilter = Option(query.getGroup) filter (_.nonEmpty) map { group =>
      Query.of(q => q.term(TermQuery.of(t => t.field("group").value(FieldValue.of(group)))))
    }

    val postFilters = (sectionFilter ++ groupFilter).toSeq

    val order = query.getSort match {
      case SearchOrder.Relevance =>
        Seq(
          SortOptions.of(s => s.score(sc => sc.order(SortOrder.Desc))),
          SortOptions.of(s => s.field(f => f.field("postdate").order(SortOrder.Desc)))
        )
      case SearchOrder.Date =>
        Seq(SortOptions.of(s => s.field(f => f.field("postdate").order(SortOrder.Desc))))
      case SearchOrder.DateReverse =>
        Seq(SortOptions.of(s => s.field(f => f.field("postdate").order(SortOrder.Asc))))
    }

    val request = new SearchRequest.Builder()
      .index(OpenSearchIndexService.MessageIndex)
      .source(s => s.filter(f => f.includes(Fields.asJava)))
      .query(esQuery)
      .sort(order.asJava)
      .aggregations("sections", a => a
        .terms(TermsAggregation.of(t => t.field("section").size(50)))
        .aggregations("groups", ga => ga.terms(TermsAggregation.of(t => t.field("group").size(50))))
      )
      .aggregations("tags", a => a
        .significantTerms(SignificantTermsAggregation.of(st => st.field("tag").minDocCount(30)))
      )
      .highlight(h => h
        .preTags("<em class=search-hl>")
        .postTags("</em>")
        .requireFieldMatch(false)
        .fields("title", HighlightField.of(hf => hf.numberOfFragments(0)))
        .fields("topicTitle", HighlightField.of(hf => hf.numberOfFragments(0)))
        .fields("message", HighlightField.of(hf => hf.numberOfFragments(1).fragmentSize(MessageFragment)))
      )
      .size(SearchRows)
      .from(query.getOffset)
      .postFilter(andFilters(postFilters))
      .timeout(s"${SearchTimeout.toSeconds}s")
      .trackTotalHits(t => t.enabled(true))
      .build()

    val response = elastic.search(request, classOf[MessageIndexDocument])
    buildResponse(query, response)
  }

  private def buildResponse(query: SearchServiceRequest, response: SearchResponse[MessageIndexDocument]): SearchServiceResponse = {
    var sectionFacetResponse: Option[Seq[FacetItem]] = None
    var groupFacetResponse: Option[Seq[FacetItem]] = None
    var foundTagsResponse: Option[Seq[TagRef]] = None

    if (response.aggregations != null) {
      val sectionsAgg = response.aggregations.get("sections")
      val sectionsTerms = sectionsAgg.sterms()
      val sectionsBucketsSeq = sectionsTerms.buckets.array().asScala

      if (sectionsBucketsSeq.size > 1 || !Strings.isNullOrEmpty(query.getSection)) {
        sectionFacetResponse = Some(buildSectionFacet(sectionsAgg, Option.apply(Strings.emptyToNull(query.getSection))))

        if (!Strings.isNullOrEmpty(query.getSection)) {
          val selectedSection = sectionsBucketsSeq.find(b => b.key == query.getSection)

          if (!Strings.isNullOrEmpty(query.getGroup)) {
            groupFacetResponse = buildGroupFacet(selectedSection, Some(query.getSection -> query.getGroup))
          } else {
            groupFacetResponse = buildGroupFacet(selectedSection, None)
          }
        }


      } else if (Strings.isNullOrEmpty(query.getSection) && sectionsBucketsSeq.size == 1) {
        val onlySection = sectionsBucketsSeq.head

        query.setSection(onlySection.key)

        groupFacetResponse = buildGroupFacet(Some(onlySection), None)
      }

      foundTagsResponse = Some(foundTags(response.aggregations))
    }

    SearchServiceResponse(
      hits = response.hits.hits.asScala.view.map(prepare).toVector,
      sectionFacet = sectionFacetResponse,
      groupFacet = groupFacetResponse,
      foundTags = foundTagsResponse,
      totalHits = response.hits.total.value,
      took = response.took)
  }

  private def andFilters(filters: Seq[Query]): Query = {
    filters match {
      case Seq()       => Query.of(q => q.matchAll(ma => ma))
      case Seq(single) => single
      case other       => Query.of(q => q.bool(BoolQuery.of(b => b.must(other.asJava))))
    }
  }

  private def prepare(doc: Hit[MessageIndexDocument]): SearchItem = {
    val source = doc.source

    val author = userService.getUserCached(source.author)

    val postdate = Instant.parse(source.postdate)

    val comment = source.isComment

    val tags: Seq[TagRef] = if (comment) {
      Seq.empty
    } else {
      source.tags.map(tag => TagService.tagRef(tag))
    }

    val docScore = if (doc.score != null) doc.score.toFloat else 0f

    SearchItem(
      title = getTitle(doc),
      postdate = Date.from(postdate),
      user = author,
      url = getUrl(doc),
      score = docScore,
      comment = comment,
      message = getMessage(doc),
      tags = tags.asJava
    )
  }

  private def getTitle(doc: Hit[MessageIndexDocument]): String = {
    val highlight = Option(doc.highlight).map(_.asScala.toMap).getOrElse(Map.empty)
    val source = doc.source

    val itemTitle = highlight.get("title").flatMap(_.asScala.headOption)
      .orElse(source.title.map { v => StringUtil.escapeHtml(v) })

    itemTitle.filter(_.trim.nonEmpty).orElse(
      highlight.get("topic_title").flatMap(_.asScala.headOption))
      .getOrElse(StringUtil.escapeHtml(source.topicTitle))
  }

  private def getMessage(doc: Hit[MessageIndexDocument]): String = {
    val highlight = Option(doc.highlight).map(_.asScala.toMap).getOrElse(Map.empty)
    val source = doc.source

    val html = highlight.get("message").flatMap(_.asScala.headOption) getOrElse {
      source.message.take(MessageFragment)
    }

    Jsoup.clean(html, siteConfig.getSecureUrl, TextSafelist)
  }

  private def getUrl(doc: Hit[MessageIndexDocument]): String = {
    val source = doc.source
    val section = source.section
    val msgid = doc.id

    val comment = source.isComment
    val topic = source.topicId
    val group = source.group

    if (comment) {
      val builder = UriComponentsBuilder.fromPath("/{section}/{group}/{msgid}?cid={cid}")
      builder.buildAndExpand(section, group, Integer.valueOf(topic), msgid).toUriString
    } else {
      val builder = UriComponentsBuilder.fromPath("/{section}/{group}/{msgid}")
      builder.buildAndExpand(section, group, Integer.valueOf(topic)).toUriString
    }
  }

  private def buildSectionFacet(sectionsAgg: Aggregate, selected:Option[String]): Seq[FacetItem] = {
    def mkItem(urlName: String, count: Long) = {
      val name = sectionService.nameToSection.get(urlName).map(_.getName).getOrElse(urlName).toLowerCase
      FacetItem(urlName, s"$name ($count)")
    }

    val sectionsTerms = sectionsAgg.sterms()
    val sectionsBuckets = sectionsTerms.buckets.array().asScala

    val items = sectionsBuckets.map(entry => mkItem(entry.key, entry.docCount)).toSeq

    val missing = selected.filter(key => items.forall(_.key !=key)).map(mkItem(_, 0)).toSeq

    val allDocCount = sectionsBuckets.foldLeft(0L)((sum, b) => sum + b.docCount)
    val all = FacetItem("", s"все ($allDocCount)")

    all +: (missing ++ items)
  }

  private def buildGroupFacet(maybeSection: Option[StringTermsBucket], selected:Option[(String, String)]): Option[Seq[FacetItem]] = {
    def mkItem(section:Section, groupUrlName:String, count:Long) = {
      val group = groupDao.getGroup(section, groupUrlName)
      val name = group.title.toLowerCase
      FacetItem(groupUrlName, s"$name ($count)")
    }

    val facetItems = maybeSection.map { sectionBucket =>
      val sectionName = sectionBucket.key
      val groups = sectionBucket.aggregations.get("groups").sterms()
      val section = sectionService.getSectionByName(sectionName)

      groups.buckets.array().asScala.map(entry =>
        mkItem(section, entry.key, entry.docCount)
      ).toSeq
    }.getOrElse(Seq.empty)

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

  private def foundTags(agg: util.Map[String, Aggregate]): Seq[TagRef] = {
    val tags = agg.get("tags").sigsterms()

    tags.buckets.array().asScala.map(bucket => TagService.tagRef(bucket.key)).toSeq
  }
}

object SearchService {
  val SearchRows = 25
  private val MessageFragment = 16384
  private val RecentBoost = 2
  private val SearchTimeout: FiniteDuration = 1.minute

  private val Fields = Seq("title", "topic_title", "author", "postdate", "topic_id",
    "section", "message", "group", "is_comment", "tag")

  private val TextSafelist: Safelist = Safelist.relaxed().addAttributes(":all", "class")
}
