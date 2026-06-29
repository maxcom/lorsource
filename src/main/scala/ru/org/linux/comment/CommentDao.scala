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

package ru.org.linux.comment

import org.springframework.stereotype.Repository
import ru.org.linux.scalikejdbc.{SpringDB, Transaction}
import ru.org.linux.scalikejdbc.Transaction.given
import ru.org.linux.site.MessageNotFoundException
import ru.org.linux.user.User
import ru.org.linux.util.StringUtil
import scalikejdbc.*

import java.sql.Timestamp

/** Операции над комментариями
  */
@Repository
class CommentDao(springDB: SpringDB):

  /** Получить комментарий по id
    *
    * @param id
    *   id нужного комментария
    * @return
    *   нужный комментарий
    * @throws MessageNotFoundException
    *   при отсутствии сообщения
    */
  @throws[MessageNotFoundException]
  def getById(id: Int): Comment =
    springDB.run:
      sql"""SELECT postdate, topic, userid, comments.id as msgid, comments.title,
            deleted, replyto, edit_count, edit_date, editor_id,
            ua_id, comments.postip, comments.reactions
            FROM comments WHERE comments.id = $id"""
        .map(rs => Comment(rs.underlying))
        .single
        .apply()
        .getOrElse(throw new MessageNotFoundException(id))

  /** Список комментариев топика
    *
    * @param topicId
    *   id топика
    * @param showDeleted
    *   вместе с удаленными
    * @return
    *   список комментариев топика
    */
  def getCommentList(topicId: Int, showDeleted: Boolean): Seq[Comment] =
    if showDeleted then
      springDB.run:
        sql"""SELECT comments.title, topic, postdate, userid, comments.id as msgid,
              replyto, edit_count, edit_date, editor_id, deleted,
              ua_id, comments.postip, comments.reactions
              FROM comments WHERE topic = $topicId ORDER BY msgid ASC""".map(rs => Comment(rs.underlying)).list.apply()
    else
      springDB.run:
        sql"""SELECT comments.title, topic, postdate, userid, comments.id as msgid,
              replyto, edit_count, edit_date, editor_id, deleted,
              ua_id, comments.postip, comments.reactions
              FROM comments WHERE topic = $topicId AND NOT deleted ORDER BY msgid ASC"""
          .map(rs => Comment(rs.underlying))
          .list
          .apply()

  /** Удалить комментарий.
    *
    * @param msgid
    *   идентификационнай номер комментария
    * @return
    *   true если комментарий был удалён, иначе false
    */
  def deleteComment(msgid: Int)(using Transaction): Boolean =
    sql"UPDATE comments SET deleted='t' WHERE id = $msgid AND NOT deleted".update.apply() > 0

  def undeleteComment(comment: Comment)(using Transaction): Unit =
    sql"UPDATE comments SET deleted='f' WHERE id = ${comment.id}".update.apply()

  /** Обновляет статистику после удаления комментариев в одном топике.
    *
    * @param commentId
    *   идентификатор любого из удаленных комментариев (обычно корневой в цепочке)
    * @param count
    *   количество удаленных комментариев
    */
  def updateStatsAfterDelete(commentId: Int, count: Int)(using Transaction): Unit =
    val topicId = sql"SELECT topic FROM comments WHERE id = $commentId".map(rs => rs.int("topic")).single.apply().get
    sql"UPDATE topics SET stat1=stat1-$count, lastmod=CURRENT_TIMESTAMP WHERE id = $topicId".update.apply()
    sql"UPDATE topics SET stat3=stat1 WHERE id = $topicId AND stat3 > stat1".update.apply()

  /** Сколько ответов на комментарий
    *
    * @param msgid
    *   id комментария
    * @return
    *   число ответов на комментарий
    */
  def getRepliesCount(msgid: Int): Int =
    springDB.run:
      sql"SELECT count(id) FROM comments WHERE replyto = $msgid AND NOT deleted"
        .map(rs => rs.int(1))
        .single
        .apply()
        .getOrElse(0)

  /** Массовое удаление комментариев пользователя со всеми ответами на комментарии.
    *
    * @param user
    *   пользователь для экзекуции
    * @return
    *   список удаленных комментариев
    */
  def getAllByUserForUpdate(user: User)(using Transaction): Seq[Int] =
    sql"SELECT id FROM comments WHERE userid = ${user.id} AND NOT deleted ORDER BY id DESC FOR UPDATE"
      .map(rs => rs.int("id"))
      .list
      .apply()

  def getCommentsByIPAddressForUpdate(ip: String, timedelta: Timestamp)(using Transaction): Seq[Int] =
    sql"SELECT id FROM comments WHERE postip = ${ip}::inet AND NOT deleted AND postdate > $timedelta ORDER BY id DESC FOR UPDATE"
      .map(rs => rs.int("id"))
      .list
      .apply()

  /** Добавить новый комментарий.
    *
    * @return
    *   идентификационный номер нового комментария
    */
  def saveNewMessage(comment: Comment, userAgent: Option[String])(using Transaction): Int =
    val msgid = sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
    val truncatedUserAgent = userAgent.map(ua => ua.substring(0, Math.min(511, ua.length)))
    val replyToOpt = Option.when(comment.replyTo != 0)(comment.replyTo)
    sql"""INSERT INTO comments (id, userid, title, postdate, replyto, deleted, topic, postip, ua_id)
          VALUES ($msgid, ${comment.userid}, ${comment.title}, CURRENT_TIMESTAMP,
                  $replyToOpt, 'f', ${comment.topicId}, ${comment.postIP}::inet,
                  create_user_agent($truncatedUserAgent))""".update.apply()
    msgid

  /** Редактирование комментария.
    *
    * @param oldComment
    *   данные старого комментария
    * @param title
    *   новый заголовок
    */
  def changeTitle(oldComment: Comment, title: String)(using Transaction): Unit =
    sql"UPDATE comments SET title = $title WHERE id = ${oldComment.id}".update.apply()

  /** Обновить информацию о последнем редакторе комментария.
    *
    * @param id
    *   идентификационный номер комментария
    * @param editorId
    *   идентификационный номер редактора комментария
    * @param editDate
    *   дата редактирования
    * @param editCount
    *   количество исправлений
    */
  def updateLatestEditorInfo(id: Int, editorId: Int, editDate: Timestamp, editCount: Int)(using Transaction): Unit =
    sql"""UPDATE comments SET editor_id = $editorId, edit_date = $editDate, edit_count = $editCount WHERE id = $id"""
      .update
      .apply()

  /** Получить список последних удалённых комментариев пользователя.
    *
    * @param userId
    *   идентификационный номер пользователя
    * @return
    *   список удалённых комментариев пользователя
    */
  def getDeletedComments(userId: Int, filter: DeletedCommentsFilterEnum, offset: Int): Seq[CommentsListItem] =
    val filterClause: SQLSyntax = filter match
      case DeletedCommentsFilterEnum.ALL => sqls"true"
      case DeletedCommentsFilterEnum.PENALTY => sqls"comdel.bonus IS NOT NULL AND comdel.bonus != 0"
      case DeletedCommentsFilterEnum.NO_AUTO => sqls"comments.deleted AND comdel.reason IS NOT NULL AND comdel.reason NOT ILIKE '%(авто%'"
      case DeletedCommentsFilterEnum.SELF_DELETED => sqls"comdel.delby = comments.userid"

    springDB.run:
      sql"""SELECT groups.title as gtitle, topics.title, topics.id as msgid,
            comdel.reason, COALESCE(comdel.delDate, topdel.delDate) deldate, comdel.bonus,
            comments.id as cid, comments.postdate, topics.deleted topic_deleted, comments.deleted comment_deleted
            FROM groups JOIN topics ON groups.id=topics.groupid
            JOIN comments ON comments.topic=topics.id
            LEFT JOIN del_info comdel ON comdel.msgid=comments.id
            LEFT JOIN del_info topdel ON topdel.msgid=topics.id
            WHERE comments.userid=$userId AND (comments.deleted OR topics.deleted) AND ${filterClause}
            ORDER BY COALESCE(comdel.delDate, topdel.delDate) DESC NULLS LAST, comments.id DESC
            LIMIT 50 OFFSET ${offset}"""
        .map(rs =>
          CommentsListItem(
            gtitle = rs.string("gtitle"),
            msgid = rs.int("msgid"),
            title = StringUtil.makeTitle(rs.string("title")),
            reason = rs.string("reason"),
            delDate = rs.timestamp("deldate"),
            bonus = rs.intOpt("bonus").getOrElse(0),
            commentId = rs.int("cid"),
            deleted = rs.boolean("comment_deleted"),
            postdate = rs.timestamp("postdate"),
            authorId = userId,
            topicDeleted = rs.boolean("topic_deleted")
          ))
        .list
        .apply()
