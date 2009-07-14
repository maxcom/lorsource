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
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
  public ModelAndView showForm(@RequestParam("msgid") int msgid) {
    return new ModelAndView("delete", "msgid", msgid);
  }

  @RequestMapping(value="/delete.jsp", method= RequestMethod.POST)
  public ModelAndView deleteMessage(
    @RequestParam("msgid") int msgid,
    @RequestParam(value="nick", required = false) String nick,
    @RequestParam("reason") String reason,
    HttpServletRequest request
  ) throws Exception {
    HttpSession session = request.getSession();
    Connection db = null;
    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      PreparedStatement lock = db.prepareStatement("SELECT deleted FROM topics WHERE id=? FOR UPDATE");
      PreparedStatement st1 = db.prepareStatement("UPDATE topics SET deleted='t',sticky='f' WHERE id=?");
      PreparedStatement st2 = db.prepareStatement("INSERT INTO del_info (msgid, delby, reason) values(?,?,?)");
      lock.setInt(1, msgid);
      st1.setInt(1, msgid);
      st2.setInt(1, msgid);
      st2.setString(3, reason);

      User user;

      if (session == null || session.getAttribute("login") == null || !(Boolean) session.getAttribute("login")) {
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
      st2.setInt(2, user.getId());

      ResultSet lockResult = lock.executeQuery(); // lock another delete.jsp on this row

      if (lockResult.next() && lockResult.getBoolean("deleted")) {
        throw new UserErrorException("Сообщение уже удалено");
      }

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
}
