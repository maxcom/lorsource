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

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ru.org.linux.site.*;
import ru.org.linux.util.HTMLFormatter;
import ru.org.linux.util.ServletParameterParser;

@Controller
public class CommitController extends ApplicationObjectSupport {
  @Autowired(required = true)
  private Properties properties;

  @Autowired(required = true)
  private XmlRpcClientConfigImpl config;

  @RequestMapping(value = "/commit.jsp", method = RequestMethod.GET)
  public ModelAndView showForm(
    HttpServletRequest request,
    @RequestParam("msgid") int msgid
  ) throws Exception {
    if (!Template.isSessionAuthorized(request.getSession())) {
      throw new AccessViolationException("Not authorized");
    }

    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      Message message = new Message(db, msgid);

      Group group = new Group(db, message.getGroupId());

      if (message.isCommited()) {
        throw new AccessViolationException("Сообщение уже подтверждено");
      }

      if (!group.isModerated()) {
        throw new AccessViolationException("группа не является модерируемой");
      }

      Section section = new Section(db, group.getSectionId());

      HashMap<String, Object> params = new HashMap<String, Object>();
      params.put("message", message);
      params.put("msgid", msgid);
      params.put("section", section);

      return new ModelAndView("commit", params);
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  @RequestMapping(value = "/commit.jsp", method = RequestMethod.POST)
  public ModelAndView commit(
    HttpServletRequest request,
    @RequestParam("msgid") int msgid,
    @RequestParam("title") String title,
    @RequestParam("bonus") int bonus
  ) throws Exception {
    if (!Template.isSessionAuthorized(request.getSession())) {
      throw new AccessViolationException("Not authorized");
    }

    Connection db = null;

    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);
      PreparedStatement pst = db.prepareStatement("UPDATE topics SET title=? WHERE id=?");
      pst.setInt(2, msgid);
      pst.setString(1, HTMLFormatter.htmlSpecialChars(title));

      Message message = new Message(db, msgid);

      User user = User.getUser(db, (String) request.getSession().getAttribute("nick"));
      message.commit(db, user, bonus);

      user.checkCommit();

      if (request.getParameter("chgrp") != null) {
        int changeGroupId = new ServletParameterParser(request).getInt("chgrp");

        int oldgrp = message.getGroupId();
        if (oldgrp != changeGroupId) {
          Group changeGroup = new Group(db, changeGroupId);

          int section = message.getSectionId();
          if (section != 1 && section != 3) {
            throw new AccessViolationException("Can't move topics in non-news section");
          }

          if (changeGroup.getSectionId() != section) {
            throw new AccessViolationException("Can't move topics between sections");
          }
          
          Statement st = db.createStatement();
          st.executeUpdate("UPDATE topics SET groupid=" + changeGroupId + " WHERE id=" + msgid);
          /* to recalc counters */
          st.executeUpdate("UPDATE groups SET stat4=stat4+1 WHERE id=" + oldgrp + " or id=" + changeGroupId);
          st.close();
        }
      }

      pst.executeUpdate();

      if (request.getParameter("tags") != null) {
        List<String> tags = Tags.parseTags(request.getParameter("tags"));
        Tags.updateTags(db, msgid, tags);
        Tags.updateCounters(db, null, Tags.getMessageTags(db, msgid));
      }

      logger.info("Подтверждено сообщение " + msgid + " пользователем " + user.getNick());

      pst.close();
      db.commit();

      pingFeedburner();

      Random random = new Random();

      return new ModelAndView(new RedirectView("view-all.jsp?nocache=" + random.nextInt()));
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  private void pingFeedburner() {
    try {
      config.setServerURL(new URL("http://ping.feedburner.com/"));

      XmlRpcClient client = new XmlRpcClient();

      client.setConfig(config);

      Object[] params = new Object[]{"Linux.org.ru", properties.getProperty(Template.PROPERTY_MAIN_URL)};

      Map r = (Map) client.execute("weblogUpdates.ping", params);

      if ((Boolean) r.get("flerror")) {
        logger.warn("Feedburner ping failed: "+r.get("message"));
      } else {
        logger.info("Feedburner ping ok: "+r.get("message"));
      }
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    } catch (XmlRpcException e) {
      logger.warn("Feedburner ping failed", e);
    }
  }

  public void setProperties(Properties properties) {
    this.properties = properties;
  }

  public void setConfig(XmlRpcClientConfigImpl config) {
    this.config = config;
  }
}
