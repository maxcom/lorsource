package ru.org.linux.spring;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.fileupload.FileUploadException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import ru.org.linux.site.*;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.BadURLException;
import ru.org.linux.util.UtilException;

public class AddMessageController extends AbstractController {
  @Override
  protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws SQLException, UtilException, IOException, FileUploadException, ScriptErrorException, BadImageException, InterruptedException {
    Map<String, Object> params = new HashMap<String, Object>();

    Template tmpl = Template.getTemplate(request);
    HttpSession session = request.getSession();

    Message previewMsg = null;
    Exception error = null;

    if (request.getMethod().equals("POST")) {
      Connection db = null;

      try {
        db = LorDataSource.getConnection();
        db.setAutoCommit(false);

        previewMsg = new Message(db, tmpl, session, request);

        if (!previewMsg.isPreview()) {
          int msgid = previewMsg.addTopicFromPreview(db, tmpl, session, request);
          if (request.getAttribute("tags")!=null) {
            List<String> tags = Tags.parseTags((String)request.getAttribute("tags"));
            Tags.updateTags(db, msgid, tags);
          }

          Group group = new Group(db, previewMsg.getGroupId());

          db.commit();

          Random random = new Random();

          String messageUrl = "view-message.jsp?msgid=" + msgid;

          if (!group.isModerated()) {
            response.setHeader("Location", tmpl.getMainUrl() + messageUrl + "&nocache=" + random.nextInt());
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
          }

          params.put("moderated", group.isModerated());
          params.put("url", tmpl.getMainUrl()+messageUrl);

          return new ModelAndView("add-done-moderated", params);
        }
      } catch (UserErrorException e) {
        error=e;
        if (db!=null) {
          db.rollback();
        }
      } catch (UserNotFoundException e) {
        error=e;
        if (db!=null) {
          db.rollback();
        }
      } catch (BadURLException e) {
        error=e;
        if (db!=null) {
          db.rollback();
        }
      } finally {
        if (db!=null) {
          db.close();
        }
      }
    }

    params.put("message", previewMsg);
    params.put("error", error);
    return new ModelAndView("add", params);
  }
}
