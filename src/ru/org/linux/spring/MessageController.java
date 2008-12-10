package ru.org.linux.spring;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import ru.org.linux.site.*;
import ru.org.linux.util.ServletParameterParser;

public class MessageController extends AbstractController {
  @Override
  protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
    int msgid = new ServletParameterParser(request).getInt("msgid");
    Template tmpl = Template.getTemplate(request);

    Map<String, Object> params = new HashMap<String, Object>();

    params.put("msgid", msgid);

    boolean showDeleted = request.getParameter("deleted") != null;

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

      if (new Group(db, message.getGroupId()).isCommentsRestricted()) {
        throw new AccessViolationException("Это сообщение нельзя посмотреть");
      }

      params.put("message", message);

      setLastmodified(response, showDeleted, message);

      params.put("prevMessage", message.getPreviousMessage(db));
      params.put("nextMessage", message.getNextMessage(db));

      if (message.isCommentEnabled()) {
        CommentList comments = CommentList.getCommentList(db, message, showDeleted);

        params.put("comments", comments);
      }

      return new ModelAndView("view-message", params);
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
    } else {
      response.setDateHeader("Expires", System.currentTimeMillis() - 24 * 60 * 60 * 1000);
    }
  }
}
