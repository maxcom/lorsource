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

import javax.servlet.ServletRequest;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

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
}
