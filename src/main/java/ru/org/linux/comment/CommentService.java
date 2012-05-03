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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ApplicationObjectSupport;
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
import ru.org.linux.search.SearchQueueSender;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.site.Template;
import ru.org.linux.spring.Configuration;
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.user.IgnoreListDao;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserEventService;
import ru.org.linux.user.UserNotFoundException;
import ru.org.linux.user.UserPropertyEditor;
import ru.org.linux.util.ExceptionBindingErrorProcessor;
import ru.org.linux.util.ServletParameterException;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.bbcode.LorCodeService;
import ru.org.linux.util.formatter.ToLorCodeFormatter;
import ru.org.linux.util.formatter.ToLorCodeTexFormatter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.beans.PropertyEditorSupport;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashSet;
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
  private IPBlockDao ipBlockDao;

  @Autowired
  private LorCodeService lorCodeService;

  @Autowired
  private UserEventService userEventService;

  @Autowired
  private MsgbaseDao msgbaseDao;

  @Autowired
  private IgnoreListDao ignoreListDao;

  /**
   * @param binder
   */
  public void requestValidator(WebDataBinder binder) {
    binder.setValidator(new CommentRequestValidator());
    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor());
  }

  /**
   * @param binder
   */
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
   * @param commentRequest
   * @param user
   * @param ipBlockInfo
   * @param request
   * @param errors
   * @throws UserNotFoundException
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
    UserNotFoundException,
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

    HttpSession session = request.getSession();

    if (!commentRequest.isPreviewMode() &&
      (!tmpl.isSessionAuthorized() || ipBlockInfo.isCaptchaRequired())) {
      captcha.checkCaptcha(request, errors);
    }

    if (!commentRequest.isPreviewMode() && !errors.hasErrors() && !session.getId().equals(request.getParameter("session"))) {
      logger.info(String.format(
        "Flood protection (session variable differs: session=%s var=%s) ip=%s",
        session.getId(),
        request.getParameter("session"),
        request.getRemoteAddr()
      ));

      errors.reject(null, "сбой добавления, попробуйте еще раз");
    }

    user.checkBlocked(errors);

    if (ipBlockInfo.isBlocked()) {
      ipBlockDao.checkBlockIP(ipBlockInfo, errors, user);
    }

    if (!commentRequest.isPreviewMode() && !errors.hasErrors()) {
      floodProtector.checkDuplication(request.getRemoteAddr(), user.getScore() > 100, errors);
    }
  }

  /**
   * @param commentRequest
   * @param user
   * @param errors
   * @return
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
   * @param commentRequest
   * @param user
   * @param request
   * @return
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
        request.getHeader("user-agent"),
        request.getRemoteAddr(),
        commentDao.getReplaysCount(commentId) > 0
      );
    }
    return comment;
  }

  /**
   * @param commentRequest
   * @param request
   * @param errors
   * @return
   */
  public User getCommentUser(
    CommentRequest commentRequest,
    HttpServletRequest request,
    Errors errors
  ) {
    User user;

    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      if (commentRequest.getNick() != null) {
        user = commentRequest.getNick();
      } else {
        user = userDao.getAnonymous();
      }

      if (commentRequest.getPassword() == null) {
        errors.reject(null, "Требуется авторизация");
      }
    } else {
      user = tmpl.getCurrentUser();
    }
    return user;
  }

  /**
   * @param add
   * @param formParams
   * @param request
   * @throws UserNotFoundException
   */
  public void prepareReplyto(
    CommentRequest add,
    Map<String, Object> formParams,
    HttpServletRequest request
  ) throws UserNotFoundException {
    if (add.getReplyto() != null) {
      formParams.put("onComment", commentPrepareService.prepareComment(add.getReplyto(), request.isSecure()));
    }
  }

  /**
   * Создание нового комментария.
   *
   * @param comment
   * @param commentBody
   * @param remoteAddress
   * @param xForwardedFor
   * @return
   * @throws MessageNotFoundException
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public int create(
    Comment comment,
    String commentBody,
    String remoteAddress,
    String xForwardedFor
  ) throws MessageNotFoundException {

    int commentId = commentDao.saveNewMessage(comment, commentBody);

    /* кастование пользователей */
    Set<User> userRefs = lorCodeService.getReplierFromMessage(commentBody);
    userEventService.addUserRefEvent(userRefs.toArray(new User[userRefs.size()]), comment.getTopicId(), commentId);

    /* оповещение об ответе на коммент */
    if (comment.getReplyTo() != 0) {
      try {
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
      } catch (UserNotFoundException e) {
        throw new RuntimeException(e);
      }
    }

    String logMessage = makeLogString("Написан комментарий " + commentId, remoteAddress, xForwardedFor);
    logger.info(logMessage);

    return commentId;
  }

  /**
   * Редактирование комментария.
   *
   * @param oldComment
   * @param newComment
   * @param commentBody
   * @param remoteAddress
   * @param xForwardedFor
   * @throws ServletParameterException
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void edit(
    Comment oldComment,
    Comment newComment,
    String commentBody,
    String remoteAddress,
    String xForwardedFor
  )
    throws ServletParameterException {

    commentDao.edit(oldComment, newComment, commentBody);

    /* кастование пользователей */
    Set<User> newUserRefs = lorCodeService.getReplierFromMessage(commentBody);

    MessageText messageText = msgbaseDao.getMessageText(oldComment.getId());
    Set<User> oldUserRefs = lorCodeService.getReplierFromMessage(messageText.getText());
    Set<User> userRefs = new HashSet<User>();
    /* кастовать только тех, кто добавился. Существующие ранее не кастуются */
    for (User user : newUserRefs) {
      if (!oldUserRefs.contains(user)) {
        userRefs.add(user);
      }
    }

    userEventService.addUserRefEvent(userRefs.toArray(new User[userRefs.size()]), oldComment.getTopicId(), oldComment.getId());

    String logMessage = makeLogString("Изменён комментарий " + oldComment.getId(), remoteAddress, xForwardedFor);
    logger.info(logMessage);

  }

  /**
   * Проверка, имет ли указанный комментарий ответы.
   *
   * @param comment  объект комментария
   * @return true если есть ответы, иначе false
   */
  public boolean isHaveAnswers(Comment comment) {
    return commentDao.isHaveAnswers(comment.getId());
  }

  /**
   * Формирование строки в лог-файл.
   *
   * @param message        сообщение
   * @param remoteAddress  IP-адрес, с которого был добавлен комментарий
   * @param xForwardedFor  IP-адрес через шлюз, с которого был добавлен комментарий
   * @return строка, готовая для добавления в лог-файл
   */
  private String makeLogString(String message, String remoteAddress, String xForwardedFor) {
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

  private String processMessage(String msg, String mode) {
    if ("ntobr".equals(mode)) {
      return toLorCodeFormatter.format(msg, true);
    } else {
      return toLorCodeTexFormatter.format(msg);
    }
  }

}
