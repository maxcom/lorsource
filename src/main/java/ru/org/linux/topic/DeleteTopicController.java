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

package ru.org.linux.topic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.Template;
import ru.org.linux.search.SearchQueueSender;
import ru.org.linux.section.Section;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.User;
import ru.org.linux.user.UserErrorException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;

@Controller
public class DeleteTopicController extends ApplicationObjectSupport {
  @Autowired
  private SearchQueueSender searchQueueSender;
  @Autowired
  private UserDao userDao;
  @Autowired
  private SectionService sectionService;
  @Autowired
  private TopicDao messageDao;
  @Autowired
  private TopicPrepareService prepareService;

  @RequestMapping(value="/delete.jsp", method= RequestMethod.GET)
  public ModelAndView showForm(
    @RequestParam("msgid") int msgid,
    HttpSession session,
    HttpServletRequest request
  ) throws Exception {
    if (!Template.isSessionAuthorized(session)) {
      throw new AccessViolationException("Not authorized");
    }

    Topic msg = messageDao.getById(msgid);

    if (msg.isDeleted()) {
      throw new UserErrorException("Сообщение уже удалено");
    }

    Section section = sectionService.getSection(msg.getSectionId());

    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("bonus", !section.isPremoderated());

    params.put("msgid", msgid);

    return new ModelAndView("delete", params);
  }

  @RequestMapping(value="/delete.jsp", method= RequestMethod.POST)
  public ModelAndView deleteMessage(
    @RequestParam("msgid") int msgid,
    @RequestParam("reason") String reason,
    @RequestParam(value="bonus", defaultValue = "0") int bonus,
    HttpServletRequest request
  ) throws Exception {
    HttpSession session = request.getSession();

    if (!Template.isSessionAuthorized(session)) {
      throw new AccessViolationException("Not authorized");
    }

    Template tmpl = Template.getTemplate(request);
    tmpl.updateCurrentUser(userDao);

    User user = tmpl.getCurrentUser();

    user.checkAnonymous();

    Topic message = messageDao.getById(msgid);
    Section section = sectionService.getSection(message.getSectionId());

    if(message.isDeleted()) {
      throw new UserErrorException("Сообщение уже удалено");
    }

    boolean perm = message.isDeletableByUser(user);

    if (!perm && user.isModerator()) {
      perm = message.isDeletableByModerator(user, section);
    }

    if (!perm) {
      user.checkDelete();
    }

    messageDao.deleteWithBonus(message, user, reason, bonus);
    logger.info("Удалено сообщение " + msgid + " пользователем " + user.getNick() + " по причине `" + reason + '\'');

    // Delete msgs from search index
    searchQueueSender.updateMessage(msgid, true);

    return new ModelAndView("action-done", "message", "Сообщение удалено");
  }

  @RequestMapping(value = "/undelete.jsp", method = RequestMethod.GET)
  public ModelAndView undeleteForm(
    HttpServletRequest request,
    @RequestParam int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not authorized");
    }

    Topic message = messageDao.getById(msgid);

    checkUndeletable(message);

    ModelAndView mv = new ModelAndView("undelete");
    mv.getModel().put("message", message);
    mv.getModel().put("preparedMessage", prepareService.prepareMessage(message, false, request.isSecure()));

    return mv;
  }

  @RequestMapping(value="/undelete.jsp", method=RequestMethod.POST)
  public ModelAndView undelete(
    HttpServletRequest request,
    @RequestParam int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not authorized");
    }

    tmpl.updateCurrentUser(userDao);

    Topic message = messageDao.getById(msgid);

    checkUndeletable(message);

    if(message.isDeleted()) {
      messageDao.undelete(message);
    }

    logger.info("Восстановлено сообщение " + msgid + " пользователем " + tmpl.getNick());

    // Undelete msgs from search index
    searchQueueSender.updateMessage(msgid, true);

    return new ModelAndView("action-done", "message", "Сообщение восстановлено");
  }

  private static void checkUndeletable(Topic message) throws AccessViolationException {
    if (message.isExpired()) {
      throw new AccessViolationException("нельзя восстанавливать устаревшие сообщения");
    }

    if (!message.isDeleted()) {
      throw new AccessViolationException("Сообщение уже восстановлено");
    }
  }
}
