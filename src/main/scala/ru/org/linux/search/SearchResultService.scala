package ru.org.linux.search

import ru.org.linux.user.{UserDao, User}
import scala.beans.BeanProperty
import org.springframework.stereotype.Service
import org.elasticsearch.search.SearchHit
import ru.org.linux.util.StringUtil
import org.springframework.beans.factory.annotation.Autowired
import org.joda.time.format.ISODateTimeFormat
import ru.org.linux.util.URLUtil._
import org.springframework.web.util.UriComponentsBuilder
import com.typesafe.scalalogging.slf4j.Logging
import ru.org.linux.tag.TagRef
import scala.collection.JavaConversions._
import ru.org.linux.topic.TopicTagService
import org.joda.time.DateTime

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
  userDao:UserDao
) extends Logging {
  private val isoDateTime = ISODateTimeFormat.dateTime

  def prepare(doc:SearchHit):SearchItem = {
    val author = userDao.getUser(doc.getFields.get("author").getValue[String])

    val postdate = isoDateTime.parseDateTime(doc.getFields.get("postdate").getValue[String])

    val comment = doc.getFields.get("is_comment").getValue[Boolean]

    val tags = if (comment) {
      Seq()
    } else {
      doc.getFields.get("tag").getValue[java.util.List[String]].map(
        tag => TopicTagService.tagRef(tag)
      )
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
    val section = doc.getFields.get("section").getValue[String]
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
          return "#"
      }
    } else {
      val comment = doc.getFields.get("is_comment").getValue[Boolean]
      val topic = doc.getFields.get("topic_id").getValue[Int]
      val group = doc.getFields.get("group").getValue[String]

      if (comment) {
        val builder = UriComponentsBuilder.fromPath("/{section}/{group}/{msgid}?cid={cid}")
        builder.buildAndExpand(section, group, new Integer(topic), msgid).toUriString
      } else {
        val builder = UriComponentsBuilder.fromPath("/{section}/{group}/{msgid}")
        builder.buildAndExpand(section, group, new Integer(topic)).toUriString
      }
    }
  }
}


