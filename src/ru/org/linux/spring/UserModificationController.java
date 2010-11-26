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

import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ru.org.linux.site.*;
import ru.org.linux.util.HTMLFormatter;

@Controller
public class UserModificationController extends ApplicationObjectSupport {
  private SearchQueueSender searchQueueSender;

  @Autowired
  @Required
  public void setSearchQueueSender(SearchQueueSender searchQueueSender) {
    this.searchQueueSender = searchQueueSender;
  }

  @RequestMapping(value="/usermod.jsp", method= RequestMethod.POST)
  public ModelAndView modifyUser(
    HttpServletRequest request,
    HttpSession session,
    @RequestParam("action") String action,
    @RequestParam("id") int id,
    @RequestParam(value="reason", required = false) String reason
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    Connection db = null;

    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      Statement st = db.createStatement();

      User user = User.getUser(db, id);

      User moderator = User.getUser(db, tmpl.getNick());

      if ("block".equals(action) || "block-n-delete-comments".equals(action)) {
        if (!user.isBlockable()) {
          throw new AccessViolationException("Пользователя " + user.getNick() + " нельзя заблокировать");
        }

        user.block(db, moderator, reason);
        user.resetPassword(db);
        logger.info("User " + user.getNick() + " blocked by " + session.getValue("nick"));

        if ("block-n-delete-comments".equals(action)) {
          Map<String, Object> params = new HashMap<String, Object>();
          params.put("message", "Удалено");
          params.put("bigMessage", user.deleteAllComments(db, moderator, searchQueueSender));
          db.commit();
          return new ModelAndView("action-done", params);
        }
      } else if ("toggle_corrector".equals(action)) {
        if (user.getScore()<User.CORRECTOR_SCORE) {
          throw new AccessViolationException("Пользователя " + user.getNick() + " нельзя сделать корректором");
        }

        if (user.canCorrect()) {
          st.executeUpdate("UPDATE users SET corrector='f' WHERE id=" + id);
        } else {
          st.executeUpdate("UPDATE users SET corrector='t' WHERE id=" + id);
        }
      } else if ("unblock".equals(action)) {
        if (!user.isBlockable()) {
          throw new AccessViolationException("Пользователя " + user.getNick() + " нельзя разблокировать");
        }

        st.executeUpdate("UPDATE users SET blocked='f' WHERE id=" + id);
        st.executeUpdate("DELETE FROM ban_info WHERE userid="+id);
        logger.info("User " + user.getNick() + " unblocked by " + session.getValue("nick"));
      } else if ("remove_userinfo".equals(action)) {
        if (user.canModerate()) {
          throw new AccessViolationException("Пользователю " + user.getNick() + " нельзя удалить сведения");
        }

        user.setUserinfo(db, null);
        user.changeScore(db, -10);
        logger.info("Clearing " + user.getNick() + " userinfo");
      } else {
        throw new UserErrorException("Invalid action=" + HTMLFormatter.htmlSpecialChars(action));
      }

      db.commit();

      Random random = new Random();

      return new ModelAndView(new RedirectView("/people/" + URLEncoder.encode(user.getNick()) + "/profile?nocache=" + random.nextInt()));
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  @RequestMapping(value="/remove-userpic.jsp", method= RequestMethod.POST)
  public ModelAndView removeUserpic(
    HttpServletRequest request,
    @RequestParam("id") int id
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not autorized");
    }

    Connection db = null;

    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      Statement st = db.createStatement();

      User user = User.getUser(db, id);

      User currentUser = User.getUser(db, tmpl.getNick());

      if (!currentUser.canModerate() && currentUser.getId()!=user.getId()) {
        throw new AccessViolationException("Not permitted");
      }

      if (user.canModerate()) {
        throw new AccessViolationException("Пользователю " + user.getNick() + " нельзя удалить картинку");
      }

      if (user.getPhoto() == null) {
        throw new AccessViolationException("Пользователь " + user.getNick() + " картинки не имеет");
      }

      st.executeUpdate("UPDATE users SET photo=null WHERE id=" + id);

      if (currentUser.canModerate() && currentUser.getId()!=user.getId()) {
        user.changeScore(db, -10);
      }

      logger.info("Clearing " + user.getNick() + " userpic by " + currentUser.getNick());

      db.commit();

      Random random = new Random();

      return new ModelAndView(new RedirectView("/people/" + URLEncoder.encode(user.getNick()) + "/profile?nocache=" + random.nextInt()));
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }
}
