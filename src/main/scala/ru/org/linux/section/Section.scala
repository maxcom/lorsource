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

package ru.org.linux.section

import org.apache.commons.lang3.StringUtils
import ru.org.linux.topic.TopicPermissionService

import java.sql.ResultSet
import scala.beans.{BeanProperty, BooleanBeanProperty}

case class Section(
    @BeanProperty
    name: String,
    @BooleanBeanProperty
    imagepost: Boolean,
    @BooleanBeanProperty
    moderate: Boolean,
    @BeanProperty
    id: Int,
    @BooleanBeanProperty
    votepoll: Boolean,
    @BeanProperty
    scrollMode: SectionScrollModeEnum,
    @BeanProperty
    topicsRestriction: Int,
    @BooleanBeanProperty
    imageAllowed: Boolean):
  import Section.*

  def getTitle: String = name

  def isPremoderated: Boolean = moderate

  def isPollPostAllowed: Boolean = votepoll

  def getUrlName: String = Section.getUrlName(id)

  def getArchiveLink(year: Int, month: Int): String = s"$getArchiveLink$year/$month/"

  def getArchiveLink: String =
    if id == Forum then
      null
    else
      s"${Section.getSectionLink(id)}archive/"

  def getNewsViewerLink: String =
    if id == Forum then
      "/forum/lenta/"
    else
      Section.getSectionLink(id)

  def getSectionLink: String = Section.getSectionLink(id)

  def uncommitedName: String =
    id match
      case Section.Gallery =>
        "Неподтверждённые галереи"
      case _ =>
        "Неподтверждённые " + name.toLowerCase

  def uncommitedNameShort: String =
    id match
      case Section.Gallery =>
        "неподтв. галереи"
      case _ =>
        "неподтв. " + name.toLowerCase

  def uncommitedNameShortCap: String = StringUtils.capitalize(uncommitedNameShort)

object Section:
  final val Forum = 2
  final val Gallery = 3
  final val News = 1
  final val Polls = 5
  final val Articles = 6

  private val sections: Map[Int, String] =
    Map(News -> "news", Forum -> "forum", Gallery -> "gallery", Polls -> "polls", Articles -> "articles")

  def getCommentPostscore(id: Int): Int = 
    id match
      case Forum | News =>
        TopicPermissionService.POSTSCORE_UNRESTRICTED
      case Articles | Gallery | Polls =>
        45
      case _ =>
        50

  def fromResultSet(rs: ResultSet): Section =
    new Section(
      rs.getString("name"),
      rs.getBoolean("imagepost"),
      rs.getBoolean("moderate"),
      rs.getInt("id"),
      rs.getBoolean("vote"),
      SectionScrollModeEnum.valueOf(rs.getString("scroll_mode")),
      if !rs.wasNull() then
        rs.getInt("restrict_topics")
      else
        TopicPermissionService.POSTSCORE_UNRESTRICTED
      ,
      rs.getBoolean("imageallowed")
    )

  def getSectionLink(section: Int): String = "/" + getUrlName(section) + "/"

  def getUrlName(section: Int): String =
    sections.get(section) match
      case None =>
        throw new SectionNotFoundException(section)
      case Some(name) =>
        name
