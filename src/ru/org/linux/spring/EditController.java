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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.*;

@Controller
public class EditController extends ApplicationObjectSupport {
  @RequestMapping("/edit.jsp")
  protected ModelAndView edit(HttpServletRequest request, @RequestParam("msgid") int msgid) throws Exception {
    Template tmpl = Template.getTemplate(request);
    HttpSession session = request.getSession();

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    Map<String, Object> params = new HashMap<String, Object>();

    Connection db = null;
    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      Message message = new Message(db, msgid);
      params.put("message", message);

      Group group = new Group(db, message.getGroupId());
      params.put("group", group);

      User user = User.getCurrentUser(db, session);

      if (!message.isEditable(db, user)) {
        throw new AccessViolationException("это сообщение нельзя править");
      }

      Message newMsg = message;

      if ("POST".equals(request.getMethod())) {
        newMsg = new Message(db, message, request);
        boolean preview = request.getParameter("preview") != null;

        boolean modified = false;

        if (!message.getTitle().equals(newMsg.getTitle())) {
          modified = true;
        }

        boolean messageModified = false;
        if (!message.getMessage().equals(newMsg.getMessage())) {
          messageModified = true;
        }

        if (message.getLinktext()==null) {
          if (newMsg.getLinktext()!=null) {
            modified = true;
          }
        } else if (!message.getLinktext().equals(newMsg.getLinktext())) {
          modified = true;
        }

        if (message.isHaveLink()) {
          if (message.getUrl() == null) {
            if (newMsg.getUrl()!=null) {
              modified = true;
            }
          } else if (!message.getUrl().equals(newMsg.getUrl())) {
            modified = true;
          }
        }

        if (!preview) {
          PreparedStatement pst = db.prepareStatement("UPDATE topics SET title=?, linktext=?, url=? WHERE id=?");

          pst.setString(1, newMsg.getTitle());
          pst.setString(2, newMsg.getLinktext());
          pst.setString(3, newMsg.getUrl());
          pst.setInt(4, message.getId());

          if (modified) {
            pst.executeUpdate();
          }

          if (messageModified) {
            newMsg.updateMessageText(db);
          }

          List<String> oldTags = message.getTags().getTags();
          List<String> newTags = Tags.parseTags(newMsg.getTags().toString());

          boolean modifiedTags = Tags.updateTags(db, message.getId(), newTags);
          if (modifiedTags && message.isCommited()) {
            Tags.updateCounters(db, oldTags, newTags);
          }

          params.put("modifiedTags", modifiedTags);
          params.put("modified", modified || messageModified || modifiedTags);

          if (modified || messageModified || modifiedTags) {
            logger.info("сообщение " + message.getId() + " исправлено " + session.getValue("nick"));

            db.commit();
            return new ModelAndView("edit-done", params);
          } else {
            params.put("info", "nothing changed");
          }
        } else {
          params.put("info", "Предпросмотр");
        }
      }

      params.put("newMsg", newMsg);

      return new ModelAndView("edit", params);
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }
}
