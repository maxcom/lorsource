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
package ru.org.linux.comment

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.springframework.stereotype.Service
import ru.org.linux.site.MessageNotFoundException
import ru.org.linux.topic.Topic
import ru.org.linux.user.User
import ru.org.linux.user.UserDao
import ru.org.linux.user.UserNotFoundException

import java.util
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
      new CommentList(commentDao.getCommentList(topic.id, true), topic.lastModified.toInstant)
    } else {
      val commentList = cache.getIfPresent(topic.id)

      if (commentList == null || commentList.getLastmod.isBefore(topic.lastModified.toInstant)) {
        val newList = new CommentList(commentDao.getCommentList(topic.id, false), topic.lastModified.toInstant)
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

  @throws[UserNotFoundException]
  def makeHideSet(comments: CommentList, filterChain: Int, ignoreList: util.Set[Integer]): util.Set[Integer] = {
    if (filterChain == CommentFilter.FILTER_NONE) {
      Set.empty[Integer].asJava
    } else {
      val hideSet = new util.HashSet[Integer]

      /* hide anonymous */
      if ((filterChain & CommentFilter.FILTER_ANONYMOUS) > 0) {
        comments.getRoot.hideAnonymous(userDao, hideSet)
      }

      /* hide ignored */
      if ((filterChain & CommentFilter.FILTER_IGNORED) > 0) {
        if (!ignoreList.isEmpty) comments.getRoot.hideIgnored(hideSet, ignoreList)
      }

      hideSet
    }
  }
}