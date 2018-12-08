/*
 * Copyright 1998-2018 Linux.org.ru
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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import ru.org.linux.auth.CaptchaService;
import ru.org.linux.auth.FloodProtector;
import ru.org.linux.auth.IPBlockDao;
import ru.org.linux.auth.IPBlockInfo;
import ru.org.linux.csrf.CSRFProtectionService;
import ru.org.linux.edithistory.EditHistoryRecord;
import ru.org.linux.edithistory.EditHistoryObjectTypeEnum;
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
import ru.org.linux.util.StringUtil;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.beans.PropertyEditorSupport;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentService {
  private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

  @Autowired
  private CommentDao commentDao;

  @Autowired
  private TopicDao topicDao;

  @Autowired
  private UserDao userDao;

  @Autowired
  private UserService userService;

  @Autowired
  private CaptchaService captcha;

  @Autowired
  private CommentPrepareService commentPrepareService;

  @Autowired
  private FloodProtector floodProtector;

  @Autowired
  private MessageTextService textService;

  @Autowired
  private UserEventService userEventService;

  @Autowired
  private MsgbaseDao msgbaseDao;

  @Autowired
  private IgnoreListDao ignoreListDao;

  @Autowired
  private EditHistoryService editHistoryService;

  @Autowired
  private TopicPermissionService permissionService;

  private final Cache<Integer, CommentList> cache =
          CacheBuilder.newBuilder()
          .maximumSize(10000)
          .build();

  public void requestValidator(WebDataBinder binder) {
    binder.setValidator(new CommentRequestValidator(textService));
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
    Errors errors
  )  {
    if (commentRequest.getMsg() == null) {
      errors.rejectValue("msg", null, "комментарий не задан");
      commentRequest.setMsg("");
    }

    Template tmpl = Template.getTemplate(request);

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

    IPBlockDao.checkBlockIP(ipBlockInfo, errors, user);

    if (!commentRequest.isPreviewMode() && !errors.hasErrors()) {
      floodProtector.checkDuplication(FloodProtector.Action.ADD_COMMENT, request.getRemoteAddr(), user.getScore() >= 100, errors);
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

      String title = commentRequest.getTitle();

      if (title == null) {
        title = "";
      }

      Integer replyto = commentRequest.getReplyto() != null ? commentRequest.getReplyto().getId() : null;

      int commentId = commentRequest.getOriginal() == null
        ? 0
        : commentRequest.getOriginal().getId();

      comment = new Comment(
        replyto,
        StringUtil.escapeHtml(title),
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
   * @param commentRequest  WEB-форма, содержащая данные
   * @param request         данные запроса от web-клиента
   * @param errors          обработчик ошибок ввода для формы
   * @return объект пользователя
   */
  @Nonnull
  public User getCommentUser(
    CommentRequest commentRequest,
    HttpServletRequest request,
    Errors errors
  ) {
    Template tmpl = Template.getTemplate(request);

    User currentUser = tmpl.getCurrentUser();

    if (currentUser!=null) {
      return currentUser;
    }

    if (commentRequest.getNick() != null) {
      if (commentRequest.getPassword() == null) {
        errors.reject(null, "Требуется авторизация");
      }

      return commentRequest.getNick();
    } else {
      return userService.getAnonymous();
    }
  }

  public ImmutableMap<String, Object> prepareReplyto(CommentRequest add) throws UserNotFoundException {
    if (add.getReplyto() != null) {
      return ImmutableMap.of("onComment", commentPrepareService.prepareCommentForReplyto(add.getReplyto()));
    } else {
      return ImmutableMap.of();
    }
  }

  /**
   * Создание нового комментария.
   *
   *
   * @param comment        объект комментария
   * @param commentBody    текст комментария
   * @param remoteAddress  IP-адрес, с которого был добавлен комментарий
   * @param xForwardedFor  IP-адрес через шлюз, с которого был добавлен комментарий
   * @param userAgent      заголовок User-Agent запроса
   * @return идентификационный номер нового комментария
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public int create(
          @Nonnull User author,
          @Nonnull Comment comment,
          MessageText commentBody,
          String remoteAddress,
          String xForwardedFor,
          Optional<String> userAgent) throws MessageNotFoundException {
    Preconditions.checkArgument(comment.getUserid() == author.getId());

    int commentId = commentDao.saveNewMessage(comment, userAgent);
    msgbaseDao.saveNewMessage(commentBody, commentId);

    /* кастование пользователей */
    if (permissionService.isUserCastAllowed(author)) {
      Set<User> userRefs = textService.mentions(commentBody);
      userRefs = userRefs.stream()
              .filter(p -> !userService.isIgnoring(p.getId(), author.getId()))
              .collect(Collectors.toSet());
      userEventService.addUserRefEvent(userRefs, comment.getTopicId(), commentId);
    }

    /* оповещение об ответе на коммент */
    if (comment.getReplyTo() != 0) {
      Comment parentComment = commentDao.getById(comment.getReplyTo());

      if (parentComment.getUserid() != comment.getUserid()) {
        User parentAuthor = userDao.getUserCached(parentComment.getUserid());

        if (!parentAuthor.isAnonymous()) {
          Set<Integer> ignoreList = ignoreListDao.get(parentAuthor);

          if (!ignoreList.contains(comment.getUserid())) {
            userEventService.addReplyEvent(
                    parentAuthor,
                    comment.getTopicId(),
                    commentId
            );
          }
        }
      }
    }

    String logMessage = makeLogString("Написан комментарий " + commentId, remoteAddress, xForwardedFor);
    logger.info(logMessage);

    return commentId;
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
  public void edit(
    Comment oldComment,
    Comment newComment,
    String commentBody,
    String remoteAddress,
    String xForwardedFor,
    @Nonnull User editor,
    MessageText originalMessageText
  ) {
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
   * Проверка, имеет ли указанный комментарий ответы.
   *
   * @param comment  объект комментария
   * @return true если есть ответы, иначе false
   */
  public boolean isHaveAnswers(@Nonnull Comment comment) {
    return commentDao.getReplaysCount(comment.getId())>0;
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
  public List<CommentDao.DeletedListItem> getDeletedComments(User user) {
    return commentDao.getDeletedComments(user.getId());
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

  @Nonnull
  public Set<Integer> makeHideSet(
          CommentList comments,
          int filterChain,
          @Nonnull Set<Integer> ignoreList
  ) throws SQLException, UserNotFoundException {
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
