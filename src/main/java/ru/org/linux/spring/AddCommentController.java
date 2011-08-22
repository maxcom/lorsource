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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import ru.org.linux.site.*;
import ru.org.linux.util.HTMLFormatter;

import org.jdom.Verifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class AddCommentController extends ApplicationObjectSupport {
  private SearchQueueSender searchQueueSender;

  @Autowired
  @Required
  public void setSearchQueueSender(SearchQueueSender searchQueueSender) {
    this.searchQueueSender = searchQueueSender;
  }

  @RequestMapping(value = "/add_comment.jsp", method = RequestMethod.GET)
  public ModelAndView showForm(
    @RequestParam("topic") int topicId,
    @RequestParam(value = "replyto", required = false) Integer replyTo,
    ServletRequest request
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    Map<String, Object> params = new HashMap<String, Object>();
    params.put("topic", topicId);
    params.put("mode", tmpl.getFormatMode());

    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      Message topic = new Message(db, topicId);
      checkTopic(topic);

      params.put("postscore", topic.getPostScore());

      createReplyTo(topic, replyTo, params, db);

      return new ModelAndView("add_comment", params);
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  private static void checkTopic(Message topic) throws AccessViolationException {
    if (topic.isExpired()) {
      throw new AccessViolationException("нельзя добавлять в устаревшие темы");
    }

    if (topic.isDeleted()) {
      throw new AccessViolationException("нельзя добавлять в удаленные темы");
    }
  }

  private static void createReplyTo(Message topic, Integer replyTo, Map<String, Object> params, Connection db) throws SQLException, MessageNotFoundException, AccessViolationException, UserNotFoundException {
    if (replyTo != null && replyTo>0) {
      Comment onComment = new Comment(db, replyTo);
      if (onComment.isDeleted()) {
        throw new AccessViolationException("нельзя комментировать удаленные комментарии");
      }
      if (onComment.getTopic() != topic.getId()) {
        throw new AccessViolationException("Некорректная тема?!");
      }
      params.put("onComment", PreparedComment.prepare(db, null, onComment));
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
    @ModelAttribute("add") AddCommentRequest add,
    Errors errors,
    HttpServletRequest request
  ) throws Exception {
    Map<String, Object> formParams = new HashMap<String, Object>();

    HttpSession session = request.getSession();

    Connection db = null;

    Template tmpl = Template.getTemplate(request);

    try {
      String title = HTMLFormatter.htmlSpecialChars(add.getTitle());

      String msg = processMessage(add.getMsg(), add.getMode());

      // prechecks is over
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);
      tmpl.updateCurrentUser(db);

      Message topic = new Message(db, add.getTopic());
      formParams.put("postscore", topic.getPostScore());

      createReplyTo(topic, add.getReplyto(), formParams, db);

      checkTopic(topic);

      if (!add.isPreviewMode() && !session.getId().equals(request.getParameter("session"))) {
        logger.info("Flood protection (session variable differs: " + session.getId() + ") " + request.getRemoteAddr());
        errors.reject(null, "сбой добавления");
      }

      IPBlockInfo.checkBlockIP(db, request.getRemoteAddr());

      User user;

      if (!Template.isSessionAuthorized(session)) {
        if (request.getParameter("nick") == null) {
          throw new BadInputException("Вы уже вышли из системы");
        }
        user = User.getUser(db, request.getParameter("nick"));
        user.checkPassword(request.getParameter("password"));
      } else {
        user = tmpl.getCurrentUser();
      }

      user.checkBlocked();

      int replyto = 0;

      Comment comment = new Comment(replyto, title, add.getTopic(), 0, request.getHeader("user-agent"), request.getRemoteAddr());

      comment.setAuthor(user.getId());

      formParams.put("comment", new PreparedComment(db, comment, msg));

      if (title.length() > Comment.TITLE_LENGTH) {
        errors.rejectValue("title", null, "заголовок превышает " + Comment.TITLE_LENGTH + " символов");
      }

      if ("".equals(msg)) {
        errors.rejectValue("msg", null, "комментарий не может быть пустым");
      }

      String error = Verifier.checkCharacterData(msg);
      if (error!=null) {
        errors.rejectValue("msg", null, error);
      }

      if (!add.isPreviewMode() && !Template.isSessionAuthorized(session)) {
        CaptchaUtils.checkCaptcha(request);
      }

      if (user.isAnonymous()) {
        if (msg.length() > 4096) {
          errors.rejectValue("msg", "Слишком большое сообщение");
        }
      } else {
        if (msg.length() > 8192) {
          errors.rejectValue("msg", "Слишком большое сообщение");
        }
      }

      topic.checkCommentsAllowed(user);

      if (!add.isPreviewMode() && !errors.hasErrors()) {
        DupeProtector.getInstance().checkDuplication(request.getRemoteAddr(), user.getScore() > 100);

        int msgid = comment.saveNewMessage(db, request.getRemoteAddr(), request.getHeader("user-agent"), msg);

        String logmessage = "Написан комментарий " + msgid + " ip:" + request.getRemoteAddr();
        if (request.getHeader("X-Forwarded-For") != null) {
          logmessage = logmessage + " XFF:" + request.getHeader(("X-Forwarded-For"));
        }

        logger.info(logmessage);

        db.commit();

        searchQueueSender.updateComment(msgid);

        String returnUrl = "jump-message.jsp?msgid=" + add.getTopic() + "&cid=" + msgid;

        return new ModelAndView(new RedirectView(returnUrl));
      }
    } catch (UserErrorException e) {
      formParams.put("error", e);
      if (db != null) {
        db.rollback();
        db.setAutoCommit(true);
      }
    } catch (UserNotFoundException e) {
      formParams.put("error", e);
      if (db != null) {
        db.rollback();
        db.setAutoCommit(true);
      }
    } finally {
      if (db != null) {
        db.close();
      }
    }

    return new ModelAndView("add_comment", formParams);
  }
}
