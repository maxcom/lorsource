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
package ru.org.linux.group

import ru.org.linux.section.Section
import ru.org.linux.topic.TopicPermissionService
import ru.org.linux.user.User

import javax.annotation.Nullable
import java.sql.Timestamp
import java.util
import scala.beans.{BeanProperty, BooleanBeanProperty}

case class PreparedTopicsListItem(@BeanProperty topicAuthor: User, @BeanProperty topicId: Int, stat1: Int,
                                  @BeanProperty groupTitle: String, @BeanProperty title: String,
                                  @BeanProperty lastCommentId: Int, @Nullable lastCommentBy: User,
                                  @BooleanBeanProperty resolved: Boolean, section: Int, groupUrlName: String,
                                  @BeanProperty postdate: Timestamp, @BooleanBeanProperty uncommited: Boolean,
                                  @BeanProperty pages: Int, @BeanProperty tags: util.List[String],
                                  @BooleanBeanProperty deleted: Boolean, @BooleanBeanProperty sticky: Boolean,
                                  topicPostscore: Int) {
  def getLastPageUrl: String = if (pages > 1) {
    getGroupUrl + topicId + "/page" + (pages - 1) + "?lastmod=" + lastCommentId
  } else {
    getGroupUrl + topicId + "?lastmod=" + lastCommentId
  }

  def getFirstPageUrl: String = if (pages <= 1) {
    getGroupUrl + topicId + "?lastmod=" + lastCommentId
  } else {
    getCanonicalUrl
  }

  def getCanonicalUrl: String = getGroupUrl + topicId

  def getGroupUrl: String = Section.getSectionLink(section) + groupUrlName + '/'

  def getCommentCount: Int = if (topicPostscore != TopicPermissionService.POSTSCORE_HIDE_COMMENTS) {
    stat1
  } else {
    0
  }

  def getAuthor: User = if (lastCommentBy != null) {
    lastCommentBy
  } else {
    topicAuthor
  }

  def isCommentsClosed: Boolean = topicPostscore >= TopicPermissionService.POSTSCORE_MODERATORS_ONLY
}