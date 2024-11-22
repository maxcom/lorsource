/*
 * Copyright 1998-2024 Linux.org.ru
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

import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import ru.org.linux.site.ScriptErrorException
import ru.org.linux.spring.dao.DeleteInfoDao
import ru.org.linux.spring.dao.DeleteInfoDao.InsertDeleteInfo
import ru.org.linux.topic.Topic
import ru.org.linux.topic.TopicDao
import ru.org.linux.topic.TopicService
import ru.org.linux.user.User
import ru.org.linux.user.UserDao
import ru.org.linux.user.UserEventService

import java.sql.Timestamp
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}

object CommentDeleteService {
  private def getAllReplys(node: CommentNode, depth: Int): Seq[CommentAndDepth] = {
    node.childs.asScala.view.flatMap { r =>
      getAllReplys(r, depth + 1) :+ CommentAndDepth(r.getComment, depth)
    }.toVector
  }

  private case class CommentAndDepth(comment: Comment, depth: Int) {
    def deleteInfo(score: Boolean, user: User): InsertDeleteInfo = {
      val (bonus, reason) = if (score) depth match {
        case 0 =>
          (-2, "7.1 Ответ на некорректное сообщение (авто, уровень 0)")
        case 1 =>
          (-1, "7.1 Ответ на некорректное сообщение (авто, уровень 1)")
        case _ =>
          (0, "7.1 Ответ на некорректное сообщение (авто, уровень >1)")
      } else {
        (0, "7.1 Ответ на некорректное сообщение (авто)")
      }

      new InsertDeleteInfo(comment.id, reason, bonus, user.getId)
    }
  }
}

@Service
class CommentDeleteService(commentDao: CommentDao, topicService: TopicService, userDao: UserDao,
                           userEventService: UserEventService, deleteInfoDao: DeleteInfoDao,
                           commentService: CommentReadService, topicDao: TopicDao,
                           val transactionManager: PlatformTransactionManager) extends TransactionManagement {
  /**
   * Удаляет один комментарий
   *
   * @param comment       удаляемый комментарий
   * @param reason        причина удаления
   * @param user          модератор который удаляет
   * @param scoreBonus    сколько шкворца снять
   * @param checkForReply производить ли проверку на ответы
   * @throws ScriptErrorException генерируем исключение если на комментарий есть ответы
   */
  def deleteComment(comment: Comment, reason: String, user: User, scoreBonus: Int, checkForReply: Boolean): Boolean = transactional() { _ =>
    if (checkForReply && commentDao.getRepliesCount(comment.id) != 0) {
      throw new ScriptErrorException("Нельзя удалить комментарий с ответами")
    }

    val deleted = deleteAndChangeScore(comment, -scoreBonus)

    if (deleted) {
      deleteInfoDao.insert(comment.id, user, reason, -scoreBonus)

      if (scoreBonus != 0) {
        userDao.changeScore(comment.userid, -scoreBonus)
      }

      commentDao.updateStatsAfterDelete(comment.id, 1)
      userEventService.processCommentsDeleted(Seq(comment.id))
    }

    deleted
  }

  /**
   * Удалить комментарий и сменить score автору, без записи del_info.
   *
   * @param comment    удаляемый комментарий
   * @param scoreBonus сколько снять скора у автора комментария
   * @return true если комментарий был удалён, иначе false
   */
  private def deleteAndChangeScore(comment: Comment, scoreBonus: Int) = {
    assert(scoreBonus <= 0, s"Score bonus '$scoreBonus' on delete must be non-positive")

    val deleted = commentDao.deleteComment(comment.id)

    if (deleted && scoreBonus != 0) {
      userDao.changeScore(comment.userid, scoreBonus)
    }

    deleted
  }

  /**
   * Удаление комментария с ответами.
   *
   * @param comment    удаляемый комментарий
   * @param user       пользователь, удаляющий комментарий
   * @param scoreBonus сколько снять скора у автора комментария
   * @return список идентификационных номеров удалённых комментариев
   */
  def deleteWithReplys(topic: Topic, comment: Comment, reason: String, user: User, scoreBonus: Int): Seq[Int] = transactional() { _ =>
    val commentList = commentService.getCommentList(topic, showDeleted = false)

    val node = commentList.getNode(comment.id)

    val replys = CommentDeleteService.getAllReplys(node, 0)

    val deleted = deleteReplys(comment, reason, replys, user, -scoreBonus)

    userEventService.processCommentsDeleted(deleted)

    deleted
  }

  /**
   * Удалить рекурсивно ответы на комментарий
   *
   * @param replys    список ответов
   * @param user      пользователь, удаляющий комментарий
   * @param rootBonus сколько снять скора у автора корневого комментария
   * @return список идентификационных номеров удалённых комментариев
   */
  private def deleteReplys(root: Comment, rootReason: String, replys: Seq[CommentDeleteService.CommentAndDepth],
                           user: User, rootBonus: Int): Seq[Int] = {
    val score = rootBonus < -2

    val deleted = new ArrayBuffer[Int](initialSize = replys.size)
    val deleteInfos = new ArrayBuffer[InsertDeleteInfo](initialSize = replys.size)

    for (cur <- replys) {
      val child = cur.comment
      val info = cur.deleteInfo(score, user)

      val del = deleteAndChangeScore(child, info.getBonus)

      if (del) {
        deleteInfos.addOne(info)
        deleted.addOne(child.id)
      }
    }

    val deletedMain = deleteAndChangeScore(root, rootBonus)

    if (deletedMain) {
      deleteInfos.addOne(new DeleteInfoDao.InsertDeleteInfo(root.id, rootReason, rootBonus, user.getId))
      deleted.addOne(root.id)
    }

    deleteInfoDao.insert(deleteInfos.asJava)

    if (deleted.nonEmpty) {
      commentDao.updateStatsAfterDelete(root.id, deleted.size)
    }

    deleted.toVector
  }

  /**
   * Удаление топиков, сообщений по ip и за определнный период времени, те комментарии на которые существуют ответы пропускаем
   *
   * @param ip        ip для которых удаляем сообщения (не проверяется на корректность)
   * @param timeDelta врменной промежуток удаления (не проверяется на корректность)
   * @param moderator экзекутор-можератор
   * @param reason    причина удаления, которая будет вписана для удаляемых топиков
   * @return список id удаленных сообщений
   */
  def deleteCommentsByIPAddress(ip: String, timeDelta: Timestamp, moderator: User, reason: String): DeleteCommentResult = transactional() { _ =>
    val deletedTopics = topicService.deleteByIPAddress(ip, timeDelta, moderator, reason)

    val skippedComments = new ArrayBuffer[Int]

    // Удаляем комментарии если на них нет ответа
    val commentIds = commentDao.getCommentsByIPAddressForUpdate(ip, timeDelta).asScala
    val deleteInfos = new ArrayBuffer[InsertDeleteInfo]

    for (msgid <- commentIds) {
      if (commentDao.getRepliesCount(msgid) == 0) {
        if (commentDao.deleteComment(msgid)) {
          deleteInfos.addOne(new InsertDeleteInfo(msgid, reason, 0, moderator.getId))
        } else {
          skippedComments.addOne(msgid)
        }
      }
    }

    for (info <- deleteInfos) {
      commentDao.updateStatsAfterDelete(info.getMsgid, 1)
    }

    deleteInfoDao.insert(deleteInfos.asJava)
    userEventService.processCommentsDeleted(deleteInfos.view.map(_.getMsgid).toVector)

    new DeleteCommentResult(
      deletedTopics.map(Integer.valueOf).asJava,
      deleteInfos.view.map(_.getMsgid).map(Integer.valueOf).toVector.asJava,
      skippedComments.map(Integer.valueOf).asJava)
  }

  /**
   * Блокировка и массивное удаление всех топиков и комментариев пользователя со всеми ответами на комментарии
   *
   * @param user      пользователь для экзекуции
   * @param moderator экзекутор-модератор
   * @param reason    прична блокировки
   * @return список удаленных комментариев
   */
  def deleteAllCommentsAndBlock(user: User, moderator: User, reason: String): DeleteCommentResult = transactional() { _ =>
    userDao.block(user, moderator, reason)

    val deletedTopics = topicService.deleteAllByUser(user, moderator)

    val deleteInfos = new ArrayBuffer[InsertDeleteInfo]
    // Удаляем все комментарии
    val commentIds = commentDao.getAllByUserForUpdate(user).asScala

    val skippedComments = new ArrayBuffer[Int]

    for (msgid <- commentIds) {
      if (commentDao.getRepliesCount(msgid) == 0) {
        val deleted = commentDao.deleteComment(msgid)

        if (deleted) {
          commentDao.updateStatsAfterDelete(msgid, 1)
          deleteInfos.addOne(new InsertDeleteInfo(msgid, "Блокировка пользователя с удалением сообщений", 0, moderator.getId))
        }
      } else {
        skippedComments.addOne(msgid)
      }
    }

    deleteInfoDao.insert(deleteInfos.asJava)
    userEventService.processCommentsDeleted(deleteInfos.view.map(_.getMsgid).toVector)

    new DeleteCommentResult(
      deletedTopics.map(Integer.valueOf).asJava,
      deleteInfos.view.map(_.getMsgid).map(Integer.valueOf).toVector.asJava,
      skippedComments.map(Integer.valueOf).asJava)
  }

  def undeleteComment(comment: Comment): Unit = transactional() { _ =>
    val deleteInfo = deleteInfoDao.getDeleteInfo(comment.id, true)

    if (deleteInfo != null && deleteInfo.getBonus != 0) {
      userDao.changeScore(comment.userid, -deleteInfo.getBonus)
    }

    commentDao.undeleteComment(comment)
    deleteInfoDao.delete(comment.id)
    topicDao.updateLastmod(comment.topicId, false)
  }
}