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
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ru.org.linux.site.*;

@Controller
public class MessageController {
  @RequestMapping("/view-message.jsp")
  public ModelAndView getMessage(WebRequest webRequest, HttpServletRequest request, HttpServletResponse response, @RequestParam("msgid") int msgid) throws Exception {
    Template tmpl = Template.getTemplate(request);

    Map<String, Object> params = new HashMap<String, Object>();

    params.put("msgid", msgid);

    boolean showDeleted = request.getParameter("deleted") != null;
    boolean rss = request.getParameter("output")!=null && "rss".equals(request.getParameter("output"));

    if (showDeleted && !"POST".equals(request.getMethod())) {
      return new ModelAndView(new RedirectView("view-message.jsp?msgid=" + msgid));
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

      if (new Group(db, message.getGroupId()).getCommentsRestriction()==-1 && !Template.isSessionAuthorized(request.getSession())) {
        throw new AccessViolationException("Это сообщение нельзя посмотреть");
      }

      String etag = getEtag(message, tmpl);
      response.setHeader("Etag", etag);

      if (request.getHeader("If-None-Match")!=null) {
        if (etag.equals(request.getHeader("If-None-Match"))) {
          response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
          return null;
        }
      } else if (webRequest.checkNotModified(message.getLastModified().getTime())) {
        return null;
      }

      params.put("message", message);

      if (message.isExpired()) {
        response.setDateHeader("Expires", System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L);
      }

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

  private String getEtag(Message message, Template tmpl) {
    String nick = tmpl.getNick();

    String userAddon = nick!=null?('-' +nick):"";

    return "message-"+message.getMessageId()+ '-' +message.getLastModified().getTime()+userAddon;
  }
}
