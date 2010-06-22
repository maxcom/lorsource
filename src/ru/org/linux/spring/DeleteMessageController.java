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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.*;

@Controller
public class DeleteMessageController extends ApplicationObjectSupport {
  @RequestMapping(value="/delete.jsp", method= RequestMethod.GET)
  public ModelAndView showForm(
    @RequestParam("msgid") int msgid
  ) throws Exception {
    Connection db = null;
    try {
      db = LorDataSource.getConnection();
      
      Message msg = new Message(db, msgid);

      if (msg.isDeleted()) {
        throw new UserErrorException("Сообщение уже удалено");
      }

      HashMap<String, Object> params = new HashMap<String, Object>();
      params.put("bonus", !msg.getSection().isPremoderated());

      params.put("msgid", msgid);

      return new ModelAndView("delete", params);
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  @RequestMapping(value="/delete.jsp", method= RequestMethod.POST)
  public ModelAndView deleteMessage(
    @RequestParam("msgid") int msgid,
    @RequestParam(value="nick", required = false) String nick,
    @RequestParam("reason") String reason,
    @RequestParam(value="bonus", required = false) Integer bonus,
    HttpServletRequest request
  ) throws Exception {
    HttpSession session = request.getSession();
    Connection db = null;
    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      PreparedStatement lock = db.prepareStatement("SELECT deleted FROM topics WHERE id=? FOR UPDATE");
      PreparedStatement st1 = db.prepareStatement("UPDATE topics SET deleted='t',sticky='f' WHERE id=?");
      PreparedStatement st2 = db.prepareStatement("INSERT INTO del_info (msgid, delby, reason, deldate) values(?,?,?, CURRENT_TIMESTAMP)");
      lock.setInt(1, msgid);
      st1.setInt(1, msgid);
      st2.setInt(1, msgid);

      User user;

      if (!Template.isSessionAuthorized(session)) {
        if (nick == null) {
          throw new BadInputException("Вы уже вышли из системы");
        }
        user = User.getUser(db, nick);
        user.checkPassword(request.getParameter("password"));
      } else {
        user = User.getCurrentUser(db, session);
        nick = user.getNick();
      }

      user.checkAnonymous();
      st2.setInt(2, user.getId());

      ResultSet lockResult = lock.executeQuery(); // lock another delete.jsp on this row

      if (lockResult.next() && lockResult.getBoolean("deleted")) {
        throw new UserErrorException("Сообщение уже удалено");
      }

      Message message = new Message(db, msgid);

      PreparedStatement pr = db.prepareStatement("SELECT postdate>CURRENT_TIMESTAMP-'1 hour'::interval as perm FROM users, topics WHERE topics.id=? AND topics.userid=users.id AND users.nick=?");
      pr.setInt(1, msgid);
      pr.setString(2, nick);
      ResultSet rs = pr.executeQuery();
      boolean perm = false;

      if (rs.next()) {
        perm = rs.getBoolean("perm");
      }

      rs.close();

      if (!perm) {
        PreparedStatement mod = db.prepareStatement("SELECT moderator FROM groups,topics WHERE topics.groupid=groups.id AND topics.id=?");
        mod.setInt(1, msgid);

        rs = mod.executeQuery();

        if (!rs.next()) {
          throw new MessageNotFoundException(msgid);
        }

        if (rs.getInt("moderator") == user.getId()) {
          perm = true; // NULL is ok
        }

        mod.close();
        rs.close();
      }

      if (!perm) {
        PreparedStatement mod = db.prepareStatement("SELECT topics.moderate as mod, sections.moderate as needmod FROM groups,topics,sections WHERE topics.groupid=groups.id AND topics.id=? AND groups.section=sections.id");
        mod.setInt(1, msgid);

        rs = mod.executeQuery();
        if (!rs.next()) {
          throw new MessageNotFoundException(msgid);
        }

        if (rs.getBoolean("needmod") && !rs.getBoolean("mod") && user.canModerate()) {
          perm = true;
        }

        rs.close();
      }

      if (!perm && user.canModerate()) {
        PreparedStatement mod = db.prepareStatement("SELECT postdate>CURRENT_TIMESTAMP-'1 month'::interval as perm, section FROM topics,groups WHERE topics.groupid=groups.id AND topics.id=?");
        mod.setInt(1, msgid);

        rs = mod.executeQuery();
        if (!rs.next()) {
          throw new MessageNotFoundException(msgid);
        }

        if (rs.getBoolean("perm")) {
          perm = true;
        }

        rs.close();
      }

      if (!perm) {
        user.checkDelete();
      }

      st1.executeUpdate();

      if (user.canModerate() && bonus!=null && bonus!=0 && user.getId()!=message.getUid()) {
        if (bonus>20 || bonus<0) {
          throw new UserErrorException("Некорректное значение bonus");
        }

        User author = User.getUser(db, message.getUid());
        author.changeScore(db, -bonus);
        reason+=" ("+bonus+")";
      }

      st2.setString(3, reason);
      st2.executeUpdate();

      logger.info("Удалено сообщение " + msgid + " пользователем " + nick + " по причине `" + reason + '\'');

      st1.close();
      st2.close();
      db.commit();

      return new ModelAndView("action-done", "message", "Сообщение удалено");
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  @RequestMapping(value = "/undelete.jsp", method = RequestMethod.GET)
  public ModelAndView undeleteForm(
    HttpServletRequest request,
    @RequestParam int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not authorized");
    }

    Connection db = null;
    try {
      db = LorDataSource.getConnection();

      Message message = new Message(db, msgid);

      checkUndeletable(message);

      ModelAndView mv = new ModelAndView("undelete");
      mv.getModel().put("message", message);

      return mv;
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  @RequestMapping(value="/undelete.jsp", method=RequestMethod.POST)
  public ModelAndView undelete(
    HttpServletRequest request,
    @RequestParam int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not authorized");
    }

    Connection db = null;
    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      Message message = new Message(db, msgid);

      checkUndeletable(message);

      PreparedStatement lock = db.prepareStatement("SELECT deleted FROM topics WHERE id=? FOR UPDATE");
      PreparedStatement st1 = db.prepareStatement("UPDATE topics SET deleted='f' WHERE id=?");
      PreparedStatement st2 = db.prepareStatement("DELETE FROM del_info WHERE msgid=?");
      lock.setInt(1, msgid);
      st1.setInt(1, msgid);
      st2.setInt(1, msgid);

      ResultSet lockResult = lock.executeQuery(); // lock another undelete.jsp on this row

      if (lockResult.next() && !lockResult.getBoolean("deleted")) {
        throw new UserErrorException("Сообщение уже восстановлено");
      }

      st1.executeUpdate();
      st2.executeUpdate();

      logger.info("Восстановлено сообщение " + msgid + " пользователем " + tmpl.getNick());

      st1.close();
      st2.close();

      db.commit();

      return new ModelAndView("action-done", "message", "Сообщение восстановлено");
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  private void checkUndeletable(Message message) throws AccessViolationException {
    if (message.isExpired()) {
      throw new AccessViolationException("нельзя восстанавливать устаревшие сообщения");
    }

    if (!message.isDeleted()) {
      throw new AccessViolationException("Сообщение уже восстановлено");
    }
  }
}
