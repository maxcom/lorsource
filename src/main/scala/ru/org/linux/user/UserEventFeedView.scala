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
package ru.org.linux.user

import com.rometools.rome.feed.synd.{SyndContentImpl, SyndEntryImpl, SyndFeed}
import org.apache.commons.text.StringEscapeUtils
import ru.org.linux.spring.AbstractRomeView
import ru.org.linux.spring.SiteConfig
import ru.org.linux.util.StringUtil

import java.util.ArrayList
import scala.jdk.CollectionConverters.*

class UserEventFeedView(siteConfig: SiteConfig) extends AbstractRomeView:

  override protected def createFeed(feed: SyndFeed, model: java.util.Map[?, ?]): Unit =
    val list = model.get("topicsList").asInstanceOf[java.util.List[PreparedUserEvent]].asScala.toSeq
    val s = s"Уведомления пользователя ${model.get("nick")}"
    feed.setTitle(s)
    feed.setLink(siteConfig.getSecureUrl)
    feed.setUri(siteConfig.getSecureUrl)
    feed.setAuthor("")
    feed.setDescription(s)

    val lastModified =
      if list.nonEmpty then
        list.head.event.eventDate
      else
        new java.util.Date()

    feed.setPublishedDate(lastModified)

    val entries = new ArrayList[com.rometools.rome.feed.synd.SyndEntry]()
    feed.setEntries(entries)

    for preparedUserEvent <- list do
      val item = preparedUserEvent.event

      val feedEntry = new SyndEntryImpl()
      feedEntry.setPublishedDate(item.eventDate)
      feedEntry.setTitle(StringEscapeUtils.unescapeHtml4(item.subj))

      if item.cid != 0 then
        feedEntry.setAuthor(preparedUserEvent.author.nick)

      val link = siteConfig.getSecureUrlWithoutSlash + preparedUserEvent.getLink

      feedEntry.setLink(link)
      feedEntry.setUri(preparedUserEvent.event.id.toString)

      val text = Option(preparedUserEvent.getMessageText).map(StringUtil.removeInvalidXmlChars).getOrElse("")

      if item.eventType == UserEventFilterEnum.REACTION then
        val message = new SyndContentImpl()
        val reactionNote = s"@${preparedUserEvent.author.nick} поставил ${item.reaction}"
        message.setValue(s"$reactionNote<br>$text")
        message.setType("text/html")
        feedEntry.setDescription(message)
      else if text.nonEmpty then
        val message = new SyndContentImpl()
        message.setValue(text)
        message.setType("text/html")
        feedEntry.setDescription(message)

      entries.add(feedEntry)
