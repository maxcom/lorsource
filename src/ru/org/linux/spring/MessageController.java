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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.*;

@Controller
public class MessageController {
  @RequestMapping("/view-message.jsp")
  public ModelAndView getMessage(HttpServletRequest request, HttpServletResponse response, @RequestParam("msgid") int msgid) throws Exception {
    Template tmpl = Template.getTemplate(request);

    Map<String, Object> params = new HashMap<String, Object>();

    params.put("msgid", msgid);

    boolean showDeleted = request.getParameter("deleted") != null;
    boolean rss = request.getParameter("output")!=null && "rss".equals(request.getParameter("output"));

    if (showDeleted && !"POST".equals(request.getMethod())) {
      response.setHeader("Location", tmpl.getMainUrl() + "view-message.jsp?msgid=" + msgid);
      response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

      showDeleted = false;
    }

    if (showDeleted) {
      if (!Template.isSessionAuthorized(request.getSession())) {
        throw new BadInputException("Вы уже вышли из системы");
      }
    }

    params.put("showDeleted", showDeleted);

    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      Message message = new Message(db, msgid);

      if (message.isExpired() && showDeleted && !tmpl.isModeratorSession()) {
        throw new MessageNotFoundException(message.getId(), "нельзя посмотреть удаленные комментарии в устаревших темах");
      }

      if (message.isExpired() && message.isDeleted() && !tmpl.isModeratorSession()) {
        throw new MessageNotFoundException(message.getId(), "нельзя посмотреть устаревшие удаленные сообщения");
      }

      if (message.isDeleted() && !Template.isSessionAuthorized(request.getSession())) {
        throw new MessageNotFoundException(message.getId(), "Сообщение удалено");
      }

      if (new Group(db, message.getGroupId()).isCommentsRestricted() && !Template.isSessionAuthorized(request.getSession())) {
        throw new AccessViolationException("Это сообщение нельзя посмотреть");
      }

      params.put("message", message);

      setLastmodified(response, showDeleted, message);

      params.put("prevMessage", message.getPreviousMessage(db));
      params.put("nextMessage", message.getNextMessage(db));

      CommentList comments = CommentList.getCommentList(db, message, showDeleted);

      params.put("comments", comments);

      return new ModelAndView(rss?"view-message-rss":"view-message", params);
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }

  private void setLastmodified(HttpServletResponse response, boolean showDeleted, Message message) {
    if (!message.isDeleted() && !showDeleted && message.getLastModified() != null) {
      response.setDateHeader("Last-Modified", message.getLastModified().getTime());
    }

    if (message.isExpired()) {
      response.setDateHeader("Expires", System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L);
    }/* else {
      response.setDateHeader("Expires", System.currentTimeMillis() - 24 * 60 * 60 * 1000);
    }*/
  }
}
