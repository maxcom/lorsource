/*
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

package ru.org.linux.comment;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.xbill.DNS.TextParseException;
import ru.org.linux.auth.CaptchaService;
import ru.org.linux.auth.FloodProtector;
import ru.org.linux.auth.IPBlockDao;
import ru.org.linux.auth.IPBlockInfo;
import ru.org.linux.csrf.CSRFProtectionService;
import ru.org.linux.edithistory.EditHistoryDto;
import ru.org.linux.edithistory.EditHistoryObjectTypeEnum;
import ru.org.linux.edithistory.EditHistoryService;
import ru.org.linux.site.MemCachedSettings;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.site.ScriptErrorException;
import ru.org.linux.site.Template;
import ru.org.linux.spring.commons.CacheProvider;
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.topic.TopicService;
import ru.org.linux.user.*;
import ru.org.linux.util.ExceptionBindingErrorProcessor;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.bbcode.LorCodeService;
import ru.org.linux.util.formatter.ToLorCodeFormatter;
import ru.org.linux.util.formatter.ToLorCodeTexFormatter;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.beans.PropertyEditorSupport;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CommentService {
  private static final Log logger = LogFactory.getLog(CommentService.class);

  @Autowired
  private CommentDao commentDao;

  @Autowired
  private TopicDao messageDao;

  @Autowired
  private TopicService topicService;

  @Autowired
  private UserDao userDao;

  @Autowired
  private ToLorCodeFormatter toLorCodeFormatter;

  @Autowired
  private ToLorCodeTexFormatter toLorCodeTexFormatter;

  @Autowired
  private CaptchaService captcha;

  @Autowired
  private CommentPrepareService commentPrepareService;

  @Autowired
  private FloodProtector floodProtector;

  @Autowired
  private LorCodeService lorCodeService;

  @Autowired
  private UserEventService userEventService;

  @Autowired
  private MsgbaseDao msgbaseDao;

  @Autowired
  private IgnoreListDao ignoreListDao;

  @Autowired
  private EditHistoryService editHistoryService;

  @Autowired
  private TopicDao topicDao;

  public void requestValidator(WebDataBinder binder) {
    binder.setValidator(new CommentRequestValidator(lorCodeService));
    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor());
  }

  public void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(Topic.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String text) throws IllegalArgumentException {
        try {
          setValue(messageDao.getById(Integer.parseInt(text.split(",")[0])));
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

    binder.registerCustomEditor(User.class, new UserPropertyEditor(userDao));
  }

  /**
   * Проверка валидности данных запроса.
   *
   * @param commentRequest  WEB-форма, содержащая данные
   * @param user            пользователь, добавляющий или изменяющий комментарий
   * @param ipBlockInfo     информация о банах
   * @param request         данные запроса от web-клиента
   * @param errors          обработчик ошибок ввода для формы
   * @throws UnknownHostException
   * @throws TextParseException
   */
  public void checkPostData(
    CommentRequest commentRequest,
    User user,
    IPBlockInfo ipBlockInfo,
    HttpServletRequest request,
    Errors errors
  ) throws
    UnknownHostException,
    TextParseException {
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
  public String getCommentBody(
    CommentRequest commentRequest,
    User user,
    Errors errors
  ) {
    String commentBody = processMessage(commentRequest.getMsg(), commentRequest.getMode());
    if (user.isAnonymous()) {
      if (commentBody.length() > 4096) {
        errors.rejectValue("msg", null, "Слишком большое сообщение");
      }
    } else {
      if (commentBody.length() > 8192) {
        errors.rejectValue("msg", null, "Слишком большое сообщение");
      }
    }
    return commentBody;
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
      return userDao.getAnonymous();
    }
  }

  public void prepareReplyto(
    CommentRequest add,
    Map<String, Object> formParams,
    HttpServletRequest request
  ) throws UserNotFoundException {
    if (add.getReplyto() != null) {
      formParams.put("onComment", commentPrepareService.prepareCommentForReplayto(add.getReplyto(), request.isSecure()));
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
   * @throws MessageNotFoundException
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public int create(
          Comment comment,
          String commentBody,
          String remoteAddress,
          String xForwardedFor,
          String userAgent) throws MessageNotFoundException {

    int commentId = commentDao.saveNewMessage(comment, commentBody, userAgent);

    /* кастование пользователей */
    Set<User> userRefs = lorCodeService.getReplierFromMessage(commentBody);
    userEventService.addUserRefEvent(userRefs, comment.getTopicId(), commentId);

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
    String xForwardedFor
  ) {
    commentDao.edit(oldComment, newComment, commentBody);

    /* кастование пользователей */
    Set<User> newUserRefs = lorCodeService.getReplierFromMessage(commentBody);

    MessageText messageText = msgbaseDao.getMessageText(oldComment.getId());
    Set<User> oldUserRefs = lorCodeService.getReplierFromMessage(messageText.getText());
    Set<User> userRefs = new HashSet<>();
    /* кастовать только тех, кто добавился. Существующие ранее не кастуются */
    for (User user : newUserRefs) {
      if (!oldUserRefs.contains(user)) {
        userRefs.add(user);
      }
    }

    userEventService.addUserRefEvent(userRefs, oldComment.getTopicId(), oldComment.getId());

    /* Обновление времени последнего изменения топика для того, чтобы данные в кеше автоматически обновились  */
    topicDao.updateLastModifiedToCurrentTime(oldComment.getTopicId());

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
  public void addEditHistoryItem(User editor, Comment original, String originalMessageText, Comment comment, String messageText) {

    EditHistoryDto editHistoryDto = new EditHistoryDto();
    editHistoryDto.setMsgid(original.getId());
    editHistoryDto.setObjectType(EditHistoryObjectTypeEnum.COMMENT);
    editHistoryDto.setEditor(editor.getId());

    boolean modified = false;
    if (!original.getTitle().equals(comment.getTitle())) {
      editHistoryDto.setOldtitle(original.getTitle());
      modified = true;
    }

    if (!originalMessageText.equals(messageText)) {
      editHistoryDto.setOldmessage(originalMessageText);
      modified = true;
    }

    if (modified) {
      editHistoryService.insert(editHistoryDto);
    }

  }

  /**
   * Обновление информации о последнем изменении коммента.
   *
   * @param editor   пользователь, изменивший комментарий
   * @param original оригинал (старый комментарий)
   * @param comment  изменённый комментарий
   */
  public void updateLatestEditorInfo(User editor, Comment original, Comment comment) {
    List<EditHistoryDto> editHistoryDtoList = editHistoryService.getEditInfo(original.getId(), EditHistoryObjectTypeEnum.COMMENT);

    commentDao.updateLatestEditorInfo(
      original.getId(),
      editor.getId(),
      comment.getPostdate(),
      editHistoryDtoList.size()
    );
  }

  /**
   * Удаляем коментарий, если на комментарий есть ответы - генерируем исключение
   *
   * @param msgid      id удаляемого сообщения
   * @param reason     причина удаления
   * @param user       модератор который удаляет
   * @param scoreBonus кол-во отрезаемого шкворца
   * @throws ScriptErrorException генерируем исключение если на комментарий есть ответы
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public boolean deleteComment(int msgid, String reason, User user, int scoreBonus) throws ScriptErrorException {
    if (commentDao.getReplaysCount(msgid) != 0) {
      throw new ScriptErrorException("Нельзя удалить комментарий с ответами");
    }

    boolean deleted = commentDao.deleteComment(msgid, reason, user, scoreBonus);

    if (deleted) {
      commentDao.updateStatsAfterDelete(msgid, 1);
    }

    return deleted;
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
      return new CommentList(commentDao.getCommentList(topic.getId(), showDeleted), topic.getLastModified().getTime());
    } else {
      CacheProvider mcc = MemCachedSettings.getCache();

      String cacheId = "commentList?msgid=" + topic.getId();

      CommentList commentList = (CommentList) mcc.getFromCache(cacheId);

      if (commentList == null || commentList.getLastmod() != topic.getLastModified().getTime()) {
        commentList = new CommentList(commentDao.getCommentList(topic.getId(), showDeleted), topic.getLastModified().getTime());
        mcc.storeToCache(cacheId, commentList);
      }

      return commentList;
    }
  }

  /**
   * Удаление ответов на комментарии.
   *
   * @param msgid  идентификационнай номер комментария
   * @param user   пользователь, удаляющий комментарий
   * @param scoreBonus  сколько снять скора у автора комментария
   * @return список идентификационных номеров удалённых комментариев
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public List<Integer> deleteWithReplys(int msgid, String reason, User user, int scoreBonus) {
    List<Integer> deleted = commentDao.deleteReplys(msgid, user, scoreBonus > 2);

    boolean deletedMain = commentDao.deleteComment(msgid, reason, user, -scoreBonus);

    if (deletedMain) {
      deleted.add(msgid);
    }

    if (!deleted.isEmpty()) {
      commentDao.updateStatsAfterDelete(msgid, deleted.size());
    }

    return deleted;
  }

  /**
   * Удаление топиков, сообщений по ip и за определнный период времени, те комментарии на которые существуют ответы пропускаем
   *
   * @param ip        ip для которых удаляем сообщения (не проверяется на корректность)
   * @param timeDelta врменной промежуток удаления (не проверяется на корректность)
   * @param moderator экзекутор-можератор
   * @param reason    причина удаления, которая будет вписана для удаляемых топиков
   * @return список id удаленных сообщений
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public DeleteCommentResult deleteCommentsByIPAddress(
    String ip,
    Timestamp timeDelta,
    final User moderator,
    final String reason) {
    return commentDao.deleteCommentsByIPAddress(ip, timeDelta, moderator, reason);
  }

  /**
   * Получить список комментариев пользователя.
   *
   * @param user    объект пользователя
   * @param limit   сколько записей должно быть в ответе
   * @param offset  начиная с какой позиции выдать ответ
   * @return список комментариев пользователя
   */
  public List<CommentDao.CommentsListItem> getUserComments(User user, int limit, int offset) {
    return commentDao.getUserComments(user.getId(), limit, offset);
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
   * Блокировка и массивное удаление всех топиков и комментариев пользователя со всеми ответами на комментарии
   *
   * @param user      пользователь для экзекуции
   * @param moderator экзекутор-модератор
   * @param reason    прична блокировки
   * @return список удаленных комментариев
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public DeleteCommentResult deleteAllCommentsAndBlock(User user, final User moderator, String reason) {
    userDao.block(user, moderator, reason);

    List<Integer> deletedTopicIds = topicService.deleteAllByUser(user, moderator);

    List<Integer> deletedCommentIds = commentDao.deleteAllByUser(user, moderator);

    return new DeleteCommentResult(deletedTopicIds, deletedCommentIds, null);
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

  /**
   * Обработать тект комментария посредством парсеров (LorCode или Tex).
   *
   * @param msg   текст комментария
   * @param mode  режим обработки
   * @return обработанная строка
   */
  private String processMessage(String msg, String mode) {
    if ("ntobr".equals(mode)) {
      return toLorCodeFormatter.format(msg, true);
    } else {
      return toLorCodeTexFormatter.format(msg);
    }
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
