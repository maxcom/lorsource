/*
 * Copyright 1998-2023 Linux.org.ru
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
package ru.org.linux.group

import ru.org.linux.section.Section
import ru.org.linux.topic.TopicPermissionService

import java.sql.ResultSet
import java.sql.SQLException
import scala.beans.{BeanProperty, BooleanBeanProperty}

object Group {
  @throws[SQLException]
  def buildGroup(rs: ResultSet): Group = {
    var restrict_topics = rs.getInt("restrict_topics")

    if (rs.wasNull) {
      restrict_topics = TopicPermissionService.POSTSCORE_UNRESTRICTED
    }

    new Group(
      premoderated = rs.getBoolean("moderate"),
      pollPostAllowed = rs.getBoolean("vote"),
      linksAllowed = rs.getBoolean("havelink"),
      sectionId = rs.getInt("section"),
      defaultLinkText = rs.getString("linktext"),
      urlName = rs.getString("urlname"),
      image = rs.getString("image"),
      topicRestriction = restrict_topics,
      commentsRestriction = rs.getInt("restrict_comments"),
      id = rs.getInt("id"),
      stat3 = rs.getInt("stat3"),
      resolvable = rs.getBoolean("resolvable"),
      title = rs.getString("title"),
      info = rs.getString("info"),
      longInfo = rs.getString("longinfo"))
  }
}

case class Group(@BooleanBeanProperty premoderated: Boolean, @BooleanBeanProperty pollPostAllowed: Boolean,
                 @BooleanBeanProperty linksAllowed: Boolean, @BeanProperty sectionId: Int,
                 defaultLinkText: String, @BeanProperty urlName: String, @BeanProperty image: String,
                 topicRestriction: Int, commentsRestriction: Int, @BeanProperty id: Int, @BeanProperty stat3: Int,
                 @BooleanBeanProperty resolvable: Boolean, @BeanProperty title: String, @BeanProperty info: String,
                 @BeanProperty longInfo: String) {
  def getSectionLink: String = Section.getSectionLink(sectionId)

  def getUrl: String = getSectionLink + urlName + '/'

  def getArchiveLink(year: Int, month: Int): String = getUrl + year + '/' + month + '/'

  def updated(newTitle: String, newInfo: String, newLongInfo: String): Group =
    this.copy(title = newTitle, info = newInfo, longInfo = newLongInfo)
}