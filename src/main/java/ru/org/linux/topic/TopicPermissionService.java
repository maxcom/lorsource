/*
 * Copyright 1998-2013 Linux.org.ru
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

package ru.org.linux.topic;

import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.comment.Comment;
import ru.org.linux.comment.CommentService;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.section.Section;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.spring.Configuration;
import ru.org.linux.user.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Service
public class TopicPermissionService {
  public static final int POSTSCORE_MOD_AUTHOR = 9999;
  public static final int POSTSCORE_UNRESTRICTED = -9999;
  public static final int POSTSCORE_MODERATORS_ONLY = 10000;
  public static final int POSTSCORE_REGISTERED_ONLY = -50;
  public static final int LINK_FOLLOW_MIN_SCORE = 100;
  public static final int VIEW_DELETED_SCORE = 100;
  public static final int DELETE_PERIOD = 60 * 60 * 1000; // milliseconds

  @Autowired
  private CommentService commentService;

  @Autowired
  private Configuration configuration;

  @Autowired
  private GroupDao groupDao;

  public static String getPostScoreInfo(int postscore) {
    switch (postscore) {
      case POSTSCORE_UNRESTRICTED:
        return "";
      case 100:
        return "<b>Ограничение на отправку комментариев</b>: " + User.getStars(100, 100);
      case 200:
        return "<b>Ограничение на отправку комментариев</b>: " + User.getStars(200, 200);
      case 300:
        return "<b>Ограничение на отправку комментариев</b>: " + User.getStars(300, 300);
      case 400:
        return "<b>Ограничение на отправку комментариев</b>: " + User.getStars(400, 400);
      case 500:
        return "<b>Ограничение на отправку комментариев</b>: " + User.getStars(500, 500);
      case POSTSCORE_MOD_AUTHOR:
        return "<b>Ограничение на отправку комментариев</b>: только для модераторов и автора";
      case POSTSCORE_MODERATORS_ONLY:
        return "<b>Ограничение на отправку комментариев</b>: только для модераторов";
      case POSTSCORE_REGISTERED_ONLY:
        return "<b>Ограничение на отправку комментариев</b>: только для зарегистрированных пользователей";
      default:
        return "<b>Ограничение на отправку комментариев</b>: только для зарегистрированных пользователей, score>=" + postscore;
    }
  }

  public void checkView(
          @Nonnull Group group,
          @Nonnull Topic message,
          @Nullable User currentUser,
          boolean showDeleted
  ) throws MessageNotFoundException, AccessViolationException {
    Preconditions.checkArgument(message.getGroupId()==group.getId());

    if (currentUser!=null && currentUser.isModerator()) {
      return;
    }

    if (message.isExpired() && showDeleted) {
      throw new MessageNotFoundException(message.getId(), "нельзя посмотреть удаленные комментарии в устаревших темах");
    }

    boolean unauthorized = currentUser == null || currentUser.isAnonymous();
    boolean topicAuthor = currentUser!=null && currentUser.getId() == message.getUid();

    if (message.isDeleted()) {
      if (message.isExpired()) {
        throw new MessageNotFoundException(message.getId(), "нельзя посмотреть устаревшие удаленные сообщения");
      }

      if (unauthorized) {
        throw new MessageNotFoundException(message.getId(), "Сообщение удалено");
      }

      if (topicAuthor) {
        return;
      }

      if (currentUser.getScore() < VIEW_DELETED_SCORE) {
        throw new MessageNotFoundException(message.getId(), "Сообщение удалено");
      }
    }

    if (message.isDraft()) {
      if (message.isExpired()) {
        throw new MessageNotFoundException(message.getId(), "Черновик устарел");
      }

      if (!topicAuthor) {
        throw new MessageNotFoundException(message.getId(), "Нельзя посмотреть чужой черновик");
      }
    }

    if (group.getCommentsRestriction() == -1 && unauthorized) {
      throw new AccessViolationException("Это сообщение нельзя посмотреть");
    }
  }

  public void checkCommentsAllowed(Topic topic, User user, Errors errors) {
    if (topic.isDeleted()) {
      errors.reject(null, "Нельзя добавлять комментарии к удаленному сообщению");
      return;
    }

    if (topic.isDraft()) {
      errors.reject(null, "Нельзя добавлять комментарии к черновику");
      return;
    }

    if (topic.isExpired()) {
      errors.reject(null, "Сообщение уже устарело");
      return;
    }

    Group group = groupDao.getGroup(topic.getGroupId());

    if (!isCommentsAllowed(group, topic, user)) {
      errors.reject(null, "Вы не можете добавлять комментарии в эту тему");
    }
  }

  private static int getCommentCountRestriction(Topic topic) {
    int commentCountPS = POSTSCORE_UNRESTRICTED;

    if (!topic.isSticky()) {
      int commentCount = topic.getCommentCount();

      if (commentCount > 3000) {
        commentCountPS = 200;
      } else if (commentCount > 2000) {
        commentCountPS = 100;
      } else if (commentCount > 1000) {
        commentCountPS = 50;
      }
    }

    return commentCountPS;
  }

  public int getPostscore(Group group, Topic topic) {
    int effective = Math.max(topic.getPostscore(), group.getCommentsRestriction());

    effective = Math.max(effective, Section.getCommentPostscore(topic.getSectionId()));

    effective = Math.max(effective, getCommentCountRestriction(topic));

    return effective;
  }

  public int getPostscore(Topic topic) {
    Group group = groupDao.getGroup(topic.getGroupId());

    return getPostscore(group, topic);
  }

  public boolean isCommentsAllowed(Group group, Topic topic, User user) {
    if (user != null && user.isBlocked()) {
      return false;
    }

    if (topic.isDeleted() || topic.isExpired() || topic.isDraft()) {
      return false;
    }

    int score = getPostscore(group, topic);

    if (score == POSTSCORE_UNRESTRICTED) {
      return true;
    }

    if (user == null || user.isAnonymous()) {
      return false;
    }

    if (user.isModerator()) {
      return true;
    }

    if (score == POSTSCORE_REGISTERED_ONLY) {
      return true;
    }

    if (score == POSTSCORE_MODERATORS_ONLY) {
      return false;
    }

    boolean isAuthor = user.getId() == topic.getUid();

    if (score == POSTSCORE_MOD_AUTHOR) {
      return isAuthor;
    }

    if (isAuthor) {
      return true;
    } else {
      return user.getScore() >= score;
    }
  }

  /**
   * Проверка на права редактирования комментария.
   *
   * @return true если комментарий доступен для редактирования текущему пользователю, иначе false
   */
  public boolean isCommentsEditingAllowed(
          @Nonnull Comment comment,
          @Nonnull Topic topic,
          @Nullable User currentUser
  ) {
    Preconditions.checkNotNull(comment);
    Preconditions.checkNotNull(topic);

    final boolean haveAnswers = commentService.isHaveAnswers(comment);
    return isCommentEditableNow(
        comment,
        currentUser,
        haveAnswers,
        topic
    );
  }

  /**
   * Проверяем можно ли редактировать комментарий на текущий момент
   *
   * @param haveAnswers есть у комменатрия ответы
   * @return результат
   */
  public boolean isCommentEditableNow(@Nonnull Comment comment, @Nullable User currentUser,
                                      boolean haveAnswers, @Nonnull Topic topic) {
    if (comment.isDeleted() || topic.isDeleted()) {
      return false;
    }

    if (currentUser==null || currentUser.isAnonymous()) {
      return false;
    }

    boolean moderatorMode = currentUser.isModerator();
    boolean authored = currentUser.getId() == comment.getUserid();

    /* Проверка на то, что пользователь модератор */
    boolean editable = moderatorMode && configuration.isModeratorAllowedToEditComments();

    if (!editable && authored) {
      /* проверка на то, что время редактирования не вышло */
      boolean isbyMinutesEnable;
      if (configuration.getCommentExpireMinutesForEdit() != 0) {
        long nowTimestamp = System.currentTimeMillis();
        long deltaTimestamp = configuration.getCommentExpireMinutesForEdit() * 60 * 1000;

        isbyMinutesEnable = comment.getPostdate().getTime() + deltaTimestamp > nowTimestamp;
      } else {
        isbyMinutesEnable = true;
      }

      /* Проверка на то, что у комментария нет ответов */
      boolean isbyAnswersEnable = configuration.isCommentEditingAllowedIfAnswersExists() || !haveAnswers;

      /* Проверка на то, что у пользователя достаточно скора для редактирования комментария */
      boolean isByScoreEnable = currentUser.getScore() >= configuration.getCommentScoreValueForEditing();

      editable = isbyMinutesEnable && isbyAnswersEnable && isByScoreEnable;
    }

    return editable;
  }

  /**
   * Проверяем можно ли удалять комментарий на текущий момент
   *
   * @param haveAnswers у комментрия есть ответы?
   * @return резултат
   */
  public boolean isCommentDeletableNow(
          Comment comment,
          @Nullable User currentUser,
          Topic topic,
          boolean haveAnswers
  ) {
    if (comment.isDeleted() || topic.isDeleted()) {
      return false;
    }

    if (currentUser==null || currentUser.isAnonymous()) {
      return false;
    }

    boolean moderatorMode = currentUser.isModerator();
    boolean authored = currentUser.getId() == comment.getUserid();

    long nowTimestamp = System.currentTimeMillis();

    return moderatorMode ||
        (!topic.isExpired() &&
         authored &&
         !haveAnswers &&
          nowTimestamp - comment.getPostdate().getTime() < DELETE_PERIOD);
  }

  /**
   * Follow для ссылок автора
   *
   * @param author автор сообщения содержащего ссылку
   * @return true обычная ссылка, false - добавить rel=nofollow
   */
  public boolean followAuthorLinks(User author) {
    if (author.isBlocked() || author.isAnonymous()) {
      return false;
    }

    return author.getScore()>= LINK_FOLLOW_MIN_SCORE;
  }

  /**
   * follow топиков которые подтверждены и у которых автор не заблокирован и
   * score > LINK_FOLLOW_MIN_SCORE
   * @param topic
   * @param author
   * @return
   */
  public boolean followInTopic(Topic topic, User author) {
    return topic.isCommited() || followAuthorLinks(author);
  }
}
