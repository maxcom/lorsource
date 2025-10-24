/*
 * Copyright 1998-2025 Linux.org.ru
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

import com.sksamuel.elastic4s.requests.searches.SearchHit
import com.sksamuel.elastic4s.requests.searches.aggs.responses.{Aggregations, FilterAggregationResult}
import com.sksamuel.elastic4s.requests.searches.aggs.responses.bucket.{TermBucket, Terms}
import com.typesafe.scalalogging.StrictLogging
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import ru.org.linux.group.GroupDao
import ru.org.linux.search.SearchResultsService.TextSafelist
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.spring.SiteConfig
import ru.org.linux.tag.{TagRef, TagService}
import ru.org.linux.user.{User, UserService}
import ru.org.linux.util.StringUtil

import java.time.Instant
import java.util.Date
import scala.beans.BeanProperty
import scala.jdk.CollectionConverters.*

case class SearchItem (
  @BeanProperty title: String,
  @BeanProperty postdate: Date,
  @BeanProperty user: User,
  @BeanProperty message: String,
  @BeanProperty url: String,
  @BeanProperty score: Float,
  @BeanProperty comment: Boolean,
  @BeanProperty tags: java.util.List[TagRef])

@Service
class SearchResultsService(userService: UserService, sectionService: SectionService, groupDao: GroupDao,
                           siteConfig: SiteConfig) extends StrictLogging {
  def prepareAll(docs: Iterable[SearchHit]): Iterable[SearchItem] = docs.map(prepare)

  def prepare(doc: SearchHit):SearchItem = {
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
      doc.sourceAsMap("message").asInstanceOf[String].take(SearchViewer.MessageFragment)
    }

    Jsoup.clean(html, siteConfig.getSecureUrl, TextSafelist)
  }

  private def getUrl(doc: SearchHit): String = {
    val section = SearchResultsService.section(doc)
    val msgid = doc.id

    val comment = doc.sourceAsMap("is_comment").asInstanceOf[Boolean]
    val topic = doc.sourceAsMap("topic_id").asInstanceOf[Int]
    val group = SearchResultsService.group(doc)

    if (comment) {
      val builder = UriComponentsBuilder.fromPath("/{section}/{group}/{msgid}?cid={cid}")
      builder.buildAndExpand(section, group, Integer.valueOf(topic), msgid).toUriString
    } else {
      val builder = UriComponentsBuilder.fromPath("/{section}/{group}/{msgid}")
      builder.buildAndExpand(section, group, Integer.valueOf(topic)).toUriString
    }
  }

  def buildSectionFacet(sectionFacet: FilterAggregationResult, selected:Option[String]): java.util.List[FacetItem] = {
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

    (all +: (missing ++ items)).asJava
  }

  def buildGroupFacet(maybeSection: Option[TermBucket], selected:Option[(String, String)]): java.util.List[FacetItem] = {
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

      (all +: items).asJava
    } else {
      null
    }
  }

  def foundTags(agg: Aggregations): java.util.List[TagRef] = {
    val tags = agg.significantTerms("tags")

    tags.buckets.map(bucket => TagService.tagRef(bucket.key)).asJava
  }
}

object SearchResultsService {
  def postdate(doc: SearchHit): Instant = Instant.parse(doc.sourceAsMap("postdate").asInstanceOf[String])
  def section(doc: SearchHit): String = doc.sourceAsMap("section").asInstanceOf[String]
  def group(doc: SearchHit): String = doc.sourceAsMap("group").asInstanceOf[String]

  private val TextSafelist: Safelist = Safelist.relaxed().addAttributes(":all", "class")
}

case class FacetItem(@BeanProperty key:String, @BeanProperty label:String)


