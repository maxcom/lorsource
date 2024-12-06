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
import ru.org.linux.auth.AuthorizedSession
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
   * @param reason  причина удаления
   * @param scoreBonus   дельта изменения score автора топика
   */
  def deleteTopic(topic: Topic, reason: String, scoreBonus: Int)
                 (implicit currentUser: AuthorizedSession): Unit = transactional() { _ =>
    assert(scoreBonus <= 20 && scoreBonus >= 0, "Некорректное значение bonus")
    assert(scoreBonus == 0 || currentUser.moderator, "Только модератор может менять score")

    doDeleteTopic(topic, currentUser.user, reason, -scoreBonus).foreach { info =>
      deleteInfoDao.insert(info)

      userEventService.processTopicDeleted(Seq(topic.id))
      userEventService.insertTopicDeleteNotification(topic, info)
    }
  }

  /**
   * Удаляет один комментарий
   *
   * @param comment       удаляемый комментарий
   * @param reason        причина удаления
   * @param scoreBonus    сколько шкворца снять
   * @param checkForReply производить ли проверку на ответы
   * @throws ScriptErrorException генерируем исключение если на комментарий есть ответы
   */
  def deleteComment(comment: Comment, reason: String, scoreBonus: Int, checkForReply: Boolean)
                   (implicit currentUser: AuthorizedSession): Boolean = transactional() { _ =>
    assert(scoreBonus == 0 || currentUser.moderator, "Только модератор может менять score")
    assert(checkForReply || currentUser.moderator, "Только модератор может удалять без ответов")

    if (checkForReply && commentDao.getRepliesCount(comment.id) != 0) {
      throw new ScriptErrorException("Нельзя удалить комментарий с ответами")
    }

    val deleted = doDeleteCommentWithScore(comment, -scoreBonus, reason, deleteBy = currentUser.user)

    deleted.foreach { info =>
      deleteInfoDao.insert(info)

      commentDao.updateStatsAfterDelete(comment.id, 1)
      userEventService.processCommentsDeleted(Seq(comment.id))
      userEventService.insertCommentDeleteNotification(comment, info)
    }

    deleted.isDefined
  }

  /**
   * Удаление комментария с ответами.
   *
   * @param comment    удаляемый комментарий
   * @param scoreBonus сколько снять скора у автора комментария
   * @return список идентификационных номеров удалённых комментариев
   */
  def deleteCommentWithReplys(topic: Topic, comment: Comment, reason: String, scoreBonus: Int)
                             (implicit currentUser: AuthorizedSession): Seq[Int] = transactional() { _ =>
    assert(currentUser.moderator, "Только модератор может удалять с ответами")

    val commentList = commentService.getCommentList(topic, showDeleted = false)

    val node = commentList.getNode(comment.id)

    val replys = DeleteService.getAllReplys(node, 0)

    deleteReplys(comment, reason, replys, currentUser.user, -scoreBonus, notifyReplys = !topic.expired)
  }

  /**
   * Удаление топиков, сообщений по ip и за определенный период времени, те комментарии на которые существуют ответы пропускаем
   *
   * @param ip        ip для которых удаляем сообщения (не проверяется на корректность)
   * @param timeDelta врменной промежуток удаления (не проверяется на корректность)
   * @param reason    причина удаления, которая будет вписана для удаляемых топиков
   * @return список id удаленных сообщений
   */
  def deleteByIPAddress(ip: String, timeDelta: Timestamp, reason: String)
                       (implicit currentUser: AuthorizedSession): DeleteCommentResult = transactional() { _ =>
    assert(currentUser.moderator, "Только модератор может выполнять массовое удаление")

    val topics = topicDao.getAllByIPForUpdate(ip, timeDelta).asScala.map(_.toInt)
    val comments = commentDao.getCommentsByIPAddressForUpdate(ip, timeDelta).asScala.map(_.toInt)

    massDelete(currentUser.user, topics, comments, reason)
  }

  /**
   * Блокировка и массивное удаление всех топиков и комментариев пользователя со всеми ответами на комментарии
   *
   * @param user      пользователь для экзекуции
   * @param moderator экзекутор-модератор
   * @param reason    прична блокировки
   * @return список удаленных комментариев
   */
  def deleteAllAndBlock(user: User,reason: String)
                       (implicit currentUser: AuthorizedSession): DeleteCommentResult = transactional() { _ =>
    assert(currentUser.moderator, "Только модератор может выполнять массовое удаление")

    userDao.block(user, currentUser.user, reason)

    val topics = topicDao.getUserTopicForUpdate(user).asScala.map(_.toInt)
    val comments = commentDao.getAllByUserForUpdate(user).asScala.map(_.toInt)

    massDelete(currentUser.user, topics, comments, "Блокировка пользователя с удалением сообщений", notifyUser = false)
  }

  def undeleteComment(comment: Comment)
                     (implicit currentUser: AuthorizedSession): Unit = transactional() { _ =>
    assert(currentUser.moderator, "Только модератор может восстанавливать")

    val deleteInfo = deleteInfoDao.getDeleteInfo(comment.id, true)

    if (deleteInfo != null && deleteInfo.getBonus != 0) {
      userDao.changeScore(comment.userid, -deleteInfo.getBonus)
    }

    commentDao.undeleteComment(comment)
    deleteInfoDao.delete(comment.id)
    topicDao.updateLastmod(comment.topicId, false)
  }

  def undeleteTopic(topic: Topic)
                   (implicit currentUser: AuthorizedSession): Unit = transactional() { _ =>
    assert(currentUser.moderator, "Только модератор может восстанавливать")

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
   * @param commentId    удаляемый комментарий
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
                           user: User, rootBonus: Int, notifyReplys: Boolean): Seq[Int] = {
    val score = rootBonus < -2

    val deleteInfos = new ArrayBuffer[InsertDeleteInfo](initialSize = replys.size)

    for (cur <- replys) {
      val child = cur.comment
      val (bonus, reason) = DeleteReasons.replyBonusAndReason(score, cur.depth)

      val del = doDeleteCommentWithScore(child, bonus, reason, user)

      del.foreach { info =>
        deleteInfos.addOne(info)

        if (notifyReplys) {
          userEventService.insertCommentDeleteNotification(child, info)
        }
      }
    }

    val deletedMain = doDeleteCommentWithScore(root, rootBonus, rootReason, user)

    deletedMain.foreach { info =>
      deleteInfos.addOne(info)
      userEventService.insertCommentDeleteNotification(root, info)
    }

    if (deleteInfos.nonEmpty) {
      deleteInfoDao.insert(deleteInfos.asJava)

      commentDao.updateStatsAfterDelete(root.id, deleteInfos.size)
    }

    val deletedComments = deleteInfos.map(_.msgid).toVector

    userEventService.processCommentsDeleted(deletedComments)

    deletedComments
  }

  private def doDeleteTopic(topic: Topic, moderator: User, reason: String, scoreBonus: Int): Option[InsertDeleteInfo] = {
    assert(scoreBonus <= 0, s"Score bonus '$scoreBonus' on delete must be non-positive")

    val deleted = topicDao.delete(topic.id)

    if (deleted) {
      if (scoreBonus !=0) {
        userDao.changeScore(topic.authorUserId, scoreBonus)
      }

      Some(new InsertDeleteInfo(topic.id, reason, scoreBonus, moderator))
    } else {
      None
    }
  }

  private def massDelete(moderator: User, topics: collection.Seq[Int], comments: collection.Seq[Int],
                         reason: String, notifyUser: Boolean = true): DeleteCommentResult = {
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

    val deletedCommentIds = deletedComments.map(_.msgid).toVector

    userEventService.processCommentsDeleted(deletedCommentIds)

    // common
    deleteInfoDao.insert((deletedComments ++ deletedTopics).asJava)

    if (notifyUser) {
      userEventService.insertTopicMassDeleteNotifications(deletedTopics.map(_.msgid), reason, moderator)
      userEventService.insertCommentMassDeleteNotifications(deletedCommentIds, reason, moderator)
    }

    new DeleteCommentResult(
      deletedTopics.view.map(_.msgid).map(Integer.valueOf).toVector.asJava,
      deletedCommentIds.map(Integer.valueOf).asJava,
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