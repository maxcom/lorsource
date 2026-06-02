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

package ru.org.linux.group

import com.typesafe.scalalogging.StrictLogging
import org.springframework.stereotype.Repository
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.section.Section
import ru.org.linux.topic.TopicPermissionService
import scalikejdbc.*

@Repository
class GroupDao(springDB: SpringDB) extends StrictLogging:

  private def buildGroup(rs: WrappedResultSet): Group =
    val restrictTopics = rs.intOpt("restrict_topics").getOrElse(TopicPermissionService.POSTSCORE_UNRESTRICTED)
    Group(
      premoderated = rs.boolean("moderate"),
      pollPostAllowed = rs.boolean("vote"),
      linksAllowed = rs.boolean("havelink"),
      sectionId = rs.int("section"),
      defaultLinkText = rs.string("linktext"),
      urlName = rs.string("urlname"),
      image = rs.string("image"),
      topicRestriction = restrictTopics,
      commentsRestriction = rs.int("restrict_comments"),
      id = rs.int("id"),
      stat3 = rs.int("stat3"),
      resolvable = rs.boolean("resolvable"),
      title = rs.string("title"),
      info = rs.string("info"),
      longInfo = rs.string("longinfo")
    )

  /** Получить объект группы по идентификатору.
    *
    * @param id
    *   идентификатор группы
    * @return
    *   объект группы
    * @throws GroupNotFoundException
    *   если группа не существует
    */
  def getGroup(id: Int): Group =
    springDB.run:
      sql"""SELECT sections.moderate, vote, section, havelink, linktext, title, urlname, image,
            groups.restrict_topics, restrict_comments, stat3, groups.id, groups.info, groups.longinfo, groups.resolvable
            FROM groups, sections WHERE groups.id=$id AND groups.section=sections.id"""
        .map(buildGroup)
        .single
        .apply()
        .getOrElse(throw new GroupNotFoundException(s"Группа $id не существует"))

  /** Получить список групп в указанной секции.
    *
    * @param section
    *   объект секции.
    * @return
    *   список групп
    */
  def getGroups(section: Section): Seq[Group] =
    springDB.run:
      sql"""SELECT sections.moderate, vote, section, havelink, linktext, title, urlname, image,
            groups.restrict_topics, restrict_comments, stat3, groups.id, groups.info, groups.longinfo, groups.resolvable
            FROM groups, sections WHERE sections.id=${section.id} AND groups.section=sections.id ORDER BY id"""
        .map(buildGroup)
        .list
        .apply()

  /** Получить объект группы в указанной секции по имени группы.
    *
    * @param section
    *   объект секции.
    * @param name
    *   имя группы
    * @return
    *   объект группы
    * @throws GroupNotFoundException
    *   если группа не существует
    */
  def getGroup(section: Section, name: String): Group =
    val group = getGroupOpt(section, name, false)

    if group.isEmpty then
      logger.info(s"Group '$name' not found in section ${section.getUrlName}")
      throw new GroupNotFoundException("group not found")
    else
      group.get

  /** Получить объект группы в указанной секции по имени группы.
    *
    * @param section
    *   объект секции.
    * @param name
    *   имя группы
    * @param allowNumber
    *   разрешить поиск по числовому id
    * @return
    *   объект группы (optional)
    */
  def getGroupOpt(section: Section, name: String, allowNumber: Boolean): Option[Group] =
    if allowNumber && name.nonEmpty && name.forall(_.isDigit) then
      springDB.run:
        sql"SELECT id FROM groups WHERE section=${section.id} AND id=${name.toInt}"
          .map(rs => rs.int("id"))
          .single
          .apply()
      match
        case Some(id) =>
          Some(getGroup(id))
        case None =>
          None
    else if name.forall(c => c.isLetterOrDigit || c == '-' || c == '_') && name.nonEmpty then
      springDB.run:
        sql"SELECT id FROM groups WHERE section=${section.id} AND urlname=$name".map(rs => rs.int("id")).single.apply()
      match
        case Some(id) =>
          Some(getGroup(id))
        case None =>
          logger.debug(s"Group '$name' not found in section ${section.getUrlName}")
          None
    else
      None

  /** Изменить настройки группы.
    *
    * @param group
    *   объект группы
    * @param title
    *   Заголовок группы
    * @param info
    *   дополнительная информация
    * @param longInfo
    *   расширенная дополнительная информация
    * @param resolvable
    *   можно ли ставить темам признак "тема решена"
    * @param urlName
    *   имя группы в URL
    */
  def setParams(
      group: Group,
      title: String,
      info: String,
      longInfo: String,
      resolvable: Boolean,
      urlName: String): Unit =
    val infoOpt = Option.when(info.nonEmpty)(info)
    val longInfoOpt = Option.when(longInfo.nonEmpty)(longInfo)
    springDB.run:
      sql"""UPDATE groups SET title=$title, info=$infoOpt, longinfo=$longInfoOpt,
            resolvable=$resolvable, urlname=$urlName WHERE id=${group.id}""".update.apply()
