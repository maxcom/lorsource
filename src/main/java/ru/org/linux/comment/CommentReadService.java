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
package ru.org.linux.comment;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Service;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.topic.Topic;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserNotFoundException;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CommentReadService {
  private final CommentDao commentDao;
  private final UserDao userDao;

  private final Cache<Integer, CommentList> cache =
          CacheBuilder.newBuilder()
                  .maximumSize(10000)
                  .build();

  public CommentReadService(CommentDao commentDao, UserDao userDao) {
    this.commentDao = commentDao;
    this.userDao = userDao;
  }

  /**
   * Получить объект комментария по идентификационному номеру
   *
   * @param id идентификационный номер комментария
   * @return объект комментария
   * @throws MessageNotFoundException если комментарий не найден
   */
  public Comment getById(int id) throws MessageNotFoundException {
    return commentDao.getById(id);
  }

  /**
   * Проверка, имеет ли указанный комментарий ответы.
   *
   * @param comment  объект комментария
   * @return true если есть ответы, иначе false
   */
  public boolean isHaveAnswers(@Nonnull Comment comment) {
    return commentDao.getReplaysCount(comment.getId())>0;
  }

  /**
   * Список комментариев топика.
   *
   * @param topic       топик
   * @param showDeleted вместе с удаленными
   * @return список комментариев топика
   */
  @Nonnull
  public CommentList getCommentList(@Nonnull Topic topic, boolean showDeleted) {
    if (showDeleted) {
      return new CommentList(commentDao.getCommentList(topic.getId(), true), topic.getLastModified().getTime());
    } else {
      CommentList commentList = cache.getIfPresent(topic.getId());

      if (commentList == null || commentList.getLastmod() < topic.getLastModified().getTime()) {
        commentList = new CommentList(commentDao.getCommentList(topic.getId(), false), topic.getLastModified().getTime());
        cache.put(topic.getId(), commentList);
      }

      return commentList;
    }
  }

  /**
   * Получить список последних удалённых комментариев пользователя.
   *
   * @param user  объект пользователя
   * @return список удалённых комментариев пользователя
   */
  public List<CommentsListItem> getDeletedComments(User user) {
    return commentDao.getDeletedComments(user.getId());
  }

  @Nonnull
  public Set<Integer> makeHideSet(
          CommentList comments,
          int filterChain,
          @Nonnull Set<Integer> ignoreList
  ) throws UserNotFoundException {
    if (filterChain == CommentFilter.FILTER_NONE) {
      return ImmutableSet.of();
    }

    Set<Integer> hideSet = new HashSet<>();

    /* hide anonymous */
    if ((filterChain & CommentFilter.FILTER_ANONYMOUS) > 0) {
      comments.getRoot().hideAnonymous(userDao, hideSet);
    }

    /* hide ignored */
    if ((filterChain & CommentFilter.FILTER_IGNORED) > 0) {
      if (!ignoreList.isEmpty()) {
        comments.getRoot().hideIgnored(hideSet, ignoreList);
      }
    }

    return hideSet;
  }
}
