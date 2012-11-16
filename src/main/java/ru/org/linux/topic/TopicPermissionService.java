/*
 * Copyright 1998-2012 Linux.org.ru
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import ru.org.linux.comment.CommentRequest;
import ru.org.linux.comment.CommentService;
import ru.org.linux.comment.DeleteCommentController;
import ru.org.linux.site.Template;
import ru.org.linux.spring.Configuration;
import ru.org.linux.user.User;

import javax.servlet.http.HttpServletRequest;

@Service
public class TopicPermissionService {
  public static final int POSTSCORE_MOD_AUTHOR = 9999;
  public static final int POSTSCORE_UNRESTRICTED = -9999;
  public static final int POSTSCORE_MODERATORS_ONLY = 10000;
  public static final int POSTSCORE_REGISTERED_ONLY = -50;
  public static final int LINK_FOLLOW_MIN_SCORE = 100;

  @Autowired
  private CommentService commentService;

  @Autowired
  private Configuration configuration;

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

  public void checkCommentsAllowed(Topic topic, User user, Errors errors) {
    if (topic.isDeleted()) {
      errors.reject(null, "Нельзя добавлять комментарии к удаленному сообщению");
      return;
    }

    if (topic.isExpired()) {
      errors.reject(null, "Сообщение уже устарело");
      return;
    }

    if (!isCommentsAllowed(topic, user)) {
      errors.reject(null, "Вы не можете добавлять комментарии в эту тему");
    }
  }
  
  public boolean isCommentsAllowed(Topic topic, User user) {
    if (user != null && user.isBlocked()) {
      return false;
    }

    if (topic.isDeleted() || topic.isExpired()) {
      return false;
    }

    int score = topic.getPostScore();

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
   *
   * @param commentRequest WEB-форма, содержащая данные
   * @param request        данные запроса от web-клиента
   * @param user           пользователь, добавивший комментарий.
   * @return true если комментарий доступен для редактирования текущему пользователю, иначе false
   */
  public boolean isCommentsEditingAllowed(
          CommentRequest commentRequest,
          HttpServletRequest request,
          User user
  ) {
    Template tmpl = Template.getTemplate(request);

    final boolean moderatorMode = tmpl.isModeratorSession();
    final boolean moderatorAllowEditComments = configuration.isModeratorAllowedToEditComments();
    final boolean commentEditingAllowedIfAnswersExists = configuration.isCommentEditingAllowedIfAnswersExists();
    final int commentScoreValueForEditing = configuration.getCommentScoreValueForEditing();
    final int userScore = tmpl.getCurrentUser().getScore();
    /* проверка на то, что пользователь владелец комментария */
    final boolean authored = (commentRequest.getOriginal().getUserid() == user.getId());
    final boolean haveAnswers = commentService.isHaveAnswers(commentRequest.getOriginal());
    final int commentExpireMinutesForEdit = configuration.getCommentExpireMinutesForEdit();
    final long commentTimestamp = commentRequest.getOriginal().getPostdate().getTime();
    return isCommentEditableNow(
        moderatorMode,
        moderatorAllowEditComments,
        commentEditingAllowedIfAnswersExists,
        commentScoreValueForEditing,
        userScore,
        authored,
        haveAnswers,
        commentExpireMinutesForEdit,
        commentTimestamp
    );
  }

  /**
   * Проверяем можно ли редактировать комментарий на текущий момент
   * @param moderatorMode текущий пользователь можератор
   * @param moderatorAllowEditComments модертор может редактировать?
   * @param commentEditingAllowedIfAnswersExists можно ли редактировать если есть ответы?
   * @param commentScoreValueForEditing кол-во шкворца необходимое для редактирования
   * @param userScore кол-во шгкворца у текущего пользователя
   * @param authored является текущий пользователь автором комментария
   * @param haveAnswers есть у комменатрия ответы
   * @param commentExpireMinutesForEdit после скольки минут редактировать невкоем случае нельзя
   * @param commentTimestamp время создания комментария
   * @return результат
   */
  public boolean isCommentEditableNow(boolean moderatorMode, boolean moderatorAllowEditComments, boolean commentEditingAllowedIfAnswersExists,
                                int commentScoreValueForEditing, int userScore,
                                boolean authored, boolean haveAnswers, int commentExpireMinutesForEdit, long commentTimestamp) {
    /* Проверка на то, что пользователь модератор */
    Boolean editable = moderatorMode && moderatorAllowEditComments;
    long nowTimestamp = System.currentTimeMillis();
    if (!editable && authored) {

      /* проверка на то, что время редактирования не вышло */
      boolean isbyMinutesEnable;
      if (commentExpireMinutesForEdit != 0) {
        long deltaTimestamp = commentExpireMinutesForEdit * 60 * 1000;

        isbyMinutesEnable = commentTimestamp + deltaTimestamp > nowTimestamp;
      } else {
        isbyMinutesEnable = true;
      }

      /* Проверка на то, что у комментария нет ответов */
      boolean isbyAnswersEnable = true;
      if (!commentEditingAllowedIfAnswersExists
        && haveAnswers) {
        isbyAnswersEnable = false;
      }

      /* Проверка на то, что у пользователя достаточно скора для редактирования комментария */
      boolean isByScoreEnable = true;
      if (commentScoreValueForEditing > userScore) {
        isByScoreEnable = false;
      }

      editable = isbyMinutesEnable && isbyAnswersEnable && isByScoreEnable;
    }
    return editable;
  }

  /**
   * Проверяем можно ли удалять комментарий на текущий момент
   * @param moderatorMode текущий пользователь модератор?
   * @param expired топик устарел(архивный)?
   * @param authored текущий пользьователь автор комментария?
   * @param haveAnswers у комментрия есть ответы?
   * @param commentTimestamp время создания комментария
   * @return резултат
   */
  public boolean isCommentDeletableNow(boolean moderatorMode, boolean expired, boolean authored, boolean haveAnswers, long commentTimestamp ) {
    long nowTimestamp = System.currentTimeMillis();
    return moderatorMode ||
        (!expired &&
         authored &&
         !haveAnswers &&
          nowTimestamp - commentTimestamp < DeleteCommentController.DELETE_PERIOD);
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

    return author.getScore()>=TopicPermissionService.LINK_FOLLOW_MIN_SCORE;
  }
}
