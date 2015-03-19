/*
 * Copyright 1998-2015 Linux.org.ru
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.Template;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserErrorException;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

@Controller
public class TopicModificationController {
  private static final Logger logger = LoggerFactory.getLogger(TopicModificationController.class);

  @Autowired
  private TopicPrepareService prepareService;

  @Autowired
  private TopicDao messageDao;

  @Autowired
  private SectionService sectionService;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private UserDao userDao;

  @RequestMapping(value="/setpostscore.jsp", method= RequestMethod.GET)
  public ModelAndView showForm(
    ServletRequest request,
    @RequestParam int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    ModelAndView mv = new ModelAndView("setpostscore");
    Topic message = messageDao.getById(msgid);
    mv.addObject("message", message);
    mv.addObject("group", groupDao.getGroup(message.getGroupId()));

    return mv;
  }

  @RequestMapping(value="/setpostscore.jsp", method= RequestMethod.POST)
  public ModelAndView modifyTopic(
    ServletRequest request,
    @RequestParam int msgid,
    @RequestParam int postscore,
    @RequestParam(defaultValue="false") boolean sticky,
    @RequestParam(defaultValue="false") boolean notop
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    if (postscore < TopicPermissionService.POSTSCORE_UNRESTRICTED) {
      throw new UserErrorException("invalid postscore " + postscore);
    }

    if (postscore > TopicPermissionService.POSTSCORE_UNRESTRICTED && postscore < TopicPermissionService.POSTSCORE_REGISTERED_ONLY) {
      throw new UserErrorException("invalid postscore " + postscore);
    }

    if (postscore > TopicPermissionService.POSTSCORE_MODERATORS_ONLY) {
      throw new UserErrorException("invalid postscore " + postscore);
    }

    User user = tmpl.getCurrentUser();
    user.checkCommit();

    Topic msg = messageDao.getById(msgid);

    messageDao.setTopicOptions(msg, postscore, sticky, notop);

    StringBuilder out = new StringBuilder();

    if (msg.getPostscore() != postscore) {
      out.append("Установлен новый уровень записи: ").append(getPostScoreInfoFull(postscore)).append("<br>");
      logger.info("Установлен новый уровень записи " + postscore + " для " + msgid + " пользователем " + user.getNick());
    }

    if (msg.isSticky() != sticky) {
      out.append("Новое значение sticky: ").append(sticky).append("<br>");
      logger.info("Новое значение sticky: " + sticky);
    }

    if (msg.isNotop() != notop) {
      out.append("Новое значение notop: ").append(notop).append("<br>");
      logger.info("Новое значение notop: " + notop);
    }

    ModelAndView mv = new ModelAndView("action-done");
    mv.getModel().put("message", "Данные изменены");
    mv.getModel().put("bigMessage", out.toString());
    mv.getModel().put("link", msg.getLink());

    return mv;
  }

  @RequestMapping(value="/mt.jsp", method=RequestMethod.POST)
  public RedirectView moveTopic(
    ServletRequest request,
    @RequestParam int msgid,
    @RequestParam("moveto") int newgr
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    Topic msg = messageDao.getById(msgid);

    if (msg.isDeleted()) {
      throw new AccessViolationException("Сообщение удалено");
    }

    Group newGrp = groupDao.getGroup(newgr);

    if (msg.getGroupId()!=newGrp.getId()) {
      messageDao.moveTopic(msg, newGrp, tmpl.getCurrentUser());
    }

    return new RedirectView(TopicLinkBuilder.baseLink(msg).forceLastmod().build());
  }

  @RequestMapping(value="/mt.jsp", method=RequestMethod.GET)
  public ModelAndView moveTopicFormForum(
    ServletRequest request,
    @RequestParam int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not authorized");
    }

    ModelAndView mv = new ModelAndView("mtn");

    Topic topic = messageDao.getById(msgid);
    Section section = sectionService.getSection(Section.SECTION_FORUM);

    mv.getModel().put("message", topic);
    mv.getModel().put("groups", groupDao.getGroups(section));
    mv.getModel().put("author", userDao.getUserCached(topic.getUid()));

    return mv;
  }

  @RequestMapping(value="/mtn.jsp", method=RequestMethod.GET)
  public ModelAndView moveTopicForm(
          ServletRequest request,
          @RequestParam int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not authorized");
    }

    ModelAndView mv = new ModelAndView("mtn");

    Topic topic = messageDao.getById(msgid);
    Section section = sectionService.getSection(topic.getSectionId());

    mv.getModel().put("message", topic);
    mv.getModel().put("groups", groupDao.getGroups(section));
    mv.getModel().put("author", userDao.getUserCached(topic.getUid()));

    return mv;
  }

  @RequestMapping(value = "/uncommit.jsp", method = RequestMethod.GET)
  public ModelAndView uncommitForm(
    HttpServletRequest request,
    @RequestParam int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not authorized");
    }

    Topic message = messageDao.getById(msgid);

    checkUncommitable(message);

    ModelAndView mv = new ModelAndView("uncommit");
    mv.getModel().put("message", message);
    mv.getModel().put("preparedMessage", prepareService.prepareTopic(message, request.isSecure(), tmpl.getCurrentUser()));

    return mv;
  }

  @RequestMapping(value="/uncommit.jsp", method=RequestMethod.POST)
  public ModelAndView uncommit(
    HttpServletRequest request,
    @RequestParam int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not authorized");
    }

    Topic message = messageDao.getById(msgid);

    checkUncommitable(message);

    messageDao.uncommit(message);

    logger.info("Отменено подтверждение сообщения " + msgid + " пользователем " + tmpl.getNick());

    return new ModelAndView("action-done", "message", "Подтверждение отменено");
  }

  private static void checkUncommitable(Topic message) throws AccessViolationException {
    if (message.isExpired()) {
      throw new AccessViolationException("нельзя восстанавливать устаревшие сообщения");
    }

    if (message.isDeleted()) {
      throw new AccessViolationException("сообщение удалено");
    }

    if (!message.isCommited()) {
      throw new AccessViolationException("сообщение не подтверждено");
    }
  }

  public static String getPostScoreInfoFull(int postscore) {
    String info = TopicPermissionService.getPostScoreInfo(postscore);
    if (info.isEmpty()) {
      return "без ограничений";
    } else {
      return info;
    }
  }
}
