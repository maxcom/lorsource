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

package ru.org.linux.topic;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.comment.Comment;
import ru.org.linux.comment.CommentReadService;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.markup.MarkupPermissions;
import ru.org.linux.markup.MarkupType;
import ru.org.linux.section.Section;
import ru.org.linux.site.DeleteInfo;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.spring.SiteConfig;
import ru.org.linux.spring.dao.DeleteInfoDao;
import ru.org.linux.user.User;
import ru.org.linux.user.UserService;
import scala.Option;
import scala.Some;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.stream.IntStream;

@Service
public class TopicPermissionService {
  public static final int POSTSCORE_MOD_AUTHOR = 9999;
  public static final int POSTSCORE_UNRESTRICTED = -9999;
  public static final int POSTSCORE_MODERATORS_ONLY = 10000; // при смене номера поправить GroupListDao
  public static final int POSTSCORE_NO_COMMENTS = 10001; // запрещает новые, но оставляет старые
  public static final int POSTSCORE_HIDE_COMMENTS = 10002; // запрещает новые, скрывает старые
  public static final int POSTSCORE_REGISTERED_ONLY = -50;
  private static final int LINK_FOLLOW_MIN_SCORE = 100;
  private static final int VIEW_DELETED_SCORE = 100;
  private static final Duration DELETE_PERIOD = Duration.standardHours(3);
  public static final int VIEW_AFTER_DELETE_DAYS = 14; // для топика

  private final CommentReadService commentService;
  private final SiteConfig siteConfig;
  private final GroupDao groupDao;

  private final DeleteInfoDao deleteInfoDao;
  private final UserService userService;

  public TopicPermissionService(CommentReadService commentService, SiteConfig siteConfig, GroupDao groupDao,
                                DeleteInfoDao deleteInfoDao, UserService userService) {
    this.commentService = commentService;
    this.siteConfig = siteConfig;
    this.groupDao = groupDao;
    this.deleteInfoDao = deleteInfoDao;
    this.userService = userService;
  }

  public static String getPostScoreInfo(int postscore) {
    return switch (postscore) {
      case POSTSCORE_UNRESTRICTED ->
              "";
      case 50 ->
              "Закрыто добавление комментариев для недавно зарегистрированных пользователей (со score < 50)";
      case 100, 200, 300, 400, 500 ->
              "<b>Ограничение на отправку комментариев</b>: " + User.getStars(postscore, postscore, true);
      case POSTSCORE_MOD_AUTHOR ->
              "<b>Ограничение на отправку комментариев</b>: только для модераторов и автора";
      case POSTSCORE_MODERATORS_ONLY -> "" +
              "<b>Ограничение на отправку комментариев</b>: только для модераторов";
      case POSTSCORE_NO_COMMENTS ->
              "<b>Ограничение на отправку комментариев</b>: комментарии запрещены";
      case POSTSCORE_HIDE_COMMENTS ->
              "<b>Ограничение на отправку комментариев</b>: без комментариев";
      case POSTSCORE_REGISTERED_ONLY ->
              "<b>Ограничение на отправку комментариев</b>: только для зарегистрированных пользователей";
      default ->
              "<b>Ограничение на отправку комментариев</b>: только для зарегистрированных пользователей, score>=" + postscore;
    };
  }

  public boolean allowViewDeletedComments(Topic message, @Nullable User currentUser) {
    if (currentUser == null || !currentUser.isModerator()) {
      if (message.isExpired() || message.isDraft()) {
        return false;
      }

      if (message.getPostscore() == POSTSCORE_MODERATORS_ONLY ||
              message.getPostscore() == POSTSCORE_NO_COMMENTS ||
              message.getPostscore() == POSTSCORE_HIDE_COMMENTS) {
        return false;
      }

      boolean unauthorized = currentUser == null || currentUser.isAnonymous();

      if (unauthorized || currentUser.isFrozen()) {
        return false;
      }
    }

    return true;
  }

  public void checkView(
          Group group,
          Topic message,
          @Nullable User currentUser,
          User topicAuthor,
          boolean showDeleted
  ) throws MessageNotFoundException, AccessViolationException {
    Preconditions.checkArgument(message.getGroupId()==group.getId());
    Preconditions.checkArgument(message.getAuthorUserId()==topicAuthor.getId());

    if (currentUser == null || !currentUser.isModerator()) {
      boolean unauthorized = currentUser == null || currentUser.isAnonymous();

      if (showDeleted) {
        if (!allowViewDeletedComments(message, currentUser)) {
          throw new MessageNotFoundException(message.getId(), "вы не можете смотреть удаленные комментарии");
        }
      }

      boolean viewByAuthor = currentUser != null && currentUser.getId() == message.getAuthorUserId();

      if (message.isDeleted()) {
        if (message.isExpired()) {
          throw new MessageNotFoundException(message.getId(), "нельзя посмотреть устаревшие удаленные сообщения");
        }

        if (unauthorized) {
          throw new MessageNotFoundException(message.getId(), "Сообщение удалено");
        }

        if (!viewByAuthor) {
          final var deleteExpire =
                  deleteInfoDao.getDeleteInfo(message.getId())
                          .map(DeleteInfo::delDate)
                          .map(Timestamp::toInstant)
                          .map(t -> t.isBefore(Instant.now().minus(VIEW_AFTER_DELETE_DAYS, ChronoUnit.DAYS)))
                          .orElse(true);

          if (deleteExpire) {
            throw new MessageNotFoundException(message.getId(), "нельзя посмотреть устаревшие удаленные сообщения");
          }

          if (currentUser.isFrozen()) {
            throw new MessageNotFoundException(message.getId(), "Сообщение удалено");
          }

          if (currentUser.getScore() < VIEW_DELETED_SCORE) {
            throw new MessageNotFoundException(message.getId(), "Сообщение удалено");
          }

          if (topicAuthor.isModerator()) {
            throw new MessageNotFoundException(message.getId(), "Сообщение удалено");
          }
        }
      }

      if (message.isDraft()) {
        if (message.isExpired()) {
          throw new MessageNotFoundException(message.getId(), "Черновик устарел");
        }

        if (!viewByAuthor) {
          throw new MessageNotFoundException(message.getId(), "Нельзя посмотреть чужой черновик");
        }
      }

      boolean viewByCorrector = currentUser != null && currentUser.canCorrect();

      if (group.isPremoderated() && !message.isCommited() && topicAuthor.isAnonymous() && !viewByCorrector) {
        throw new AccessViolationException("Это сообщение нельзя посмотреть");
      }
    }
  }

  public void checkCommentsAllowed(Topic topic, Optional<User> user, Errors errors) {
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

    if (!isCommentsAllowed(group, topic, user, false)) {
      errors.reject(null, "Вы не можете добавлять комментарии в эту тему");
    }
  }

  private static int getCommentCountRestriction(Topic topic) {
    int postscore = POSTSCORE_UNRESTRICTED;

    if (!topic.isSticky()) {
      int commentCount = topic.getCommentCount();

      if (commentCount > 3000) {
        postscore = 200;
      } else if (commentCount > 2000) {
        postscore = 100;
      } else if (commentCount > 1000) {
        postscore = 50;
      }
    }

    return postscore;
  }

  private int getAllowAnonymousPostscore(Topic topic) {
    if (topic.isAllowAnonymous()) {
      return POSTSCORE_UNRESTRICTED;
    } else {
      return POSTSCORE_REGISTERED_ONLY;
    }
  }

  public int getPostscore(Group group, Topic topic) {
    return IntStream.of(topic.getPostscore(), group.commentsRestriction(),
            Section.getCommentPostscore(topic.getSectionId()),
            getCommentCountRestriction(topic), getAllowAnonymousPostscore(topic)).max().getAsInt();
  }

  public int getPostscore(Topic topic) {
    Group group = groupDao.getGroup(topic.getGroupId());

    return getPostscore(group, topic);
  }

  public boolean isCommentsAllowed(Group group, Topic topic, Optional<User> user, boolean ignoreFrozen) {
    if (topic.isDeleted() || topic.isExpired() || topic.isDraft()) {
      return false;
    }

    var effectiveUser = user.orElseGet(userService::getAnonymous);

    if (effectiveUser.isBlocked() || (!ignoreFrozen && effectiveUser.isFrozen())) {
      return false;
    }

    int score = getPostscore(group, topic);

    if (score == POSTSCORE_NO_COMMENTS || score == POSTSCORE_HIDE_COMMENTS) {
      return false;
    }

    if (score == POSTSCORE_UNRESTRICTED) {
      return true;
    }

    if (user.isEmpty() || user.get().isAnonymous()) {
      return false;
    } else {
      if (user.get().isModerator()) {
        return true;
      }

      if (score == POSTSCORE_REGISTERED_ONLY) {
        return true;
      }

      if (score == POSTSCORE_MODERATORS_ONLY) {
        return false;
      }

      boolean isAuthor = user.get().getId() == topic.getAuthorUserId();

      if (score == POSTSCORE_MOD_AUTHOR) {
        return isAuthor;
      }

      if (isAuthor) {
        return true;
      } else {
        return user.get().getScore() >= score;
      }
    }
  }

  /**
   * Проверка на права редактирования комментария.
   */
  public void checkCommentsEditingAllowed(
          @Nonnull Comment comment,
          @Nonnull Topic topic,
          @Nullable User currentUser,
          Errors errors,
          MarkupType markup
  ) {
    Preconditions.checkNotNull(comment);
    Preconditions.checkNotNull(topic);

    boolean haveAnswers = commentService.hasAnswers(comment);

    checkCommentEditableNow(comment, currentUser, haveAnswers, topic, errors, markup);
  }

  public Option<DateTime> getEditDeadline(Comment comment) {
    if (siteConfig.getCommentExpireMinutesForEdit() != 0) {
      DateTime editDeadline = new DateTime(comment.getPostdate()).plusMinutes(siteConfig.getCommentExpireMinutesForEdit());

      return Some.apply(editDeadline);
    } else {
      return Option.empty();
    }
  }
  /**
   * Проверяем можно ли редактировать комментарий на текущий момент
   *
   * @param haveAnswers есть у комменатрия ответы
   * @return результат
   */
  public boolean isCommentEditableNow(@Nonnull Comment comment, @Nullable User currentUser,
                                      boolean haveAnswers, @Nonnull Topic topic, MarkupType markup) {
    Errors errors = new MapBindingResult(ImmutableMap.of(), "obj");

    checkCommentsAllowed(topic, Optional.ofNullable(currentUser), errors);
    checkCommentEditableNow(comment, currentUser, haveAnswers, topic, errors, markup);

    return !errors.hasErrors();
  }

  /**
   * Проверяем можно ли редактировать комментарий на текущий момент
   *
   * @param haveAnswers есть у комменатрия ответы
   */
  private void checkCommentEditableNow(@Nonnull Comment comment, @Nullable User currentUser,
                                      boolean haveAnswers, @Nonnull Topic topic, Errors errors, MarkupType markup) {
    if (comment.isDeleted() || topic.isDeleted()) {
      errors.reject(null, "Тема или комментарий удалены");
    }

    if (currentUser==null || currentUser.isAnonymous()) {
      errors.reject(null, "Анонимный пользователь");
    }

    boolean authored = currentUser!=null && (currentUser.getId() == comment.getUserid());

    /* Проверка на то, что пользователь модератор */
    boolean editable = currentUser!=null && (currentUser.isModerator() && siteConfig.isModeratorAllowedToEditComments());

    if (editable || authored) {
      /* проверка на то, что время редактирования не вышло */
      Option<DateTime> maybeDeadline = getEditDeadline(comment);

      if (maybeDeadline.isDefined() && maybeDeadline.get().isBeforeNow()) {
        errors.reject(null, "Истек срок редактирования");
      }

      /* Проверка на то, что у комментария нет ответов */
      if (!siteConfig.isCommentEditingAllowedIfAnswersExists() && haveAnswers) {
        errors.reject(null, "Редактирование комментариев с ответами запрещено");
      }

      /* Проверка на то, что у пользователя достаточно скора для редактирования комментария */
      if (currentUser.getScore() < siteConfig.getCommentScoreValueForEditing()) {
        errors.reject(null, "У вас недостаточно прав для редактирования этого комментария");
      }

      if (!MarkupPermissions.allowedFormatsJava(currentUser).contains(markup)) {
        errors.reject(null, "Вы не можете редактировать тексты данного формата");
      }
    } else {
      errors.reject(null, "У вас недостаточно прав для редактирования этого комментария");
    }
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

    DateTime deleteDeadline = new DateTime(comment.getPostdate()).plus(DELETE_PERIOD);

    return moderatorMode ||
        (!topic.isExpired() &&
         authored &&
         !haveAnswers &&
          deleteDeadline.isAfterNow());
  }

  /**
   * Follow для ссылок автора
   *
   * @param author автор сообщения содержащего ссылку
   * @return true обычная ссылка, false - добавить rel=nofollow
   */
  public boolean followAuthorLinks(User author) {
    if (author.isBlocked() || author.isAnonymous() || author.isFrozen()) {
      return false;
    }

    return author.getScore()>= LINK_FOLLOW_MIN_SCORE;
  }

  /**
   * follow топиков которые подтверждены и у которых автор не заблокирован и
   * score > LINK_FOLLOW_MIN_SCORE
   */
  public boolean followInTopic(Topic topic, User author) {
    return topic.isCommited() || followAuthorLinks(author);
  }

  public boolean isUserCastAllowed(User author) {
    return author.getScore() >= 0;
  }

  public boolean isUndeletable(Topic topic, Comment comment, @Nullable User user, Optional<DeleteInfo> deleteInfo) {
    if (user==null) {
      return false;
    }

    if (topic.isDeleted() || !comment.isDeleted() || !user.isModerator() || topic.isExpired()) {
      return false;
    }

    if (comment.getUserid() == deleteInfo.map(DeleteInfo::userid).orElse(0)) {
      return false;
    }

    return true;
  }

  public boolean isTopicSearchable(Topic msg, Group group) {
    Preconditions.checkArgument(msg.getGroupId()==group.getId());

    return !msg.isDeleted()
            && !msg.isDraft()
            && (msg.getPostscore() != TopicPermissionService.POSTSCORE_HIDE_COMMENTS)
            && (!group.isPremoderated() || msg.isCommited() || msg.getAuthorUserId() != User.ANONYMOUS_ID);
  }
}
