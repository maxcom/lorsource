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
import java.sql.Statement;

import javax.servlet.ServletRequest;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ru.org.linux.site.*;

@Controller
public class TopicModificationController extends ApplicationObjectSupport {
  @RequestMapping(value="/setpostscore.jsp", method= RequestMethod.GET)
  public ModelAndView showForm(
    ServletRequest request,
    @RequestParam int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      Message msg = new Message(db, msgid);

      return new ModelAndView("setpostscore", "message", msg);
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }

  @RequestMapping(value="/setpostscore.jsp", method= RequestMethod.POST)
  public ModelAndView modifyTopic(
    ServletRequest request,
    @RequestParam int msgid,
    @RequestParam int postscore,
    @RequestParam(required=false) Boolean sticky,
    @RequestParam(required=false) Boolean notop
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    if (sticky==null) {
      sticky = false;
    }

    if (notop==null) {
      notop = false;
    }

    if (postscore < -1) {
      postscore = 0;
    }
    if (postscore > 500) {
      postscore = 500;
    }

    Connection db = null;
    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      Message msg = new Message(db, msgid);

      PreparedStatement pst = db.prepareStatement("UPDATE topics SET postscore=?, sticky=?, notop=? WHERE id=?");
      pst.setInt(1, postscore);
      pst.setBoolean(2, sticky);
      pst.setBoolean(3, notop);
      pst.setInt(4, msgid);

      User user = User.getUser(db, tmpl.getNick());
      user.checkCommit();

      pst.executeUpdate();

      StringBuilder out = new StringBuilder();

      if (msg.getPostScore() != postscore) {
        out.append("Установлен новый уровень записи ").append(postscore < 0 ? "только для модераторов" : Integer.toString(postscore)).append("<br>");
        logger.info("Установлен новый уровень записи " + postscore + " для " + msgid + " пользователем " + user.getNick());
      }

      if (msg.isSticky() != sticky) {
        out.append("Новое значение sticky: ").append(sticky).append("<br>");
        logger.info("Новое значение sticky: " + sticky);
      }

      if (msg.isNotop() != notop) {
        out.append("Новое значение notop: ").append(notop).append("<br>");
        logger.info("Новое значение notop: " + notop);
      }

      pst.close();
      db.commit();

      ModelAndView mv = new ModelAndView("action-done");
      mv.getModel().put("message", "Данные изменены");
      mv.getModel().put("bigMessage", out.toString());

      return mv;
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  @RequestMapping(value="/mtn.jsp", method=RequestMethod.GET)
  public ModelAndView moveTopicForm(
    ServletRequest request,
    @RequestParam int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new IllegalAccessException("Not authorized");
    }

    ModelAndView mv = new ModelAndView("mtn");

    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      Message message = new Message(db, msgid);

      mv.getModel().put("message", message);

      mv.getModel().put("groups", Group.getGroups(db, message.getSection()));

      return mv;
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }

  @RequestMapping(value="/mt.jsp", method=RequestMethod.POST)
  public ModelAndView moveTopic(
    ServletRequest request,
    @RequestParam int msgid,
    @RequestParam("moveto") int newgr
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    Connection db = null;

    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      Message msg = new Message(db, msgid);

      Statement st1 = db.createStatement();

      Group newGrp = new Group(db, newgr);
      String url = msg.getUrl();

      PreparedStatement movePst = db.prepareStatement("UPDATE topics SET groupid=?,lastmod=CURRENT_TIMESTAMP WHERE id=?");
      movePst.setInt(1, newGrp.getId());
      movePst.setInt(2, msgid);

      movePst.executeUpdate();

      if (url != null && !newGrp.isLinksAllowed() && !newGrp.isImagePostAllowed()) {
        String sSql = "UPDATE topics SET linktext=null, url=null WHERE id=" + msgid;

        String title = msg.getGroupTitle();
        String linktext = msg.getLinktext();

        st1.executeUpdate(sSql);

        /* if url is not null, update the topic text */
        PreparedStatement pst1 = db.prepareStatement("UPDATE msgbase SET message=message||? WHERE id=?");

        String link;
        if (msg.isLorcode()) {
          link = "\n[url=" + url + ']' + linktext + "[/url]\n";
        } else {
          link = "<br><a href=\"" + url + "\">" + linktext + "</a>\n<br>\n";
        }

        if (msg.isLorcode()) {
          pst1.setString(1, '\n' + link + "\n[i]Перемещено " + tmpl.getNick() + " из " + title + "[/i]\n");
        } else {
          pst1.setString(1, '\n' + link + "<br><i>Перемещено " + tmpl.getNick() + " из " + title + "</i>\n");
        }

        pst1.setInt(2, msgid);
        pst1.executeUpdate();
      }

      logger.info("topic " + msgid + " moved" +
              " by " + tmpl.getNick() + " from news/forum " + msg.getGroupTitle() + " to forum " + newGrp.getTitle());
      db.commit();

      return new ModelAndView(new RedirectView(msg.getLinkLastmod()));
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }

  @RequestMapping(value="/mt.jsp", method=RequestMethod.GET)
  public ModelAndView moveTopicFormForum(
    ServletRequest request,
    @RequestParam int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new IllegalAccessException("Not authorized");
    }

    ModelAndView mv = new ModelAndView("mtn");

    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      Message message = new Message(db, msgid);

      mv.getModel().put("message", message);

      mv.getModel().put("groups", Group.getGroups(db, new Section(db, 2)));

      return mv;
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }
}
