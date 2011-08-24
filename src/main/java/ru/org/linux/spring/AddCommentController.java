/*
 * Copyright 1998-2010 Linux.org.ru
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

package ru.org.linux.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.site.*;
import ru.org.linux.spring.validators.AddCommentRequestValidator;
import ru.org.linux.util.HTMLFormatter;
import ru.org.linux.util.ServletParameterException;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.beans.PropertyEditorSupport;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class AddCommentController extends ApplicationObjectSupport {
  private SearchQueueSender searchQueueSender;
  private CaptchaService captcha;
  private DupeProtector dupeProtector;

  @Autowired
  public void setSearchQueueSender(SearchQueueSender searchQueueSender) {
    this.searchQueueSender = searchQueueSender;
  }

  @Autowired
  public void setCaptcha(CaptchaService captcha) {
    this.captcha = captcha;
  }

  @Autowired
  public void setDupeProtector(DupeProtector dupeProtector) {
    this.dupeProtector = dupeProtector;
  }

  @RequestMapping(value = "/add_comment.jsp", method = RequestMethod.GET)
  public ModelAndView showFormReply(
    @ModelAttribute("add") @Valid AddCommentRequest add,
    BindingResult errors,
    ServletRequest request
  ) throws Exception {
    if (errors.hasErrors()) {
      throw new BindException(errors);
    }

    if (add.getTopic()==null) {
      throw new ServletParameterException("тема на задана");
    }

    Template tmpl = Template.getTemplate(request);
    
    Map<String, Object> params = new HashMap<String, Object>();

    if (add.getMode()==null) {
      add.setMode(tmpl.getFormatMode());
    }

    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      checkAndCreateReplyto(add, errors, params, db);

      if (errors.hasErrors()) {
        throw new BindException(errors);
      }

      return new ModelAndView("add_comment", params);
    } finally {
      JdbcUtils.closeConnection(db);
    }
  }

  @RequestMapping("/comment-message.jsp")
  public ModelAndView showFormTopic(
    @ModelAttribute("add") @Valid AddCommentRequest add,
    HttpServletRequest request
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      if (add.getMode()==null) {
        add.setMode(tmpl.getFormatMode());
      }
      
      HashMap<String, Object> params = new HashMap<String, Object>();

      params.put("preparedMessage", new PreparedMessage(db, add.getTopic(), true));

      return new ModelAndView("comment-message", params);
    } finally {
      JdbcUtils.closeConnection(db);
    }
  }

  private static String processMessage(String msg, String mode) {
    if ("lorcode".equals(mode)) {
      return msg;
    }

    HTMLFormatter form = new HTMLFormatter(msg);
    form.setMaxLength(80);
    form.enableUrlHighLightMode();
    form.setOutputLorcode(true);

    if ("ntobr".equals(mode)) {
      form.enableNewLineMode();
      form.enableQuoting();
    }
    if ("quot".equals(mode)) {
      form.enableTexNewLineMode();
      form.enableQuoting();
    }

    return form.process();
  }

  @RequestMapping(value = "/add_comment.jsp", method = RequestMethod.POST)
  public ModelAndView addComment(
    @ModelAttribute("add") @Valid AddCommentRequest add,
    Errors errors,
    HttpServletRequest request
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    String msg = processMessage(add.getMsg(), add.getMode());

    if (add.getMsg()==null || add.getMsg().trim().isEmpty()) {
      errors.rejectValue("msg", null, "комментарий не может быть пустым");
    }

    if (add.getMode()==null) {
      add.setMode(tmpl.getFormatMode());
    }

    HttpSession session = request.getSession();

    if (!add.isPreviewMode() && !Template.isSessionAuthorized(session)) {
      captcha.checkCaptcha(request, errors);
    }

    if (!add.isPreviewMode() && !errors.hasErrors() && !session.getId().equals(request.getParameter("session"))) {
      logger.info("Flood protection (session variable differs: " + session.getId() + ") " + request.getRemoteAddr());
      errors.reject(null, "сбой добавления");
    }

    Connection db = null;

    try {
      // prechecks is over
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);
      IPBlockInfo.checkBlockIP(db, request.getRemoteAddr());

      tmpl.updateCurrentUser(db);

      Map<String, Object> formParams = new HashMap<String, Object>();

      checkAndCreateReplyto(add, errors, formParams, db);

      User user;

      if (!Template.isSessionAuthorized(session)) {
        if (request.getParameter("nick") == null) {
          throw new AccessViolationException("Вы уже вышли из системы");
        }
        try {
          user = User.getUser(db, request.getParameter("nick"));
          user.checkPassword(request.getParameter("password"));
        } catch (UserNotFoundException ex) {
          errors.reject(null, "Пользователь не найден");
          user = User.getAnonymous(db);
        } catch (BadPasswordException ex) {
          errors.reject(null, ex.getMessage());
          user = User.getAnonymous(db);
        }
      } else {
        user = tmpl.getCurrentUser();
      }

      user.checkBlocked(errors);

      Comment comment = new Comment(
              add.getReplyto(),
              HTMLFormatter.htmlSpecialChars(add.getTitle()),
              add.getTopic().getId(),
              user.getId(),
              request.getHeader("user-agent"),
              request.getRemoteAddr()
      );

      formParams.put("comment", new PreparedComment(db, comment, msg));

      if (user.isAnonymous()) {
        if (msg.length() > 4096) {
          errors.rejectValue("msg", null, "Слишком большое сообщение");
        }
      } else {
        if (msg.length() > 8192) {
          errors.rejectValue("msg", null, "Слишком большое сообщение");
        }
      }

      add.getTopic().checkCommentsAllowed(user, errors);

      if (!add.isPreviewMode() && !errors.hasErrors()) {
        dupeProtector.checkDuplication(request.getRemoteAddr(), user.getScore() > 100, errors);
      }

      if (!add.isPreviewMode() && !errors.hasErrors()) {
        int msgid = comment.saveNewMessage(db, request.getRemoteAddr(), request.getHeader("user-agent"), msg);

        String logmessage = "Написан комментарий " + msgid + " ip:" + request.getRemoteAddr();
        if (request.getHeader("X-Forwarded-For") != null) {
          logmessage = logmessage + " XFF:" + request.getHeader(("X-Forwarded-For"));
        }

        logger.info(logmessage);

        db.commit();

        searchQueueSender.updateComment(msgid);

        String returnUrl = "jump-message.jsp?msgid=" + add.getTopic().getId() + "&cid=" + msgid;

        return new ModelAndView(new RedirectView(returnUrl));
      }

      return new ModelAndView("add_comment", formParams);
    } finally {
      JdbcUtils.closeConnection(db);
    }
  }

  private static void checkAndCreateReplyto(AddCommentRequest add, Errors errors, Map<String, Object> formParams, Connection db) throws SQLException, MessageNotFoundException, UserNotFoundException {
    if (add.getReplyto()!=null && add.getReplyto() > 0) {
      Comment onComment = new Comment(db, add.getReplyto());

      if (onComment.isDeleted()) {
        errors.reject(null, "нельзя комментировать удаленные комментарии");
      }

      if (onComment.getTopic() != add.getTopic().getId()) {
        errors.reject(null, "Некорректная тема?!");
      }

      formParams.put("onComment", PreparedComment.prepare(db, null, onComment));
    }
  }

  @InitBinder("add")
  public void requestValidator(WebDataBinder binder) {
    binder.setValidator(new AddCommentRequestValidator());

    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor());
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(Message.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String text) throws IllegalArgumentException {
        Connection db=null;

        try {
          db = LorDataSource.getConnection();

          setValue(new Message(db, Integer.parseInt(text)));
        } catch (SQLException e) {
          throw new RuntimeException(e);
        } catch (MessageNotFoundException e) {
          throw new IllegalArgumentException(e);
        } finally {
          JdbcUtils.closeConnection(db);
        }
      }
    });
  }

  @ExceptionHandler(BindException.class)
  public String handleInvalidRequest() {
    return "error-parameter"; 
  }
}
