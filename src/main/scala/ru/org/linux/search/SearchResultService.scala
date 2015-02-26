package ru.org.linux.search

import com.typesafe.scalalogging.slf4j.StrictLogging
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.filter.Filter
import org.elasticsearch.search.aggregations.bucket.significant.SignificantTerms
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import ru.org.linux.group.GroupDao
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.tag.{TagRef, TagService}
import ru.org.linux.user.{User, UserDao}
import ru.org.linux.util.StringUtil
import ru.org.linux.util.URLUtil._

import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

case class SearchItem (
  @BeanProperty title:String,
  @BeanProperty postdate:DateTime,
  @BeanProperty user:User, // TODO use UserRef
  @BeanProperty message:String,
  @BeanProperty url:String,
  @BeanProperty score:Float,
  @BeanProperty comment:Boolean,
  @BeanProperty tags:java.util.List[TagRef]
)

@Service
class SearchResultsService @Autowired() (
  userDao:UserDao, sectionService:SectionService, groupDao:GroupDao
) extends StrictLogging {
  import ru.org.linux.search.SearchResultsService._

  def prepareAll(docs:java.lang.Iterable[SearchHit]) = (docs map prepare).asJavaCollection

  def prepare(doc:SearchHit):SearchItem = {
    val author = userDao.getUser(doc.getFields.get("author").getValue[String])

    val postdate = isoDateTime.parseDateTime(doc.getFields.get("postdate").getValue[String])

    val comment = doc.getFields.get("is_comment").getValue[Boolean]

    val tags = if (comment) {
      Seq()
    } else {
      if (doc.getFields.containsKey("tag")) {
        doc.getFields.get("tag").getValues.map(
          tag => TagService.tagRef(tag.toString))
      } else {
        Seq()
      }
    }

    SearchItem(
      title = getTitle(doc),
      postdate = postdate,
      user = author,
      url = getUrl(doc),
      score = doc.getScore,
      comment = comment,
      message = getMessage(doc),
      tags = tags
    )
  }

  private def getTitle(doc: SearchHit):String = {
    val itemTitle = if (doc.getHighlightFields.containsKey("title")) {
      Some(doc.getHighlightFields.get("title").fragments()(0).string)
    } else if (doc.getFields.containsKey("title")) {
      Some(StringUtil.escapeHtml(doc.getFields.get("title").getValue[String]))
    } else {
      None
    }

    val topicTitle = if (doc.getHighlightFields.containsKey("topic_title")) {
      doc.getHighlightFields.get("topic_title").fragments()(0).string
    } else {
      StringUtil.escapeHtml(doc.getFields.get("topic_title").getValue[String])
    }

    itemTitle.filter(!_.trim.isEmpty).getOrElse(topicTitle)
  }

  private def getMessage(doc: SearchHit): String = {
    if (doc.getHighlightFields.containsKey("message")) {
      doc.getHighlightFields.get("message").fragments()(0).string
    } else {
      val rawMessage = doc.getFields.get("message").getValue[String].take(SearchViewer.MessageFragment)

      StringUtil.escapeHtml(rawMessage)
    }
  }

  private def getUrl(doc:SearchHit): String = {
    val section = SearchResultsService.section(doc)
    val msgid = doc.getId

    if ("wiki" == section) {
      val virtualWiki = {
        val msgIds = msgid.split("-")
        if (msgIds.length != 2) {
          throw new RuntimeException("Invalid wiki ID")
        }

        msgIds(0)
      }

      val title = doc.getFields.get("title").getValue[String]

      try {
        buildWikiURL(virtualWiki, title)
      } catch {
        case e: Exception =>
          logger.warn(s"Fail build topic url for $title in $virtualWiki")
          "#"
      }
    } else {
      val comment = doc.getFields.get("is_comment").getValue[Boolean]
      val topic = doc.getFields.get("topic_id").getValue[Int]
      val group = SearchResultsService.group(doc)

      if (comment) {
        val builder = UriComponentsBuilder.fromPath("/{section}/{group}/{msgid}?cid={cid}")
        builder.buildAndExpand(section, group, new Integer(topic), msgid).toUriString
      } else {
        val builder = UriComponentsBuilder.fromPath("/{section}/{group}/{msgid}")
        builder.buildAndExpand(section, group, new Integer(topic)).toUriString
      }
    }
  }

  def buildSectionFacet(sectionFacet: Filter, selected:Option[String]): java.util.List[FacetItem] = {
    def mkItem(urlName:String, count:Long) = {
      val name = sectionService.nameToSection.get(urlName).map(_.getName).getOrElse(urlName).toLowerCase
      FacetItem(urlName, s"$name ($count)")
    }

    val agg = sectionFacet.getAggregations.get[Terms]("sections")

    val items = for (entry <- agg.getBuckets.toSeq) yield {
      mkItem(entry.getKey, entry.getDocCount)
    }

    val missing = selected.filter(key ⇒ items.forall(_.key !=key)).map(mkItem(_, 0)).toSeq

    val all = FacetItem("", s"все (${sectionFacet.getDocCount})")

    all +: (missing ++ items)
  }

  def buildGroupFacet(maybeSection: Option[Terms.Bucket], selected:Option[(String, String)]): java.util.List[FacetItem] = {
    def mkItem(section:Section, groupUrlName:String, count:Long) = {
      val group = groupDao.getGroup(section, groupUrlName)
      val name = group.getTitle.toLowerCase
      FacetItem(groupUrlName, s"$name ($count)")
    }

    val facetItems = for {
      selectedSection <- maybeSection.toSeq
      groups = selectedSection.getAggregations.get[Terms]("groups")
      section = sectionService.getSectionByName(selectedSection.getKey)
      entry <- groups.getBuckets.toSeq
    } yield {
      mkItem(section, entry.getKey, entry.getDocCount)
    }

    val missing = selected.filter(key ⇒ facetItems.forall(_.key != key._2)).map(p ⇒
      mkItem(sectionService.getSectionByName(p._1), p._2, 0)
    ).toSeq

    val items = facetItems ++ missing

    if (items.size > 1 || selected.isDefined) {
      val all = FacetItem("", s"все (${maybeSection.map(_.getDocCount).getOrElse(0)})")

      all +: items
    } else {
      null
    }
  }

  def foundTags(agg:Aggregations):java.util.List[TagRef] = {
    val tags = agg.get[SignificantTerms]("tags")

    tags.getBuckets.map(bucket => TagService.tagRef(bucket.getKey)).toSeq
  }
}

object SearchResultsService {
  private val isoDateTime = ISODateTimeFormat.dateTime

  def postdate(doc:SearchHit) = isoDateTime.parseDateTime(doc.getFields.get("postdate").getValue[String])
  def section(doc:SearchHit) = doc.getFields.get("section").getValue[String]
  def group(doc:SearchHit) = doc.getFields.get("group").getValue[String]
}

case class FacetItem(@BeanProperty key:String, @BeanProperty label:String)


