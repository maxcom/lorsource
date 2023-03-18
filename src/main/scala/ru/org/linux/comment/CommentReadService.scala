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
package ru.org.linux.comment

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.springframework.stereotype.Service
import ru.org.linux.site.MessageNotFoundException
import ru.org.linux.topic.Topic
import ru.org.linux.user.User
import ru.org.linux.user.UserDao

import java.util
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.*

@Service
class CommentReadService(commentDao: CommentDao, userDao: UserDao) {
  private val cache: Cache[Int, CommentList] =
    CacheBuilder.newBuilder.maximumSize(10000).build[Int, CommentList]

  /**
   * Получить объект комментария по идентификационному номеру
   *
   * @param id идентификационный номер комментария
   * @return объект комментария
   * @throws MessageNotFoundException если комментарий не найден
   */
  @throws[MessageNotFoundException]
  def getById(id: Int): Comment = commentDao.getById(id)

  /**
   * Проверка, имеет ли указанный комментарий ответы.
   *
   * @param comment объект комментария
   * @return true если есть ответы, иначе false
   */
  def hasAnswers(comment: Comment): Boolean = commentDao.getRepliesCount(comment.id) > 0

  /**
   * Список комментариев топика.
   *
   * @param topic       топик
   * @param showDeleted вместе с удаленными
   * @return список комментариев топика
   */
  def getCommentList(topic: Topic, showDeleted: Boolean): CommentList = {
    if (showDeleted) {
      new CommentList(commentDao.getCommentList(topic.id, true).asScala.toVector, topic.lastModified.toInstant)
    } else {
      val commentList = cache.getIfPresent(topic.id)

      if (commentList == null || commentList.lastmod.isBefore(topic.lastModified.toInstant)) {
        val newList = new CommentList(commentDao.getCommentList(topic.id, false).asScala.toVector, topic.lastModified.toInstant)
        cache.put(topic.id, newList)
        newList
      } else {
        commentList
      }
    }
  }

  /**
   * Получить список последних удалённых комментариев пользователя.
   *
   * @param user объект пользователя
   * @return список удалённых комментариев пользователя
   */
  def getDeletedComments(user: User): util.List[CommentsListItem] = commentDao.getDeletedComments(user.getId)

  def makeHideSet(comments: CommentList, filterChain: Int, ignoreList: Set[Int]): Set[Int] = {
    if (filterChain == CommentFilter.FILTER_NONE) {
      Set.empty[Int]
    } else {
      val hideSet = new util.HashSet[Integer]

      /* hide anonymous */
      if ((filterChain & CommentFilter.FILTER_ANONYMOUS) > 0) {
        comments.root.hideAnonymous(userDao, hideSet)
      }

      /* hide ignored */
      if ((filterChain & CommentFilter.FILTER_IGNORED) > 0 && ignoreList.nonEmpty) {
        comments.root.hideIgnored(hideSet, ignoreList.map(Integer.valueOf).asJava)
      }

      hideSet.asScala.view.map(_.toInt).toSet
    }
  }

  def getCommentsSubtree(comments: CommentList, parentId: Int, hideSet: Set[Int]): Seq[Comment] = {
    val parentNode = comments.getNode(parentId)

    val childList = new ArrayBuffer[Comment]()

    parentNode.foreach((comment: Comment) => {
      if (!hideSet.contains(comment.id)) {
        childList += comment
      }

    })

    childList.toSeq
  }
}