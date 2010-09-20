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

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ru.org.linux.site.*;

@Controller
public class MemoriesController {
  @RequestMapping(value="/memories.jsp", params = {"add"}, method= RequestMethod.POST)
  public View add(
    HttpServletRequest request,
    @RequestParam("msgid") int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      tmpl.initCurrentUser(db);

      User user = tmpl.getCurrentUser();
      user.checkBlocked();
      user.checkAnonymous();

      Message topic = new Message(db, msgid);
      if (topic.isDeleted()) {
        throw new UserErrorException("Тема удалена");
      }

      PreparedStatement pst = db.prepareStatement("INSERT INTO memories (userid, topic) values (?,?)");
      pst.setInt(1, user.getId());
      pst.setInt(2, topic.getId());
      pst.executeUpdate();

      return new RedirectView(topic.getLink());
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }

  @RequestMapping(value="/memories.jsp", params = {"remove"}, method= RequestMethod.POST)
  public ModelAndView remove(
    HttpServletRequest request,
    @RequestParam("id") int id
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    Connection db = null;

    try {
      db = LorDataSource.getConnection();
      tmpl.initCurrentUser(db);

      User user = tmpl.getCurrentUser();
      user.checkBlocked();
      user.checkAnonymous();

      MemoriesListItem m = MemoriesListItem.getMemoriesListItem(db, id);

      if (m != null) {
        if (m.getUserid() != user.getId()) {
          throw new AccessViolationException("Нельзя удалить чужую запись");
        }

        Message topic = new Message(db, m.getTopic());

        PreparedStatement pst = db.prepareStatement("DELETE FROM memories WHERE id=?");
        pst.setInt(1, id);
        pst.executeUpdate();

        return new ModelAndView(new RedirectView(topic.getLink()));
      } else {
        return new ModelAndView("action-done", "message", "Запись уже удалена");
      }

    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }
}
