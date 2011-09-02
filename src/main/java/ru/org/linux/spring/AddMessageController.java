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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.site.*;
import ru.org.linux.spring.dao.GroupDao;
import ru.org.linux.spring.dao.IPBlockDao;
import ru.org.linux.spring.dao.SectionDao;
import ru.org.linux.spring.dao.TagDao;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.BadURLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.Connection;
import java.util.*;

@Controller
public class AddMessageController extends ApplicationObjectSupport {
  private SearchQueueSender searchQueueSender;
  private CaptchaService captcha;
  private DupeProtector dupeProtector;
  private IPBlockDao ipBlockDao;
  private GroupDao groupDao;
  private SectionDao sectionDao;
  private TagDao tagDao;

  @Autowired
  public void setSearchQueueSender(SearchQueueSender searchQueueSender) {
    this.searchQueueSender = searchQueueSender;
  }

  @Autowired
  public void setCaptcha(CaptchaService captcha) {
    this.captcha = captcha;
  }

  @Autowired
  public void setDupeProtector(DupeProtector dupeProtector) {
    this.dupeProtector = dupeProtector;
  }

  @Autowired
  public void setIpBlockDao(IPBlockDao ipBlockDao) {
    this.ipBlockDao = ipBlockDao;
  }

  @Autowired
  public void setGroupDao(GroupDao groupDao) {
    this.groupDao = groupDao;
  }

  @Autowired
  public void setSectionDao(SectionDao sectionDao) {
    this.sectionDao = sectionDao;
  }

  @Autowired
  public void setTagDao(TagDao tagDao) {
    this.tagDao = tagDao;
  }

  @RequestMapping(value = "/add.jsp", method = RequestMethod.GET)
  public ModelAndView add(HttpServletRequest request) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();

    Template tmpl = Template.getTemplate(request);

    AddMessageForm form = new AddMessageForm(request, tmpl);
    form.setMode(tmpl.getFormatMode());
    params.put("form", form);

    Group group = groupDao.getGroup(form.getGuid());

    if (!group.isTopicPostingAllowed(tmpl.getCurrentUser())) {
      throw new AccessViolationException("Не достаточно прав для постинга тем в эту группу");
    }

    params.put("group", group);

    if (group.isModerated()) {
      params.put("topTags", tagDao.getTopTags());
    }

    params.put("addportal", sectionDao.getAddInfo(group.getSectionId()));

    return new ModelAndView("add", params);
  }

  @RequestMapping(value="/add.jsp", method=RequestMethod.POST)
  public ModelAndView doAdd(HttpServletRequest request) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();

    Template tmpl = Template.getTemplate(request);
    HttpSession session = request.getSession();

    AddMessageForm form = new AddMessageForm(request, tmpl);
    params.put("form", form);

    Group group = groupDao.getGroup(form.getGuid());
    params.put("group", group);

    if (group.isModerated()) {
      params.put("topTags", tagDao.getTopTags());
    }

    params.put("addportal", sectionDao.getAddInfo(group.getSectionId()));

    Connection db = null;
    Exception error = null;

    try {
      ipBlockDao.checkBlockIP(request.getRemoteAddr());

      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      tmpl.updateCurrentUser(db);

      User user = form.validateAndGetUser(tmpl, db);

      form.validate(group, user);

      if (group.isImagePostAllowed()) {
        form.processUpload(session, tmpl);
      }

      Message previewMsg = new Message(db, form, user);
      params.put("message", new PreparedMessage(db, previewMsg, true));

      if (!form.isPreview()) {
        // Flood protection
        if (!session.getId().equals(form.getSessionId())) {
          logger.info("Flood protection (session variable differs) " + request.getRemoteAddr());
          logger.info("Flood protection (session variable differs) " + session.getId() + " != " + form.getSessionId());
          throw new BadInputException("сбой добавления");
        }

        // Captch
        if (!Template.isSessionAuthorized(session)) {
          captcha.checkCaptcha(request);
        }
        dupeProtector.checkDuplication(request.getRemoteAddr());

        int msgid = previewMsg.addTopicFromPreview(db, tmpl, request, form.getPreviewImagePath(), user);

        if (form.getPollList() != null) {
          int pollId = Poll.createPoll(db, form.getPollList(), form.getMultiSelect());

          Poll poll = new Poll(db, pollId);
          poll.setTopicId(db, msgid);
        }

        if (form.getTags() != null) {
          List<String> tags = TagDao.parseTags(form.getTags());
          TagDao.updateTags(db, msgid, tags);
          TagDao.updateCounters(db, Collections.<String>emptyList(), tags);
        }

        db.commit();

        searchQueueSender.updateMessageOnly(msgid);

        Random random = new Random();

        String messageUrl = "view-message.jsp?msgid=" + msgid;

        if (!group.isModerated()) {
          return new ModelAndView(new RedirectView(messageUrl + "&nocache=" + random.nextInt()));
        }

        params.put("moderated", group.isModerated());
        params.put("url", messageUrl);

        return new ModelAndView("add-done-moderated", params);
      }
    } catch (UserErrorException e) {
      error = e;
      if (db != null) {
        db.rollback();
      }
    } catch (UserNotFoundException e) {
      error = e;
      if (db != null) {
        db.rollback();
      }
    } catch (BadURLException e) {
      error = e;
      if (db != null) {
        db.rollback();
      }
    } catch (BadImageException e) {
      error = e;
      if (db != null) {
        db.rollback();
      }
    } finally {
      if (db != null) {
        db.close();
      }
    }

    if (form.getPollList() != null) {
      params.put("exception", error);
      return new ModelAndView("error", params);
    }

    params.put("error", error);
    return new ModelAndView("add", params);
  }

  @RequestMapping(value="/add-poll.jsp", method=RequestMethod.GET)
  public ModelAndView addPoll(HttpServletRequest request) throws Exception {
    if (!Template.isSessionAuthorized(request.getSession())) {
      throw new AccessViolationException("Not authorized");
    }

    return new ModelAndView("add-poll");
  }

  @RequestMapping(value = "/add-section.jsp")
  public ModelAndView showForm(@RequestParam("section") int sectionId) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();

    params.put("sectionId", sectionId);

    Section section = sectionDao.getSection(sectionId);

    params.put("section", section);

    params.put("info", sectionDao.getAddInfo(section.getId()));

    Connection db = null;
    try {
      db = LorDataSource.getConnection();

      params.put("groups", Group.getGroups(db, section));

      return new ModelAndView("add-section", params);
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }
}
