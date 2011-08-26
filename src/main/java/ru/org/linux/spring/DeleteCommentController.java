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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import ru.org.linux.site.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class DeleteCommentController {
  private SearchQueueSender searchQueueSender;
  private static final int DELETE_PERIOD = 60 * 60 * 1000; // milliseconds

  @Autowired
  @Required
  public void setSearchQueueSender(SearchQueueSender searchQueueSender) {
    this.searchQueueSender = searchQueueSender;
  }

  @RequestMapping(value = "/delete_comment.jsp", method = RequestMethod.GET)
  public ModelAndView showForm(
    HttpSession session,
    HttpServletRequest request,
    @RequestParam("msgid") int msgid
  ) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();

    if (!Template.isSessionAuthorized(session)) {
      throw new AccessViolationException("Not authorized");
    }

    Template tmpl = Template.getTemplate(request);

    params.put("msgid", msgid);

    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      Comment comment = Comment.getComment(db, msgid);

      if (comment.isDeleted()) {
        throw new AccessViolationException("комментарий уже удален");
      }

      int topicId = comment.getTopic();

      Message topic = new Message(db, topicId);

      if (topic.isDeleted()) {
        throw new AccessViolationException("тема удалена");
      }

      params.put("topic", topic);

      CommentList comments = CommentList.getCommentList(db, topic, tmpl.isModeratorSession());

      CommentFilter cv = new CommentFilter(comments);

      List<Comment> list = cv.getCommentsSubtree(msgid);

      params.put("commentsPrepared", PreparedComment.prepare(db, comments, list));
      params.put("comments", comments);

      return new ModelAndView("delete_comment", params);
    } finally {
      if (db!=null) {
        db.close();
      }
    }
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
      throw new BadParameterException("incorrect bonus value");
    }

    if (!Template.isSessionAuthorized(session)) {
      throw new AccessViolationException("Not authorized");
    }

    Template tmpl = Template.getTemplate(request);

    Connection db = null;

    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);
      tmpl.updateCurrentUser(db);

      CommentDeleter deleter = new CommentDeleter(db);

      User user = tmpl.getCurrentUser();
      user.checkBlocked();
      user.checkAnonymous();

      Comment comment = Comment.getComment(db, msgid);
      Message topic = new Message(db, comment.getTopic());

      boolean perm = false;
      boolean selfDel = false;

      if (comment.getUserid() == user.getId() && !user.canModerate()) {
        perm = (System.currentTimeMillis() - comment.getPostdate().getTime()) < DELETE_PERIOD;

        if (perm) {
          selfDel = true;
        }
      }

      if (!perm && user.canModerate()) {
        perm = true;
      }

      if (!perm) {
        user.checkDelete();
      }

      StringBuilder out = new StringBuilder();

      LinkedList<Integer> deleted = new LinkedList<Integer>();
      deleted.add(msgid);

      if (!selfDel) {
        List<Integer> deletedReplys = deleter.deleteReplys(msgid, user, bonus > 2);
        if (!deletedReplys.isEmpty()) {
          out.append("Удаленные ответы: ").append(deletedReplys).append("<br>");
        }

        deleted.addAll(deletedReplys);

        deleter.deleteComment(msgid, reason, user, -bonus);
      } else {
        deleter.deleteComment(msgid, reason, user, 0);
      }

      out.append("Сообщение ").append(msgid).append(" удалено");

      deleter.close();

      db.commit();

      searchQueueSender.updateComment(deleted);

      Map<String, Object> params = new HashMap<String, Object>();
      params.put("message", "Удалено успешно");
      params.put("bigMessage", out.toString());

      params.put("link", topic.getLink());

      return new ModelAndView("action-done", params);
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }
}