/*
 * Copyright 1998-2022 Linux.org.ru
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

import com.typesafe.scalalogging.StrictLogging
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.org.linux.topic.TopicListDto.CommitMode.{COMMITED_ONLY, POSTMODERATED_ONLY, UNCOMMITED_ONLY}
import ru.org.linux.topic.TopicListDto.{DateLimitType, MiniNewsMode}
import ru.org.linux.user.User

import java.sql.ResultSet
import scala.jdk.CollectionConverters.*
import javax.annotation.Nullable
import javax.sql.DataSource
import scala.collection.mutable

object TopicListDao {
  /**
   * Создание условий выборки SQL-запроса.
   *
   * @param request объект, содержащий условия выборки
   * @return строка, содержащая условия выборки SQL-запроса
   */
  private def makeConditions(request: TopicListDto, paramsBuilder: mutable.Map[String, AnyRef]) = {
    val where = new StringBuilder("NOT deleted")

    if (paramsBuilder.contains("userid")) {
      where.append(" AND ((sections.moderate AND commitdate is not null) OR userid NOT IN (select ignored from ignore_list where userid=:userid))")
    }

    where.append(request.getCommitMode.getQueryPiece)

    val sections = request.getSections.asScala.filter(_ != 0)

    if (sections.nonEmpty) {
      where.append(" AND section in (:sections)")
      paramsBuilder.put("sections", sections.asJava)
    }

    if (request.getGroup != 0) {
      where.append(" AND groupid=:groupId")
      paramsBuilder.put("groupId", Integer.valueOf(request.getGroup))
    }

    if (!request.isIncludeAnonymous) {
      where.append(s" AND topics.userid != ${User.ANONYMOUS_ID}")
    }

    request.getDateLimitType match {
      case DateLimitType.BETWEEN =>
        where.append(" AND postdate>=:fromDate AND postdate<:toDate")
        paramsBuilder.put("fromDate", request.getFromDate)
        paramsBuilder.put("toDate", request.getToDate)
      case DateLimitType.FROM_DATE =>
        where.append(" AND postdate>=:fromDate")
        paramsBuilder.put("fromDate", request.getFromDate)
      case DateLimitType.NONE =>
    }

    if (request.getUserId != 0) {
      paramsBuilder.put("userId", Integer.valueOf(request.getUserId))

      if (request.isUserFavs) {
        where.append(" AND memories.userid=:userId")
      } else {
        where.append(" AND userid=:userId")
      }

      if (request.isUserFavs) {
        if (request.isUserWatches) {
          where.append(" AND watch ")
        } else {
          where.append(" AND NOT watch ")
        }
      }
    }

    if (request.isNotalks) {
      where.append(" AND not topics.groupid=8404")
    }

    if (request.isTech) {
      where.append(" AND not topics.groupid in (8404, 4068, 9326, 19405)")
    }

    request.getMiniNewsMode match {
      case MiniNewsMode.MAJOR => where.append(" AND NOT minor")
      case MiniNewsMode.MINOR => where.append(" AND minor")
      case MiniNewsMode.ALL =>
    }

    if (request.getTag != 0) {
      paramsBuilder.put("tagId", Integer.valueOf(request.getTag))
      where.append(" AND topics.id IN (SELECT msgid FROM tags WHERE tagid=:tagId)")
    }

    if (!request.isShowDraft) {
      where.append(" AND NOT topics.draft ")
    } else {
      where.append(" AND topics.draft ")
    }

    where
  }

  /**
   * Создание условий сортировки SQL-запроса.
   *
   * @param topicListDto объект, содержащий условия выборки
   * @return строка, содержащая условия сортировки
   */
  private def makeSortOrder(topicListDto: TopicListDto): String = {
    if (topicListDto.isUserFavs) {
      "ORDER BY memories.id DESC"
    } else {
      topicListDto.getCommitMode match {
        case COMMITED_ONLY => " ORDER BY commitdate DESC"
        case UNCOMMITED_ONLY | POSTMODERATED_ONLY => " ORDER BY postdate DESC"
        case _ => " ORDER BY COALESCE(commitdate, postdate) DESC"
      }
    }
  }

  /**
   * Создание ограничений размера результатов SQL-запроса.
   *
   * @param topicListDto объект, содержащий условия выборки
   * @return строка, содержащая смещение и количество записей
   */
  private def makeLimitAndOffset(topicListDto: TopicListDto) = {
    var limitStr = ""

    if (topicListDto.getLimit != null) {
      limitStr += " LIMIT " + topicListDto.getLimit.toString
    }

    if (topicListDto.getOffset != null) {
      limitStr += " OFFSET " + topicListDto.getOffset.toString
    }

    limitStr
  }
}

@Repository
class TopicListDao(ds: DataSource) extends StrictLogging {
  private val jdbcTemplate: JdbcTemplate = new JdbcTemplate(ds)
  private val namedJdbcTemplate: NamedParameterJdbcTemplate = new NamedParameterJdbcTemplate(ds)

  def getTopics(topicListDto: TopicListDto, @Nullable currentUser: User): java.util.List[Topic] = {
    val params = new mutable.HashMap[String, AnyRef]

    if (currentUser != null) {
      params.put("userid", Integer.valueOf(currentUser.getId))
    }

    val sort = TopicListDao.makeSortOrder(topicListDto)
    val limit = TopicListDao.makeLimitAndOffset(topicListDto)

    val query = new StringBuilder

    query.append(
      """SELECT postdate, topics.id as msgid, topics.userid, topics.title, topics.groupid as guid, topics.url,
        | topics.linktext, ua_id, urlname, section, topics.sticky, topics.postip,
        | COALESCE(commitdate, postdate)<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby,
        | commitdate, topics.stat1, postscore, topics.moderate, notop, topics.resolved, minor, draft, allow_anonymous,
        | topics.reactions
        |FROM topics
        | INNER JOIN groups ON (groups.id=topics.groupid)
        | INNER JOIN sections ON (sections.id=groups.section) """.stripMargin)

    if (topicListDto.isUserFavs) {
      query.append("INNER JOIN memories ON (memories.topic = topics.id) ")
    }

    query
      .append("WHERE ")
      .append(TopicListDao.makeConditions(topicListDto, params))
      .append(sort)
      .append(limit)

    namedJdbcTemplate.query(query.toString, params.asJava, (resultSet: ResultSet, _: Int) => Topic.fromResultSet(resultSet))
  }

  /**
   * Возвращает удаленные темы в премодерируемом разделе.
   *
   * Темы, удаленные автором пропускаются.
   *
   * @param sectionId     номер раздела или 0 для всех премодерируемых
   * @param skipBadReason Пропускать темы, удаленные с пустым комментарием и спам
   * @param includeAnonymous включать ли в выборку темы, созданные anonymous
   * @return список удаленных тем
   */
  def getDeletedTopics(sectionId: Int, skipBadReason: Boolean, includeAnonymous: Boolean): java.util.List[DeletedTopic] = {
    val query = new StringBuilder

    query
      .append("SELECT ")
      .append("topics.title as subj, nick, groups.section, topics.id as msgid, ")
      .append("reason, topics.postdate, del_info.delDate, bonus ")
      .append("FROM topics,groups,users,sections,del_info ")
      .append("WHERE sections.id=groups.section AND topics.userid=users.id ")
      .append("AND topics.groupid=groups.id AND sections.moderate AND deleted ")
      .append("AND del_info.msgid=topics.id AND topics.userid!=del_info.delby ")
      .append("AND delDate > CURRENT_TIMESTAMP - '2 weeks'::interval ")

    if (skipBadReason) {
      query.append("AND reason!='' AND reason!='Блокировка пользователя с удалением сообщений' AND reason!='4.6 Спам' ")
    }

    if (!includeAnonymous) {
      query.append("AND topics.userid != " + User.ANONYMOUS_ID + " ")
    }

    val queryParameters = mutable.ArrayBuffer[AnyRef]()

    if (sectionId != 0) {
      query.append(" AND section=? ")
      queryParameters += Integer.valueOf(sectionId)
    }

    query.append(" ORDER BY del_info.delDate DESC LIMIT 20")

    jdbcTemplate.query(query.toString, (rs: ResultSet, _: Int) => DeletedTopic.apply(rs), queryParameters.toSeq *)
  }

  def getDeletedUserTopics(user: User, topics: Int): java.util.List[DeletedTopic] = {
    val query =
      s"""SELECT topics.title as subj, nick, groups.section, topics.id as msgid, reason, topics.postdate,
         |  del_info.delDate, bonus FROM topics,groups,users,del_info
         | WHERE topics.userid=users.id AND topics.groupid=groups.id AND deleted AND del_info.msgid=topics.id
         | AND delDate is not null AND topics.userid = ${user.getId}
         | ORDER BY del_info.delDate DESC LIMIT $topics""".stripMargin

    jdbcTemplate.query(query, (rs: ResultSet, _: Int) => DeletedTopic.apply(rs))
  }
}