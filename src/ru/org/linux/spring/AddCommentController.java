/*
 * Copyright 1998-2009 Linux.org.ru
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
import java.util.Random;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ru.org.linux.site.*;
import ru.org.linux.util.BadURLException;
import ru.org.linux.util.HTMLFormatter;

@Controller
public class AddCommentController extends ApplicationObjectSupport {
  @RequestMapping(value = "/add_comment.jsp", method = RequestMethod.GET)
  public ModelAndView showForm(
    @RequestParam("topic") int topicId,
    @RequestParam(value = "replyto", required = false) Integer replyTo,
    ServletRequest request
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    Map<String, Object> params = new HashMap<String, Object>();
    params.put("topic", topicId);
    params.put("autourl", true);
    params.put("mode", tmpl.getFormatMode());

    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      Message topic = new Message(db, topicId);
      checkTopic(topic);

      int postscore = topic.getPostScore();
      params.put("postscore", postscore);

      createReplyTo(replyTo, params, db);

      return new ModelAndView("add_comment", params);
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  private void checkTopic(Message topic) throws AccessViolationException {
    if (topic.isExpired()) {
      throw new AccessViolationException("нельзя добавлять в устаревшие темы");
    }

    if (topic.isDeleted()) {
      throw new AccessViolationException("нельзя добавлять в удаленные темы");
    }
  }

  private void createReplyTo(Integer replyTo, Map<String, Object> params, Connection db) throws SQLException, MessageNotFoundException {
    if (replyTo != null) {
      Comment onComment = new Comment(db, replyTo);
      params.put("onComment", onComment);
      if (onComment.isDeleted()) {
        throw new MessageNotFoundException(replyTo);
      }
    }
  }

  @RequestMapping(value = "/add_comment.jsp", method = RequestMethod.POST)
  public ModelAndView addComment(
    @RequestParam(value = "preview", required = false) String previewStr,
    @RequestParam("mode") String mode,
    @RequestParam("autourl") boolean autourl,
    @RequestParam("msg") String msg,
    @RequestParam(value = "replyto", required = false) Integer replyToObject,
    @RequestParam("title") String title,
    @RequestParam("topic") int topicId,
    HttpServletRequest request
  ) throws Exception {
    boolean preview = previewStr != null;
    Template tmpl = Template.getTemplate(request);
    Map<String, Object> formParams = new HashMap<String, Object>();

    formParams.put("topic", topicId);
    formParams.put("autourl", autourl);
    formParams.put("mode", mode);    

    int replyto = 0;

    if (replyToObject != null) {
      replyto = replyToObject;
    }

    HttpSession session = request.getSession();

    Connection db = null;

    try {
      if (title == null) {
        title = "";
      }

      title = HTMLFormatter.htmlSpecialChars(title);

      int maxlength = 80; // TODO: remove this hack
      HTMLFormatter form = new HTMLFormatter(msg);
      form.setMaxLength(maxlength);
      if ("pre".equals(mode)) {
        form.enablePreformatMode();
      }
      if (autourl) {
        form.enableUrlHighLightMode();
      }
      if ("ntobrq".equals(mode)) {
        form.enableNewLineMode();
        form.enableQuoting();
      }
      if ("ntobr".equals(mode)) {
        form.enableNewLineMode();
      }
      if ("tex".equals(mode)) {
        form.enableTexNewLineMode();
      }
      if ("quot".equals(mode)) {
        form.enableTexNewLineMode();
        form.enableQuoting();
      }

      msg = form.process();

      // prechecks is over
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      Message topic = new Message(db, topicId);
      checkTopic(topic);
      formParams.put("postscore", topic.getPostScore());

      createReplyTo(replyToObject, formParams, db);

      if (!preview && !session.getId().equals(request.getParameter("session"))) {
        logger.info("Flood protection (session variable differs: " + session.getId() + ") " + request.getRemoteAddr());
        throw new BadInputException("сбой добавления");
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
        user = User.getUser(db, (String) session.getAttribute("nick"));
      }

      user.checkBlocked();

      Comment comment = new Comment(replyto, title, msg, topicId, 0, request.getHeader("user-agent"), request.getRemoteAddr());

      comment.setAuthor(user.getId());

      formParams.put("comment", comment);

      if ("".equals(title)) {
        throw new BadInputException("заголовок сообщения не может быть пустым");
      }

      if (title.length() > Comment.TITLE_LENGTH) {
        throw new BadInputException("заголовок превышает " + Comment.TITLE_LENGTH + " символов");
      }

      if ("".equals(msg)) {
        throw new BadInputException("комментарий не может быть пустым");
      }

      if (!preview && !Template.isSessionAuthorized(session)) {
        CaptchaSingleton.checkCaptcha(session, request);
      }

      if (user.isAnonymous()) {
        if (msg.length() > 4096) {
          throw new BadInputException("Слишком большое сообщение");
        }
      } else {
        if (msg.length() > 8192) {
          throw new BadInputException("Слишком большое сообщение");
        }
      }

      if (replyto != 0) {
        Comment reply = new Comment(db, replyto);
        if (reply.isDeleted()) {
          throw new AccessViolationException("Комментарий был удален");
        }

        if (reply.getTopic() != topicId) {
          throw new AccessViolationException("Некорректная тема?!");
        }
      }

      topic.checkCommentsAllowed(db, user);

      if (!preview) {
        DupeProtector.getInstance().checkDuplication(request.getRemoteAddr(), user.getScore() > 100);

        int msgid = comment.saveNewMessage(db, request.getRemoteAddr(), request.getHeader("user-agent"));

        String logmessage = "Написан комментарий " + msgid + " ip:" + request.getRemoteAddr();
        if (request.getHeader("X-Forwarded-For") != null) {
          logmessage = logmessage + " XFF:" + request.getHeader(("X-Forwarded-For"));
        }

        logger.info(logmessage);

        // update lastmod in topic and generate new commentList
        topic = new Message(db, topicId);
        CommentList commentList = CommentList.getCommentList(db, topic, false);
        Comment newComment = commentList.getNode(msgid).getComment();
        int pageNum = commentList.getCommentPage(newComment, tmpl);

        Random random = new Random();

        String returnUrl;

        if (pageNum > 0) {
          returnUrl = "view-message.jsp?msgid=" + topicId + "&page=" + pageNum + "&nocache=" + random.nextInt() + "#comment-" + msgid;
        } else {
          returnUrl = "view-message.jsp?msgid=" + topicId + "&nocache=" + random.nextInt() + "#comment-" + msgid;
        }

        db.commit();

        return new ModelAndView(new RedirectView(tmpl.getMainUrl() + returnUrl));
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
    } catch (BadURLException e) {
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
