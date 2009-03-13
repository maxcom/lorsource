package ru.org.linux.spring;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import ru.org.linux.site.*;
import ru.org.linux.util.ServletParameterParser;

public class EditController extends AbstractController {
  @Override
  protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
    Template tmpl = Template.getTemplate(request);
    HttpSession session = request.getSession();

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    int msgid = new ServletParameterParser(request).getInt("msgid");

    Map<String, Object> params = new HashMap<String, Object>();

    Connection db = null;
    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      Message message = new Message(db, msgid);
      params.put("message", message);

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


        if (!message.getLinktext().equals(newMsg.getLinktext())) {
          modified = true;
        }


        if (message.isHaveLink() && !message.getUrl().equals(newMsg.getUrl())) {
          modified = true;
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

          List<String> oldTags = Tags.getMessageTags(db, message.getId());
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
