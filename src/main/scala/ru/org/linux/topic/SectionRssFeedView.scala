/*
 *  Copyright 1998-2026 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ru.org.linux.topic

import com.rometools.rome.feed.synd.{SyndContentImpl, SyndEntry, SyndEntryImpl, SyndFeed}
import ru.org.linux.spring.{AbstractRomeView, SiteConfig}
import ru.org.linux.util.StringUtil

import java.util.ArrayList
import scala.jdk.CollectionConverters.*

class SectionRssFeedView(siteConfig: SiteConfig) extends AbstractRomeView:

  override protected def createFeed(feed: SyndFeed, model: java.util.Map[?, ?]): Unit =
    val messages = model.get("messages").asInstanceOf[java.util.List[PreparedTopic]].asScala.toSeq
    val ptitle = model.get("ptitle").asInstanceOf[String]

    val title = s"Linux.org.ru: $ptitle"
    feed.setTitle(title)
    feed.setDescription(title)
    feed.setLanguage("ru")
    feed.setLink(siteConfig.getSecureUrl)
    feed.setUri(siteConfig.getSecureUrl)
    feed.setPublishedDate(new java.util.Date())

    val entries = new ArrayList[SyndEntry]()
    feed.setEntries(entries)

    for preparedTopic <- messages do
      val topic = preparedTopic.message

      val feedEntry = new SyndEntryImpl()
      feedEntry.setAuthor(preparedTopic.author.nick)
      val link = siteConfig.getSecureUrlWithoutSlash + topic.getLink
      feedEntry.setLink(link)
      feedEntry.setUri(link)
      feedEntry.setTitle(topic.getTitle)
      feedEntry.setPublishedDate(
        if topic.commitDate != null then
          topic.commitDate
        else
          topic.postdate)

      val content = new SyndContentImpl()
      content.setType("text/html")
      content.setValue(renderDescription(preparedTopic))
      feedEntry.setDescription(content)

      entries.add(feedEntry)
    end for
  end createFeed

  private def renderDescription(preparedTopic: PreparedTopic): String =
    val sb = new StringBuilder

    if !preparedTopic.images.isEmpty then
      val image = preparedTopic.images.get(0)
      sb.append(s"""<img src="${image.mediumName}" srcset="${image.getSrcset}">""")
    end if

    sb.append(preparedTopic.processedMessage)

    if preparedTopic.section.isPollPostAllowed && preparedTopic.poll != null then
      sb.append(preparedTopic.poll.renderPoll)
    end if

    if !preparedTopic.tags.isEmpty then
      sb.append(renderTags(preparedTopic.tags.asScala.toSeq))
    end if

    StringUtil.removeInvalidXmlChars(sb.toString)
  end renderDescription

  private def renderTags(tags: Seq[ru.org.linux.tag.TagRef]): String =
    val buf = new StringBuilder("<p class=\"tags\"><i class=\"icon-tag\"></i>&nbsp;")

    buf.append(
      tags
        .map { el =>
          if el.url.isDefined then
            s"""<a class=tag rel=tag href="${el.url.get}">${StringUtil.escapeHtml(el.name)}</a>"""
          else
            s"<span class=tag>${StringUtil.escapeHtml(el.name)}</span>"
        }
        .mkString(", "))

    buf.append("</p>")
    buf.toString
  end renderTags
end SectionRssFeedView
