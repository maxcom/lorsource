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
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.site.*;
import ru.org.linux.spring.dao.GroupDao;
import ru.org.linux.spring.dao.IPBlockDao;
import ru.org.linux.spring.dao.SectionDao;
import ru.org.linux.spring.dao.TagDao;
import ru.org.linux.spring.validators.AddCommentRequestValidator;
import ru.org.linux.spring.validators.AddMessageRequestValidator;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.BadURLException;
import ru.org.linux.util.HTMLFormatter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.beans.PropertyEditorSupport;
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
  public ModelAndView add(@Valid @ModelAttribute("form") AddMessageRequest form, HttpServletRequest request) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();

    Template tmpl = Template.getTemplate(request);

    AddMessageForm oldForm = new AddMessageForm(request, tmpl);
    oldForm.setMode(tmpl.getFormatMode());
    params.put("oldForm", oldForm);

    Group group = form.getGroup();

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

  private static String processMessage(String msg, String mode) {
    if (msg == null) {
      return "";
    }

    if ("lorcode".equals(mode)) {
      return msg;
    } else {
      // Format message
      HTMLFormatter formatter = new HTMLFormatter(msg);

      formatter.setMaxLength(80);
      formatter.setOutputLorcode(true);

      if ("ntobr".equals(mode)) {
        formatter.enableNewLineMode();
      }

      return formatter.process();
    }
  }


  @RequestMapping(value="/add.jsp", method=RequestMethod.POST)
  public ModelAndView doAdd(
          HttpServletRequest request,
          @Valid @ModelAttribute("form") AddMessageRequest form,
          BindingResult errors
  ) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();

    Template tmpl = Template.getTemplate(request);
    HttpSession session = request.getSession();

    AddMessageForm oldForm = new AddMessageForm(request, tmpl);
    params.put("oldForm", oldForm);

    Group group = form.getGroup();
    params.put("group", group);

    if (group!=null && group.isModerated()) {
      params.put("topTags", tagDao.getTopTags());
    }

    if (group!=null) {
      params.put("addportal", sectionDao.getAddInfo(group.getSectionId()));
    }

    Connection db = null;

    try {
      ipBlockDao.checkBlockIP(request.getRemoteAddr(), errors);

      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      tmpl.updateCurrentUser(db);

      User user = oldForm.validateAndGetUser(tmpl, db);

      if (group!=null && !group.isTopicPostingAllowed(user)) {
        throw new AccessViolationException("Не достаточно прав для постинга тем в эту группу");
      }
      form.validate(errors);

      String message = processMessage(form.getMsg(), oldForm.getMode());

      if (user.isAnonymous()) {
        if (message.length() > AddMessageForm.MAX_MESSAGE_LENGTH_ANONYMOUS) {
          errors.rejectValue("msg", null, "Слишком большое сообщение");
        }
      } else {
        if (message.length() > AddMessageForm.MAX_MESSAGE_LENGTH) {
          errors.rejectValue("msg", null, "Слишком большое сообщение");
        }
      }

      if (group!=null && group.isImagePostAllowed()) {
        form.setUrl(oldForm.processUpload(session, tmpl));
      }

      Message previewMsg = null;

      if (group!=null) {
        previewMsg = new Message(oldForm, form, user, message);
        params.put("message", new PreparedMessage(db, previewMsg, true));
      }

      if (!oldForm.isPreview() && !errors.hasErrors()) {
        // Flood protection
        if (!session.getId().equals(oldForm.getSessionId())) {
          logger.info("Flood protection (session variable differs) " + request.getRemoteAddr());
          logger.info("Flood protection (session variable differs) " + session.getId() + " != " + oldForm.getSessionId());
          errors.reject(null, "сбой добавления");
        }
      }

      if (!oldForm.isPreview() && !errors.hasErrors() && !Template.isSessionAuthorized(session)) {
        captcha.checkCaptcha(request, errors);
      }

      if (!oldForm.isPreview() && !errors.hasErrors()) {
        dupeProtector.checkDuplication(request.getRemoteAddr(), false, errors);
      }

      if (!oldForm.isPreview() && !errors.hasErrors()) {
        int msgid = previewMsg.addTopicFromPreview(db, tmpl, request, oldForm.getPreviewImagePath(), user);

        if (oldForm.getPollList() != null) {
          int pollId = Poll.createPoll(db, oldForm.getPollList(), oldForm.getMultiSelect());

          Poll poll = new Poll(db, pollId);
          poll.setTopicId(db, msgid);
        }

        if (oldForm.getTags() != null) {
          List<String> tags = TagDao.parseTags(oldForm.getTags());
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
      errors.reject(null, e.getMessage());
      if (db != null) {
        db.rollback();
      }
    } catch (UserNotFoundException e) {
      errors.reject(null, e.getMessage());
      if (db != null) {
        db.rollback();
      }
    } catch (BadURLException e) {
      errors.reject(null, e.getMessage());
      if (db != null) {
        db.rollback();
      }
    } catch (BadImageException e) {
      errors.reject(null, e.getMessage());
      if (db != null) {
        db.rollback();
      }
    } finally {
      if (db != null) {
        db.close();
      }
    }

    if (oldForm.getPollList() != null) {
      params.put("exception", new BindException(errors));
      return new ModelAndView("error", params);
    }

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

    params.put("groups", groupDao.getGroups(section));

    return new ModelAndView("add-section", params);
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(Group.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String text) throws IllegalArgumentException {
        try {
          setValue(groupDao.getGroup(Integer.parseInt(text)));
        } catch (BadGroupException e) {
          throw new IllegalArgumentException(e);
        }
      }

      @Override
      public String getAsText() {
        if (getValue()==null) {
          return null;
        } else {
          return Integer.toString(((Group) getValue()).getId());
        }
      }
    });
  }

  @InitBinder("form")
  public void requestValidator(WebDataBinder binder) {
    binder.setValidator(new AddMessageRequestValidator());

    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor());
  }
}
