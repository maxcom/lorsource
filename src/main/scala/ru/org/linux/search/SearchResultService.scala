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
import ru.org.linux.section.SectionService
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
      val fullMessage = doc.getFields.get("message").getValue[String]
      if (fullMessage.length > SearchViewer.MESSAGE_FRAGMENT) {
        fullMessage.substring(0, SearchViewer.MESSAGE_FRAGMENT)
      } else {
        fullMessage
      }
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

  def buildSectionFacet(sectionFacet: Filter): java.util.List[FacetItem] = {
    val agg = sectionFacet.getAggregations.get[Terms]("sections")

    val items = for (entry <- agg.getBuckets.toSeq) yield {
      val urlName = entry.getKey
      val name = sectionService.nameToSection.get(urlName).map(_.getName).getOrElse(urlName).toLowerCase
      new FacetItem(entry.getKey, name + " (" + entry.getDocCount + ')')
    }

    val all = new FacetItem("", s"все (${sectionFacet.getDocCount})")

    all +: items
  }

  def buildGroupFacet(selectedSection: Terms.Bucket): java.util.List[FacetItem] = {
    val groups = selectedSection.getAggregations.get[Terms]("groups")

    if (groups.getBuckets.size > 1) {
      val all = new FacetItem("", s"все (${selectedSection.getDocCount})")
      val section = sectionService.getSectionByName(selectedSection.getKey)

      val items = for (entry <- groups.getBuckets.toSeq) yield {
        val groupUrlName = entry.getKey
        val group = groupDao.getGroup(section, groupUrlName)
        val name = group.getTitle.toLowerCase
        new FacetItem(groupUrlName, name + " (" + entry.getDocCount + ')')
      }

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


