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
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.*;
import ru.org.linux.storage.StorageException;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.BadURLException;
import ru.org.linux.util.UtilException;

@Controller
public class AddMessageController extends ApplicationObjectSupport {
  @RequestMapping("/add.jsp")
  protected ModelAndView add(HttpServletRequest request, HttpServletResponse response) throws UtilException, IOException, FileUploadException, ScriptErrorException, BadImageException, InterruptedException, UserErrorException,  StorageException, SQLException {
    Map<String, Object> params = new HashMap<String, Object>();

    Template tmpl = Template.getTemplate(request);
    HttpSession session = request.getSession();

    Message previewMsg = null;
    Exception error = null;

    if (request.getMethod().equals("POST")) {
      Connection db = null;
      AddMessageForm form = null;

      try {
        form = new AddMessageForm(request, tmpl);

        db = LorDataSource.getConnection();
        db.setAutoCommit(false);

        Group group = new Group(db, form.getGuid());
        params.put("group", group);
        User user = form.validateAndGetUser(session, db);

        form.validate(group, user);

        if (group.isImagePostAllowed()) {
          form.processUpload(session, tmpl);
        }

        previewMsg = new Message(db, form, user);

        int section = group.getSectionId();
        params.put("addportal", tmpl.getObjectConfig().getStorage().readMessageDefault("addportal", String.valueOf(section), ""));

        if (!form.isPreview()) {
          // Flood protection
          if (!session.getId().equals(form.getSessionId())) {
            logger.info("Flood protection (session variable differs) " + request.getRemoteAddr());
            logger.info("Flood protection (session variable differs) " + session.getId() + " != " + form.getSessionId());
            throw new BadInputException("сбой добавления");
          }

          // Captch
          if (!Template.isSessionAuthorized(session)) {
            CaptchaSingleton.checkCaptcha(session, form.getCaptchaResponse());
          }
          // Blocked IP
          IPBlockInfo.checkBlockIP(db, request.getRemoteAddr());

          int msgid = previewMsg.addTopicFromPreview(db, tmpl, request, form.getPreviewImagePath(), user);

          if (form.getPollList()!=null) {
            int pollId = Poll.createPoll(db, previewMsg.getTitle(), form.getPollList());

            Poll poll = new Poll(db, pollId);
            poll.setTopicId(db, msgid);
          }

          if (form.getTags()!=null) {
            List<String> tags = Tags.parseTags(form.getTags());
            Tags.updateTags(db, msgid, tags);
          }

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

      params.put("form", form);

      if (form.getPollList()!=null) {
        params.put("exception", error);
        return new ModelAndView("error", params);
      }
    } else {
      AddMessageForm form = new AddMessageForm(request, tmpl);

      params.put("form", form);

      Connection db = null;

      try {
        db = LorDataSource.getConnection();

        Integer groupId = form.getGuid();

        Group group = new Group(db, groupId);

        User currentUser = User.getCurrentUser(db, session);

        if (!group.isTopicPostingAllowed(currentUser)) {
          throw new AccessViolationException("Не достаточно прав для постинга тем в эту группу");
        }

        int section = group.getSectionId();
        params.put("addportal", tmpl.getObjectConfig().getStorage().readMessageDefault("addportal", String.valueOf(section), ""));
        params.put("group", group);
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
