/*
 * Copyright 1998-2012 Linux.org.ru
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.group.GroupPermissionService;
import ru.org.linux.search.SearchQueueSender;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.Template;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserErrorException;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

@Controller
public class DeleteTopicController {
  private static final Log logger = LogFactory.getLog(DeleteTopicController.class);

  @Autowired
  private SearchQueueSender searchQueueSender;

  @Autowired
  private SectionService sectionService;

  @Autowired
  private TopicDao messageDao;

  @Autowired
  private TopicService topicService;

  @Autowired
  private TopicPrepareService prepareService;

  @Autowired
  private GroupPermissionService permissionService;

  @Autowired
  private UserDao userDao;

  @RequestMapping(value="/delete.jsp", method= RequestMethod.GET)
  public ModelAndView showForm(
    @RequestParam("msgid") int msgid,
    HttpServletRequest request
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    Topic msg = messageDao.getById(msgid);

    if (msg.isDeleted()) {
      throw new UserErrorException("Сообщение уже удалено");
    }

    if (!permissionService.isDeletable(msg, tmpl.getCurrentUser())) {
      throw new AccessViolationException("Вы не можете удалить это сообщение");
    }

    Section section = sectionService.getSection(msg.getSectionId());

    HashMap<String, Object> params = new HashMap<>();
    params.put("bonus", !section.isPremoderated());

    params.put("author", userDao.getUser(msg.getUid()));

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
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = tmpl.getCurrentUser();

    user.checkAnonymous();

    Topic message = messageDao.getById(msgid);

    if (message.isDeleted()) {
      throw new UserErrorException("Сообщение уже удалено");
    }

    if (!permissionService.isDeletable(message, user))  {
      throw new AccessViolationException("Вы не можете удалить это сообщение");
    }

    topicService.deleteWithBonus(message, user, reason, bonus);
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
    mv.getModel().put("preparedMessage", prepareService.prepareTopic(message, request.isSecure(), tmpl.getCurrentUser()));

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
