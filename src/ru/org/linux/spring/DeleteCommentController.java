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
  public ModelAndView showForm(@RequestParam("msgid") int msgid) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();

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
    @RequestParam(value="nick", required=false) String nick,
    @RequestParam("reason") String reason,
    @RequestParam("bonus") int bonus,
    HttpServletRequest request
  ) throws Exception {
    if (bonus < 0 || bonus > 20) {
      throw new BadParameterException("incorrect bonus value");
    }

    Connection db = null;
    HttpSession session = request.getSession();

    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      CommentDeleter deleter = new CommentDeleter(db);

      User user;

      if (!Template.isSessionAuthorized(session)) {
        if (nick == null) {
          throw new BadInputException("Вы уже вышли из системы");
        }
        user = User.getUser(db, nick);
        user.checkPassword(request.getParameter("password"));
      } else {
        user = User.getUser(db, (String) session.getAttribute("nick"));
        nick = (String) session.getAttribute("nick");
      }

      user.checkAnonymous();

      PreparedStatement pr = db.prepareStatement("SELECT postdate>CURRENT_TIMESTAMP-'1 hour'::interval as perm FROM users, comments WHERE comments.id=? AND comments.userid=users.id AND users.nick=?");
      pr.setInt(1, msgid);
      pr.setString(2, nick);
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
        out.append(deleter.deleteReplys(msgid, user, bonus != 0));
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
