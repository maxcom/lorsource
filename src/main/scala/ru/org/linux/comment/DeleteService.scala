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
import ru.org.linux.common.DeleteReasons
import ru.org.linux.site.ScriptErrorException
import ru.org.linux.spring.dao.DeleteInfoDao
import ru.org.linux.spring.dao.DeleteInfoDao.InsertDeleteInfo
import ru.org.linux.topic.{Topic, TopicDao}
import ru.org.linux.user.{User, UserDao, UserEventService}

import java.sql.Timestamp
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}

@Service
class DeleteService(commentDao: CommentDao, userDao: UserDao, userEventService: UserEventService,
                    deleteInfoDao: DeleteInfoDao, commentService: CommentReadService, topicDao: TopicDao,
                    val transactionManager: PlatformTransactionManager) extends TransactionManagement {
  /**
   * Удаление топика и если удаляет модератор изменить автору score
   *
   * @param topic удаляемый топик
   * @param user    удаляющий пользователь
   * @param reason  причина удаления
   * @param bonus   дельта изменения score автора топика
   */
  def deleteTopic(topic: Topic, user: User, reason: String, bonus: Int): Unit = transactional() { _ =>
    assert(bonus <= 20 && bonus >= 0, "Некорректное значение bonus")

    doDeleteTopic(topic, user, reason, -bonus).foreach { info =>
      deleteInfoDao.insert(info)

      userEventService.processTopicDeleted(Seq(topic.id))
    }
  }

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
  def deleteComment(comment: Comment, reason: String, user: User, scoreBonus: Int,
                    checkForReply: Boolean): Boolean = transactional() { _ =>
    if (checkForReply && commentDao.getRepliesCount(comment.id) != 0) {
      throw new ScriptErrorException("Нельзя удалить комментарий с ответами")
    }

    val deleted = doDeleteCommentWithScore(comment, -scoreBonus, reason, deleteBy = user)

    deleted.foreach { info =>
      deleteInfoDao.insert(info)

      commentDao.updateStatsAfterDelete(comment.id, 1)
      userEventService.processCommentsDeleted(Seq(comment.id))
    }

    deleted.isDefined
  }

  /**
   * Удаление комментария с ответами.
   *
   * @param comment    удаляемый комментарий
   * @param user       пользователь, удаляющий комментарий
   * @param scoreBonus сколько снять скора у автора комментария
   * @return список идентификационных номеров удалённых комментариев
   */
  def deleteCommentWithReplys(topic: Topic, comment: Comment, reason: String, user: User, scoreBonus: Int): Seq[Int] = transactional() { _ =>
    val commentList = commentService.getCommentList(topic, showDeleted = false)

    val node = commentList.getNode(comment.id)

    val replys = DeleteService.getAllReplys(node, 0)

    val deleted = deleteReplys(comment, reason, replys, user, -scoreBonus)

    userEventService.processCommentsDeleted(deleted)

    deleted
  }

  /**
   * Удаление топиков, сообщений по ip и за определенный период времени, те комментарии на которые существуют ответы пропускаем
   *
   * @param ip        ip для которых удаляем сообщения (не проверяется на корректность)
   * @param timeDelta врменной промежуток удаления (не проверяется на корректность)
   * @param moderator экзекутор-можератор
   * @param reason    причина удаления, которая будет вписана для удаляемых топиков
   * @return список id удаленных сообщений
   */
  def deleteByIPAddress(ip: String, timeDelta: Timestamp, moderator: User, reason: String): DeleteCommentResult = transactional() { _ =>
    val topics = topicDao.getAllByIPForUpdate(ip, timeDelta).asScala.map(_.toInt)
    val comments = commentDao.getCommentsByIPAddressForUpdate(ip, timeDelta).asScala.map(_.toInt)

    massDelete(moderator, topics, comments, reason)
  }

  /**
   * Блокировка и массивное удаление всех топиков и комментариев пользователя со всеми ответами на комментарии
   *
   * @param user      пользователь для экзекуции
   * @param moderator экзекутор-модератор
   * @param reason    прична блокировки
   * @return список удаленных комментариев
   */
  def deleteAllAndBlock(user: User, moderator: User, reason: String): DeleteCommentResult = transactional() { _ =>
    userDao.block(user, moderator, reason)

    val topics = topicDao.getUserTopicForUpdate(user).asScala.map(_.toInt)
    val comments = commentDao.getAllByUserForUpdate(user).asScala.map(_.toInt)

    massDelete(moderator, topics, comments, "Блокировка пользователя с удалением сообщений")
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

  def undeleteTopic(topic: Topic): Unit = transactional() { _ =>
    val deleteInfo = deleteInfoDao.getDeleteInfo(topic.id, true)

    if (deleteInfo != null && deleteInfo.getBonus != 0) {
      userDao.changeScore(topic.authorUserId, -deleteInfo.getBonus)
    }

    topicDao.undelete(topic)
    deleteInfoDao.delete(topic.id)
  }

  /**
   * Удалить комментарий и сменить score автору.
   *
   * @param comment    удаляемый комментарий
   * @param scoreBonus сколько снять скора у автора комментария
   * @return DeleteInfo, rоторый потом надо вставить в БД
   */
  private def doDeleteCommentWithScore(comment: Comment, scoreBonus: Int, reason: String,
                                       deleteBy: User): Option[InsertDeleteInfo] = {
    assert(scoreBonus <= 0, s"Score bonus '$scoreBonus' on delete must be non-positive")

    val deleted = commentDao.deleteComment(comment.id)

    if (deleted && scoreBonus != 0) {
      userDao.changeScore(comment.userid, scoreBonus)
    }

    if (deleted) {
      Some(new InsertDeleteInfo(comment.id, reason, scoreBonus, deleteBy))
    } else {
      None
    }
  }

  /**
   * Удалить комментарий и сменить score автору.
   *
   * @param comment    удаляемый комментарий
   * @param scoreBonus сколько снять скора у автора комментария
   * @return DeleteInfo, rоторый потом надо вставить в БД
   */
  private def doDeleteComment(commentId: Int, reason: String, deleteBy: User): Option[InsertDeleteInfo] = {
    val deleted = commentDao.deleteComment(commentId)

    if (deleted) {
      Some(new InsertDeleteInfo(commentId, reason, 0, deleteBy))
    } else {
      None
    }
  }

  /**
   * Удалить рекурсивно ответы на комментарий
   *
   * @param replys    список ответов
   * @param user      пользователь, удаляющий комментарий
   * @param rootBonus сколько снять скора у автора корневого комментария
   * @return список идентификационных номеров удалённых комментариев
   */
  private def deleteReplys(root: Comment, rootReason: String, replys: Seq[DeleteService.CommentAndDepth],
                           user: User, rootBonus: Int): Seq[Int] = {
    val score = rootBonus < -2

    val deleteInfos = new ArrayBuffer[InsertDeleteInfo](initialSize = replys.size)

    for (cur <- replys) {
      val child = cur.comment
      val (bonus, reason) = DeleteReasons.replyBonusAndReason(score, cur.depth)

      val del = doDeleteCommentWithScore(child, bonus, reason, user)

      del.foreach { info =>
        deleteInfos.addOne(info)
      }
    }

    val deletedMain = doDeleteCommentWithScore(root, rootBonus, rootReason, user)

    deletedMain.foreach { info =>
      deleteInfos.addOne(info)
    }

    if (deleteInfos.nonEmpty) {
      deleteInfoDao.insert(deleteInfos.asJava)

      commentDao.updateStatsAfterDelete(root.id, deleteInfos.size)
    }

    deleteInfos.map(_.msgid).toVector
  }

  private def doDeleteTopic(topic: Topic, moderator: User, reason: String, bonus: Int): Option[InsertDeleteInfo] = {
    val deleted = topicDao.delete(topic.id)

    if (deleted) {
      if (bonus !=0) {
        userDao.changeScore(topic.authorUserId, -bonus)
      }

      Some(new InsertDeleteInfo(topic.id, reason, bonus, moderator))
    } else {
      None
    }
  }

  private def massDelete(moderator: User, topics: collection.Seq[Int], comments: collection.Seq[Int],
                         reason: String) = {
    // deleteTopics
    val deletedTopicsBuilder = Vector.newBuilder[InsertDeleteInfo]

    for (mid <- topics) {
      val deleted = topicDao.delete(mid)

      if (deleted) {
        val info = new InsertDeleteInfo(mid, reason, 0, moderator)
        deletedTopicsBuilder.addOne(info)
      }
    }

    val deletedTopics = deletedTopicsBuilder.result()

    userEventService.processTopicDeleted(deletedTopics.map(_.msgid))

    // delete comments
    val skippedComments = new ArrayBuffer[Int]
    val deletedComments = new ArrayBuffer[InsertDeleteInfo]

    for (msgid <- comments) {
      if (commentDao.getRepliesCount(msgid) == 0) {
        val deleted = doDeleteComment(msgid, reason, moderator)

        deleted.foreach { info =>
          commentDao.updateStatsAfterDelete(msgid, 1)
          deletedComments.addOne(info)
        }
      } else {
        skippedComments.addOne(msgid)
      }
    }

    for (info <- deletedComments) {
      commentDao.updateStatsAfterDelete(info.msgid, 1)
    }

    userEventService.processCommentsDeleted(deletedComments.view.map(_.msgid).toVector)

    // common
    deleteInfoDao.insert((deletedComments ++ deletedTopics).asJava)

    new DeleteCommentResult(
      deletedTopics.view.map(_.msgid).map(Integer.valueOf).toVector.asJava,
      deletedComments.view.map(_.msgid).map(Integer.valueOf).toVector.asJava,
      skippedComments.map(Integer.valueOf).asJava)
  }
}

object DeleteService {
  private def getAllReplys(node: CommentNode, depth: Int): Seq[CommentAndDepth] = {
    node.childs.asScala.view.flatMap { r =>
      getAllReplys(r, depth + 1) :+ CommentAndDepth(r.getComment, depth)
    }.toVector
  }

  private case class CommentAndDepth(comment: Comment, depth: Int)
}