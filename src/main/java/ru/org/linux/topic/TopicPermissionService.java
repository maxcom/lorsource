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
import ru.org.linux.site.Template;
import ru.org.linux.spring.Configuration;
import ru.org.linux.user.User;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@Service
public class TopicPermissionService {
  public static final int POSTSCORE_MOD_AUTHOR = 9999;
  public static final int POSTSCORE_UNRESTRICTED = -9999;
  public static final int POSTSCORE_MODERATORS_ONLY = 10000;
  public static final int POSTSCORE_REGISTERED_ONLY = -50;

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
   * @param commentRequest WEB-форма, содержащая данные
   * @param request        данные запроса от web-клиента
   * @param user           пользователь, добавивший комментарий.
   * @param errors         обработчик ошибок ввода для формы
   * @return true если комментарий доступен для редактирования текущему пользователю, иначе false
   */
  public boolean isCommentsEditingAllowed(
    CommentRequest commentRequest,
    HttpServletRequest request,
    User user,
    Errors errors
  ) {
    Template tmpl = Template.getTemplate(request);

    /* Проверка на то, что пользователь модератор */
    Boolean editable = tmpl.isModeratorSession() && configuration.isModeratorAllowedToEditComments();

    /* проверка на то, что пользователь владелец комментария */
    if (!editable && commentRequest.getOriginal().getUserid() == user.getId()) {
      /* проверка на то, что время редактирования не вышло */
      Integer minutesToEdit = configuration.getCommentExpireMinutesForEdit();

      boolean isByMinutesEnable = false;
      if (minutesToEdit != null && !minutesToEdit.equals(0)) {
        long commentTimestamp = commentRequest.getOriginal().getPostdate().getTime();
        long deltaTimestamp = minutesToEdit * 60 * 1000;
        long nowTimestamp = new Date().getTime();

        isByMinutesEnable = commentTimestamp + deltaTimestamp > nowTimestamp;
      } else {
        isByMinutesEnable = true;
      }

      /* Проверка на то, что у комментария нет ответов */
      boolean isByAnswersEnable = true;
      if (!configuration.isCommentEditingAllowedIfAnswersExists()
        && commentService.isHaveAnswers(commentRequest.getOriginal())) {
        isByAnswersEnable = false;
      }

      /* Проверка на то, что у пользователя достаточно скора для редактирования комментария */
      Integer scoreToEdit = configuration.getCommentScoreValueForEditing();
      boolean isByScoreEnable = true;
      if (scoreToEdit != null && scoreToEdit > tmpl.getCurrentUser().getScore()) {
        isByScoreEnable = false;
      }

      editable = isByMinutesEnable & isByAnswersEnable & isByScoreEnable;
    }
    return editable;
  }

}
