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
package ru.org.linux.topic

import com.google.common.base.Strings
import org.apache.commons.text.StringEscapeUtils
import org.joda.time.DateTime
import ru.org.linux.group.Group
import ru.org.linux.reaction.{ReactionDao, Reactions}
import ru.org.linux.section.Section
import ru.org.linux.section.Section.{SECTION_ARTICLES, SECTION_NEWS}
import ru.org.linux.topic.TopicPermissionService.POSTSCORE_HIDE_COMMENTS
import ru.org.linux.user.User
import ru.org.linux.util.{StringUtil, URLUtil}

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.sql.{ResultSet, Timestamp}
import java.time.Instant
import javax.annotation.Nullable
import scala.beans.{BeanProperty, BooleanBeanProperty}

case class Topic(@BeanProperty id: Int, @BeanProperty postscore: Int, @BooleanBeanProperty sticky: Boolean,
                 @Nullable @BeanProperty linktext: String, @Nullable @BeanProperty url: String,
                 @BeanProperty title: String, @BeanProperty authorUserId: Int, @BeanProperty groupId: Int,
                 @BooleanBeanProperty deleted: Boolean, @BooleanBeanProperty expired: Boolean,
                 @BeanProperty commitby: Int, @BeanProperty postdate: Timestamp,
                 @BeanProperty @Nullable commitDate: Timestamp, @BeanProperty groupUrl: String,
                 @BeanProperty lastModified: Timestamp, @BeanProperty sectionId: Int,
                 @BeanProperty commentCount: Int, @BooleanBeanProperty commited: Boolean,
                 @BooleanBeanProperty notop: Boolean, @BeanProperty userAgentId: Int, @BeanProperty postIP: String,
                 @BooleanBeanProperty resolved: Boolean, @BooleanBeanProperty minor: Boolean,
                 @BooleanBeanProperty draft: Boolean, @BooleanBeanProperty allowAnonymous: Boolean,
                 reactions: Reactions, @Nullable expireDate: Timestamp, @BeanProperty openWarnings: Int) {
  def getTitleUnescaped: String = StringEscapeUtils.unescapeHtml4(title)

  def getPageCount(messages: Int): Int = Math.ceil(commentCount / messages.toDouble).toInt

  /**
   * Дата размещения сообщения на сайте
   *
   * @return postdate для постмодерируемых и commitdate для премодерируемых, прошедших модерацию
   */
  def getEffectiveDate: DateTime = if (commited && commitDate != null) {
    new DateTime(commitDate.getTime)
  } else {
    new DateTime(postdate.getTime)
  }

  def getLink: String =
    Section.getSectionLink(sectionId) + URLEncoder.encode(groupUrl, StandardCharsets.UTF_8) + '/' + id

  def getLinkPage(page: Int): String = {
    if (page == 0) {
      getLink
    } else {
      Section.getSectionLink(sectionId) + URLEncoder.encode(groupUrl, StandardCharsets.UTF_8) + '/' + id + "/page" + page
    }
  }

  def isCommentsHidden: Boolean = postscore == POSTSCORE_HIDE_COMMENTS

  def withId(id: Int): Topic = copy(id = id)
}

object Topic {
  def pageCount(commentCount: Int, messages: Int): Int = Math.ceil(commentCount / messages.toDouble).toInt

  def fromResultSet(rs: ResultSet): Topic = {
    val postscore = if (rs.getObject("postscore") == null) {
      TopicPermissionService.POSTSCORE_UNRESTRICTED
    } else {
      rs.getInt("postscore")
    }

    val sticky = rs.getBoolean("sticky")

    Topic(
      id = rs.getInt("msgid"),
      postscore = postscore,
      sticky = sticky,
      linktext = rs.getString("linktext"),
      url = rs.getString("url"),
      title = StringUtil.makeTitle(rs.getString("title")),
      authorUserId = rs.getInt("userid"),
      groupId = rs.getInt("guid"),
      deleted = rs.getBoolean("deleted"),
      expired = !sticky && rs.getBoolean("expired"),
      commitby = rs.getInt("commitby"),
      postdate = rs.getTimestamp("postdate"),
      commitDate = rs.getTimestamp("commitdate"),
      groupUrl = rs.getString("urlname"),
      lastModified = rs.getTimestamp("lastmod"),
      sectionId = rs.getInt("section"),
      commentCount = rs.getInt("stat1"),
      commited = rs.getBoolean("moderate"),
      notop = rs.getBoolean("notop"),
      userAgentId = rs.getInt("ua_id"),
      postIP = rs.getString("postip"),
      resolved = rs.getBoolean("resolved"),
      minor = rs.getBoolean("minor"),
      draft = rs.getBoolean("draft"),
      allowAnonymous = rs.getBoolean("allow_anonymous"),
      reactions = ReactionDao.parse(rs.getString("reactions")),
      expireDate = if (!sticky) rs.getTimestamp("expire_date") else null,
      openWarnings = rs.getInt("open_warnings"))
  }

  def fromAddRequest(form: AddTopicRequest, user: User, postIP: String): Topic = {
    val group = form.group

    Topic(
      userAgentId = 0,
      postIP = postIP,
      groupId = form.group.id,
      linktext =  if (form.linktext != null) StringUtil.escapeHtml(form.linktext) else null,
      url = if (!Strings.isNullOrEmpty(form.url)) URLUtil.fixURL(form.url) else null,
      title = if (form.title!=null) StringUtil.escapeHtml(form.title) else "",
      sectionId = group.sectionId,
      // Defaults
      id = 0,
      postscore = 0,
      sticky = false,
      deleted = false,
      expired = false,
      commitby = 0,
      postdate = Timestamp.from(Instant.now()),
      commitDate = null,
      groupUrl = group.urlName,
      lastModified = Timestamp.from(Instant.now()),
      commentCount = 0,
      commited = false,
      notop = false,
      authorUserId = user.getId,
      resolved = false,
      minor = false,
      draft = form.isDraftMode,
      allowAnonymous = form.allowAnonymous,
      reactions = Reactions.empty,
      expireDate = null,
      openWarnings = 0)
  }

  def fromEditRequest(group: Group, original: Topic, form: EditTopicRequest, publish: Boolean): Topic = {
    val sectionId = group.sectionId

    val minor: Boolean = if (sectionId == SECTION_NEWS || sectionId == SECTION_ARTICLES) {
      form.minor
    } else {
      original.minor
    }

    Topic(
      userAgentId = original.userAgentId,
      postIP = original.postIP,
      groupId = original.groupId,
      linktext = if (form.linktext != null && group.linksAllowed) form.linktext else original.linktext,
      url = if (form.url != null && group.linksAllowed) URLUtil.fixURL(form.url) else original.url,
      title = if (form.title != null) StringUtil.escapeHtml(form.title) else original.title,
      resolved = original.resolved,
      sectionId = sectionId,
      id = original.id,
      postscore = original.postscore,
      sticky = original.sticky,
      deleted = original.deleted,
      expired = original.expired,
      commitby = original.commitby,
      postdate = original.postdate,
      commitDate = original.commitDate,
      groupUrl = original.groupUrl,
      lastModified = Timestamp.from(Instant.now()),
      commentCount = original.commentCount,
      commited = original.commited,
      notop = original.notop,
      authorUserId = original.authorUserId,
      draft = if (publish) false else original.draft,
      minor = minor,
      allowAnonymous = original.allowAnonymous,
      reactions = original.reactions,
      expireDate = original.expireDate,
      openWarnings = original.openWarnings)
  }
}