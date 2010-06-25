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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.*;

@Controller
public class DeleteCommentController {
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

      Comment comment = new Comment(db, msgid);

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
      params.put("commentsPrepared", list);
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
    HttpSession session
  ) throws Exception {
    if (bonus < 0 || bonus > 20) {
      throw new BadParameterException("incorrect bonus value");
    }

    if (!Template.isSessionAuthorized(session)) {
      throw new AccessViolationException("Not authorized");
    }

    Connection db = null;

    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      CommentDeleter deleter = new CommentDeleter(db);

      User user = Template.getCurrentUser(db, session);
      user.checkBlocked();
      user.checkAnonymous();

      PreparedStatement pr = db.prepareStatement("SELECT postdate>CURRENT_TIMESTAMP-'1 hour'::interval as perm FROM comments WHERE comments.id=? AND comments.userid=?");
      pr.setInt(1, msgid);
      pr.setInt(2, user.getId());
      ResultSet rs = pr.executeQuery();
      boolean perm = false;

      if (rs.next()) {
        perm = rs.getBoolean("perm");
      }

      boolean selfDel = false;

      if (perm) {
        selfDel = true;
      }

      rs.close();

      if (!perm && user.canModerate()) {
        perm = true;
      }

      if (!perm) {
        PreparedStatement mod = db.prepareStatement("SELECT moderator FROM groups,topics,comments WHERE topics.groupid=groups.id AND comments.id=? AND comments.topic=topics.id");
        mod.setInt(1, msgid);

        rs = mod.executeQuery();
        if (!rs.next()) {
          throw new MessageNotFoundException(msgid);
        }

        if (rs.getInt("moderator") == user.getId()) {
          perm = true; // NULL is ok
        }

        rs.close();
        mod.close();
      }

      if (!perm) {
        user.checkDelete();
      }

      StringBuilder out = new StringBuilder();

      if (!selfDel) {
        out.append(deleter.deleteReplys(msgid, user, bonus > 2));
        out.append(deleter.deleteComment(msgid, reason, user, -bonus));
      } else {
        out.append(deleter.deleteComment(msgid, reason, user, 0));
      }

      deleter.close();

      db.commit();

      Map<String, Object> params = new HashMap<String, Object>();
      params.put("message", "Удалено успешно");
      params.put("bigMessage", out.toString());

      return new ModelAndView("action-done", params);
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }
}