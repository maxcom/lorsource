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
import ru.org.linux.comment.CommentService;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.markup.MarkupPermissions;
import ru.org.linux.markup.MarkupType;
import ru.org.linux.section.Section;
import ru.org.linux.site.DeleteInfo;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.spring.SiteConfig;
import ru.org.linux.user.User;
import scala.Option;
import scala.Some;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

  private final CommentService commentService;
  private final SiteConfig siteConfig;
  private final GroupDao groupDao;

  public TopicPermissionService(CommentService commentService, SiteConfig siteConfig, GroupDao groupDao) {
    this.commentService = commentService;
    this.siteConfig = siteConfig;
    this.groupDao = groupDao;
  }

  public static String getPostScoreInfo(int postscore) {
    switch (postscore) {
      case POSTSCORE_UNRESTRICTED:
        return "";
      case 50:
        return "Закрыто добавление комментариев для недавно зарегистрированных пользователей (со score < 50)";
      case 100:
      case 200:
      case 300:
      case 400:
      case 500:
        return "<b>Ограничение на отправку комментариев</b>: " + User.getStars(postscore, postscore, true);
      case POSTSCORE_MOD_AUTHOR:
        return "<b>Ограничение на отправку комментариев</b>: только для модераторов и автора";
      case POSTSCORE_MODERATORS_ONLY:
        return "<b>Ограничение на отправку комментариев</b>: только для модераторов";
      case POSTSCORE_NO_COMMENTS:
        return "<b>Ограничение на отправку комментариев</b>: комментарии запрещены";
      case POSTSCORE_HIDE_COMMENTS:
        return "<b>Ограничение на отправку комментариев</b>: без комментариев";
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

    if (showDeleted) {
      if (message.isExpired()) {
        throw new MessageNotFoundException(message.getId(), "нельзя посмотреть удаленные комментарии в устаревших темах");
      }

      if (message.getPostscore() == POSTSCORE_MODERATORS_ONLY ||
              message.getPostscore() == POSTSCORE_NO_COMMENTS ||
              message.getPostscore() == POSTSCORE_HIDE_COMMENTS) {
        throw new MessageNotFoundException(message.getId(), "нельзя посмотреть удаленные комментарии в закрытых темах");
      }
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

  private int getAllowAnonymousPostscore(Topic topic) {
    if (topic.isAllowAnonymous()) {
      return POSTSCORE_UNRESTRICTED;
    } else {
      return POSTSCORE_REGISTERED_ONLY;
    }
  }

  public int getPostscore(Group group, Topic topic) {
    return IntStream.of(topic.getPostscore(), group.getCommentsRestriction(),
            Section.getCommentPostscore(topic.getSectionId()),
            getCommentCountRestriction(topic), getAllowAnonymousPostscore(topic)).max().getAsInt();
  }

  public int getPostscore(Topic topic) {
    Group group = groupDao.getGroup(topic.getGroupId());

    return getPostscore(group, topic);
  }

  public boolean isCommentsAllowed(Group group, Topic topic, User user) {
    if (user != null && (user.isBlocked() || user.isFrozen())) {
      return false;
    }

    if (topic.isDeleted() || topic.isExpired() || topic.isDraft()) {
      return false;
    }

    int score = getPostscore(group, topic);

    if (score == POSTSCORE_NO_COMMENTS || score == POSTSCORE_HIDE_COMMENTS) {
      return false;
    }

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

    boolean haveAnswers = commentService.isHaveAnswers(comment);

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

    checkCommentsAllowed(topic, currentUser, errors);
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
    if (author.isBlocked() || author.isAnonymous()) {
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
    return !author.isAnonymousScore();
  }

  public boolean isUndeletable(Topic topic, Comment comment, @Nullable User user, DeleteInfo deleteInfo) {
    if (user==null) {
      return false;
    }

    if (topic.isDeleted() || !comment.isDeleted() || !user.isModerator() || topic.isExpired()) {
      return false;
    }

    if (comment.getUserid() == deleteInfo.userid()) {
      return false;
    }

    return true;
  }
}
