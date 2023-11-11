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

package ru.org.linux.comment;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import ru.org.linux.auth.*;
import ru.org.linux.csrf.CSRFProtectionService;
import ru.org.linux.edithistory.EditHistoryObjectTypeEnum;
import ru.org.linux.edithistory.EditHistoryRecord;
import ru.org.linux.edithistory.EditHistoryService;
import ru.org.linux.markup.MessageTextService;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.site.Template;
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.topic.TopicPermissionService;
import ru.org.linux.user.*;
import ru.org.linux.util.ExceptionBindingErrorProcessor;
import scala.Tuple2;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.beans.PropertyEditorSupport;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommentCreateService {
  private static final Logger logger = LoggerFactory.getLogger(CommentCreateService.class);

  private final CommentDao commentDao;
  private final TopicDao topicDao;
  private final UserService userService;
  private final CaptchaService captcha;
  private final CommentPrepareService commentPrepareService;
  private final FloodProtector floodProtector;
  private final MessageTextService textService;
  private final UserEventService userEventService;
  private final MsgbaseDao msgbaseDao;
  private final IgnoreListDao ignoreListDao;
  private final EditHistoryService editHistoryService;
  private final TopicPermissionService permissionService;

  public CommentCreateService(CommentDao commentDao, TopicDao topicDao, UserService userService, CaptchaService captcha,
                              CommentPrepareService commentPrepareService, FloodProtector floodProtector,
                              EditHistoryService editHistoryService, MessageTextService textService,
                              UserEventService userEventService, MsgbaseDao msgbaseDao, IgnoreListDao ignoreListDao,
                              TopicPermissionService permissionService) {
    this.commentDao = commentDao;
    this.topicDao = topicDao;
    this.userService = userService;
    this.captcha = captcha;
    this.commentPrepareService = commentPrepareService;
    this.floodProtector = floodProtector;
    this.editHistoryService = editHistoryService;
    this.textService = textService;
    this.userEventService = userEventService;
    this.msgbaseDao = msgbaseDao;
    this.ignoreListDao = ignoreListDao;
    this.permissionService = permissionService;
  }

  public void requestValidator(WebDataBinder binder) {
    binder.setValidator(new CommentRequestValidator());
    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor());
  }

  public void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(Topic.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String text) throws IllegalArgumentException {
        try {
          setValue(topicDao.getById(Integer.parseInt(text.split(",")[0])));
        } catch (MessageNotFoundException e) {
          throw new IllegalArgumentException(e);
        }
      }
    });

    binder.registerCustomEditor(Comment.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String text) throws IllegalArgumentException {
        if (text.isEmpty() || "0".equals(text)) {
          setValue(null);
          return;
        }

        try {
          setValue(commentDao.getById(Integer.parseInt(text)));
        } catch (MessageNotFoundException e) {
          throw new IllegalArgumentException(e);
        }
      }
    });

    binder.registerCustomEditor(User.class, new UserPropertyEditor(userService));
  }

  /**
   * Проверка валидности данных запроса.
   *
   * @param commentRequest  WEB-форма, содержащая данные
   * @param user            пользователь, добавляющий или изменяющий комментарий
   * @param ipBlockInfo     информация о банах
   * @param request         данные запроса от web-клиента
   * @param errors          обработчик ошибок ввода для формы
   */
  public void checkPostData(
    CommentRequest commentRequest,
    User user,
    IPBlockInfo ipBlockInfo,
    HttpServletRequest request,
    Errors errors,
    boolean editMode
  )  {
    if (commentRequest.getMsg() == null) {
      errors.rejectValue("msg", null, "комментарий не задан");
      commentRequest.setMsg("");
    }

    Template tmpl = Template.getTemplate();

    if (commentRequest.getMode() == null) {
      commentRequest.setMode(tmpl.getFormatMode());
    }

    if (!commentRequest.isPreviewMode() &&
      (!tmpl.isSessionAuthorized() || ipBlockInfo.isCaptchaRequired())) {
      captcha.checkCaptcha(request, errors);
    }

    if (!commentRequest.isPreviewMode() && !errors.hasErrors()) {
      CSRFProtectionService.checkCSRF(request, errors);
    }

    user.checkBlocked(errors);
    user.checkFrozen(errors);

    IPBlockDao.checkBlockIP(ipBlockInfo, errors, user);

    if (!commentRequest.isPreviewMode() && !errors.hasErrors() && !editMode) {
      floodProtector.checkDuplication(FloodProtector.Action.ADD_COMMENT, request.getRemoteAddr(), user, errors);
    }
  }

  /**
   * Получить текст комментария.
   *
   * @param commentRequest  WEB-форма, содержащая данные
   * @param user            пользователь, добавляющий или изменяющий комментарий
   * @param errors          обработчик ошибок ввода для формы
   * @return текст комментария
   */
  public MessageText getCommentBody(
    CommentRequest commentRequest,
    User user,
    Errors errors
  ) {
    MessageText messageText = MessageTextService.processPostingText(commentRequest.getMsg(), commentRequest.getMode());
    String commentBody = (messageText.text());

    if (user.isAnonymous()) {
      if (commentBody.length() > 4096) {
        errors.rejectValue("msg", null, "Слишком большое сообщение");
      }
    } else {
      if (commentBody.length() > 8192) {
        errors.rejectValue("msg", null, "Слишком большое сообщение");
      }
    }

    return messageText;
  }

  /**
   * Получить объект комментария из WEB-запроса.
   *
   * @param commentRequest  WEB-форма, содержащая данные
   * @param user            пользователь, добавляющий или изменяющий комментарий
   * @param request         данные запроса от web-клиента
   * @return объект комментария из WEB-запроса
   */
  public Comment getComment(
    CommentRequest commentRequest,
    User user,
    HttpServletRequest request
  ) {
    Comment comment = null;

    if (commentRequest.getTopic() != null) {

      Integer replyto = commentRequest.getReplyto() != null ? commentRequest.getReplyto().getId() : null;

      int commentId = commentRequest.getOriginal() == null
        ? 0
        : commentRequest.getOriginal().getId();

      comment = Comment.buildNew(
        replyto,
        commentRequest.getTopic().getId(),
        commentId,
        user.getId(),
        request.getRemoteAddr()
      );
    }
    return comment;
  }

  /**
   * Получить объект пользователя, добавляющего или изменяющего комментарий
   *
   * @param commentRequest WEB-форма, содержащая данные
   * @param errors         обработчик ошибок ввода для формы
   * @return объект пользователя
   */
  public User getCommentUser(@Nullable User currentUser, CommentRequest commentRequest, Errors errors) {
    if (currentUser!=null) {
      return currentUser;
    } else {
      if (commentRequest.getNick() != null) {
        if (commentRequest.getPassword() == null) {
          errors.reject(null, "Требуется авторизация");
        }

        return commentRequest.getNick();
      } else {
        return userService.getAnonymous();
      }
    }
  }

  public ImmutableMap<String, Object> prepareReplyto(CommentRequest add, @Nullable User currentUser,
                                                     Profile profile, Topic topic) throws UserNotFoundException {
    if (add.getReplyto() != null) {
      Set<Integer> ignoreList;

      if (currentUser!=null) {
        ignoreList = ignoreListDao.getJava(currentUser);
      } else {
        ignoreList = ImmutableSet.of();
      }

      PreparedComment preparedComment = commentPrepareService.prepareCommentOnly(add.getReplyto(), currentUser,
              profile, topic, ignoreList);

      return ImmutableMap.of("onComment", preparedComment);
    } else {
      return ImmutableMap.of();
    }
  }

   /**
   * Создание нового комментария.
   *
   * @param comment        объект комментария
   * @param commentBody    текст комментария
   * @param remoteAddress  IP-адрес, с которого был добавлен комментарий
   * @param xForwardedFor  IP-адрес через шлюз, с которого был добавлен комментарий
   * @param userAgent      заголовок User-Agent запроса
   * @return идентификационный номер нового комментария + список пользователей у которых появились события
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public Tuple2<Integer, List<Integer>> create(
          User author,
          Comment comment,
          MessageText commentBody,
          String remoteAddress,
          String xForwardedFor,
          Optional<String> userAgent) throws MessageNotFoundException {
    Preconditions.checkArgument(comment.getUserid() == author.getId());

    ImmutableList.Builder<Integer> notifyUsers = ImmutableList.builder();

    int commentId = commentDao.saveNewMessage(comment, userAgent);
    msgbaseDao.saveNewMessage(commentBody, commentId);

    if (permissionService.isUserCastAllowed(author)) {
      Set<User> mentions = notifyMentions(author, comment, commentBody, commentId);

      notifyUsers.addAll(mentions.stream().map(User::getId).iterator());
    }

    Optional<Comment> parentCommentOpt;

    if (comment.getReplyTo() != 0) {
      Comment parentComment = commentDao.getById(comment.getReplyTo());

      Optional<User> mention = notifyReply(comment, commentId, parentComment);

      mention.ifPresent(user -> notifyUsers.add(user.getId()));

      parentCommentOpt = Optional.of(parentComment);
    } else {
      parentCommentOpt = Optional.empty();
    }

    List<Integer> commentNotified = userEventService.insertCommentWatchNotification(comment, parentCommentOpt, commentId);
    notifyUsers.addAll(commentNotified);

    String logMessage = makeLogString("Написан комментарий " + commentId, remoteAddress, xForwardedFor);
    logger.info(logMessage);

    return Tuple2.apply(commentId, notifyUsers.build());
  }

  /* оповещение об ответе на коммент */
  private Optional<User> notifyReply(Comment comment, int commentId, Comment parentComment) {
    if (parentComment.getUserid() != comment.getUserid()) {
      User parentAuthor = userService.getUserCached(parentComment.getUserid());

      if (!parentAuthor.isAnonymous()) {
        Set<Integer> ignoreList = ignoreListDao.getJava(parentAuthor);

        if (!ignoreList.contains(comment.getUserid())) {
          userEventService.addReplyEvent(parentAuthor, comment.getTopicId(), commentId);
          return Optional.of(parentAuthor);
        }
      }
    }

    return Optional.empty();
  }

  /* кастование пользователей */
  private Set<User> notifyMentions(User author, Comment comment, MessageText commentBody, int commentId) {
    Set<User> userRefs = textService.mentions(commentBody);
    userRefs = userRefs.stream()
            .filter(p -> !userService.isIgnoring(p.getId(), author.getId()))
            .collect(Collectors.toSet());

    userEventService.addUserRefEvent(userRefs, comment.getTopicId(), commentId);

    return userRefs;
  }

  /**
   * Редактирование комментария.
   *
   * @param oldComment     данные старого комментария
   * @param newComment     данные нового комментария
   * @param commentBody    текст нового комментария
   * @param remoteAddress  IP-адрес, с которого был добавлен комментарий
   * @param xForwardedFor  IP-адрес через шлюз, с которого был добавлен комментарий
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void edit(Comment oldComment, Comment newComment, String commentBody, String remoteAddress,
                   String xForwardedFor, User editor, MessageText originalMessageText) {
    commentDao.changeTitle(oldComment, newComment.getTitle());
    msgbaseDao.updateMessage(oldComment.getId(), commentBody);

    /* кастование пользователей */
    Set<User> newUserRefs = textService.mentions(MessageText.apply(commentBody, originalMessageText.markup()));

    MessageText messageText = msgbaseDao.getMessageText(oldComment.getId());
    Set<User> oldUserRefs = textService.mentions(messageText);
    Set<User> userRefs = new HashSet<>();
    /* кастовать только тех, кто добавился. Существующие ранее не кастуются */
    for (User user :newUserRefs) {
      if (!oldUserRefs.contains(user)) {
        userRefs.add(user);
      }
    }

    if (permissionService.isUserCastAllowed(editor)) {
      userEventService.addUserRefEvent(userRefs, oldComment.getTopicId(), oldComment.getId());
    }

    /* Обновление времени последнего изменения топика для того, чтобы данные в кеше автоматически обновились  */
    topicDao.updateLastmod(oldComment.getTopicId(), false);

    addEditHistoryItem(editor, oldComment, originalMessageText.text(), newComment, commentBody);

    updateLatestEditorInfo(editor, oldComment, newComment);

    String logMessage = makeLogString("Изменён комментарий " + oldComment.getId(), remoteAddress, xForwardedFor);
    logger.info(logMessage);
  }

  /**
   * Добавить элемент истории для комментария.
   *
   * @param editor              пользователь, изменивший комментарий
   * @param original            оригинал (старый комментарий)
   * @param originalMessageText старое содержимое комментария
   * @param comment             изменённый комментарий
   * @param messageText         новое содержимое комментария
   */
  private void addEditHistoryItem(User editor, Comment original, String originalMessageText, Comment comment, String messageText) {
    EditHistoryRecord editHistoryRecord = new EditHistoryRecord();
    editHistoryRecord.setMsgid(original.getId());
    editHistoryRecord.setObjectType(EditHistoryObjectTypeEnum.COMMENT);
    editHistoryRecord.setEditor(editor.getId());

    boolean modified = false;
    if (!original.getTitle().equals(comment.getTitle())) {
      editHistoryRecord.setOldtitle(original.getTitle());
      modified = true;
    }

    if (!originalMessageText.equals(messageText)) {
      editHistoryRecord.setOldmessage(originalMessageText);
      modified = true;
    }

    if (modified) {
      editHistoryService.insert(editHistoryRecord);
    }
  }

  /**
   * Обновление информации о последнем изменении коммента.
   *
   * @param editor   пользователь, изменивший комментарий
   * @param original оригинал (старый комментарий)
   * @param comment  изменённый комментарий
   */
  private void updateLatestEditorInfo(User editor, Comment original, Comment comment) {
    int editCount = editHistoryService.editCount(original.getId(), EditHistoryObjectTypeEnum.COMMENT);

    commentDao.updateLatestEditorInfo(
      original.getId(),
      editor.getId(),
      comment.getPostdate(),
      editCount
    );
  }

  /**
   * Формирование строки в лог-файл.
   *
   * @param message        сообщение
   * @param remoteAddress  IP-адрес, с которого был добавлен комментарий
   * @param xForwardedFor  IP-адрес через шлюз, с которого был добавлен комментарий
   * @return строка, готовая для добавления в лог-файл
   */
  private static String makeLogString(String message, String remoteAddress, String xForwardedFor) {
    StringBuilder logMessage = new StringBuilder();

    logMessage
      .append(message)
      .append("; ip: ")
      .append(remoteAddress);

    if (xForwardedFor != null) {
      logMessage
        .append(" XFF:")
        .append(xForwardedFor);
    }

    return logMessage.toString();
  }
}
