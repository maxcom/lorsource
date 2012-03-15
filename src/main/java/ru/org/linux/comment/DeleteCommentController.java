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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.ApplicationController;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.site.Template;
import ru.org.linux.search.SearchQueueSender;
import ru.org.linux.site.*;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.user.UserDao;
import ru.org.linux.topic.Topic;
import ru.org.linux.user.User;
import ru.org.linux.user.UserErrorException;
import ru.org.linux.user.UserNotFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Controller
public class DeleteCommentController extends ApplicationController {
  @Autowired
  private SearchQueueSender searchQueueSender;

  @Autowired
  private CommentDao commentDao;

  @Autowired
  private TopicDao messageDao;

  @Autowired
  private UserDao userDao;

  @Autowired
  private CommentPrepareService prepareService;

  private static final int DELETE_PERIOD = 60 * 60 * 1000; // milliseconds

  @RequestMapping(value = "/delete_comment.jsp", method = RequestMethod.GET)
  public ModelAndView showForm(
    HttpSession session,
    HttpServletRequest request,
    @RequestParam("msgid") int msgid
  ) throws Exception {
    ModelAndView modelAndView = new ModelAndView("delete_comment");


    if (!Template.isSessionAuthorized(session)) {
      throw new AccessViolationException("нет авторизации");
    }

    Template tmpl = Template.getTemplate(request);

    modelAndView.addObject("msgid", msgid);

    Comment comment = commentDao.getById(msgid);

    if (comment.isDeleted()) {
      throw new UserErrorException("комментарий уже удален");
    }

    int topicId = comment.getTopicId();

    Topic topic = messageDao.getById(topicId);

    if (topic.isDeleted()) {
      throw new AccessViolationException("тема удалена");
    }

    modelAndView.addObject("topic", topic);

    CommentList comments = commentDao.getCommentList(topic, tmpl.isModeratorSession());

    CommentFilter cv = new CommentFilter(comments);

    List<Comment> list = cv.getCommentsSubtree(msgid);

    modelAndView.addObject("commentsPrepared", prepareService.prepareCommentList(comments, list, request.isSecure()));
    modelAndView.addObject("comments", comments);

    return render(modelAndView);
  }

  @RequestMapping(value = "/delete_comment.jsp", method = RequestMethod.POST)
  public ModelAndView deleteComments(
    @RequestParam("msgid") int msgid,
    @RequestParam("reason") String reason,
    @RequestParam(value="bonus", defaultValue="0") int bonus,
    HttpSession session,
    HttpServletRequest request
  ) throws Exception {
    if (bonus < 0 || bonus > 20) {
      throw new BadParameterException("неправильный размер штрафа");
    }

    if (!Template.isSessionAuthorized(session)) {
      throw new AccessViolationException("нет авторизации");
    }

    Template tmpl = Template.getTemplate(request);

    tmpl.updateCurrentUser(userDao);

    User user = tmpl.getCurrentUser();
    user.checkBlocked();
    user.checkAnonymous();

    Comment comment = commentDao.getById(msgid);
    Topic topic = messageDao.getById(comment.getTopicId());

    if (comment.isDeleted()) {
      throw new UserErrorException("комментарий уже удален");
    }

    boolean perm = false;
    boolean selfDel = false;

    if (comment.getUserid() == user.getId()) {
      if(!user.isModerator()) {
        perm = (System.currentTimeMillis() - comment.getPostdate().getTime()) < DELETE_PERIOD;
        if (perm) {
          selfDel = true;
        }
      } else {
        bonus = 0;
      }
    }

    if (!perm && user.isModerator()) {
      perm = true;
    }

    if (!perm) {
      user.checkDelete();
    }

    StringBuilder out = new StringBuilder();

    LinkedList<Integer> deleted = new LinkedList<Integer>();
    deleted.add(msgid);

    if (!selfDel) {
      List<Integer> deletedReplys = commentDao.deleteReplys(msgid, user, bonus > 2);
      if (!deletedReplys.isEmpty()) {
        out.append("Удаленные ответы: ").append(deletedReplys).append("<br>");
      }

      deleted.addAll(deletedReplys);

      if (commentDao.deleteComment(msgid, reason, user, -bonus)) {
        out.append("Сообщение ").append(msgid).append(" удалено");
      }
    } else {
      if (commentDao.deleteComment(msgid, reason, user, 0)) {
        out.append("Сообщение ").append(msgid).append(" удалено");
      } else {
        out.append("Сообщение ").append(msgid).append(" уже было удалено");
      }
    }

    searchQueueSender.updateComment(deleted);

    ModelAndView modelAndView = new ModelAndView("action-done");
    modelAndView.addObject("message", "Удалено успешно");
    modelAndView.addObject("bigMessage", out.toString());

    modelAndView.addObject("link", topic.getLink());

    return render(modelAndView);
  }

  @ExceptionHandler({ScriptErrorException.class, UserErrorException.class, AccessViolationException.class})
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ModelAndView handleUserNotFound(Exception ex, HttpServletRequest request, HttpServletResponse response) {
    ModelAndView modelAndView = new ModelAndView("error-good-penguin");
    modelAndView.addObject("msgTitle", "Ошибка: " + ex.getMessage());
    modelAndView.addObject("msgHeader", ex.getMessage());
    modelAndView.addObject("msgMessage", "");
    return render(modelAndView);
  }

}
