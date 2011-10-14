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
import ru.org.linux.spring.dao.MessageDao;
import ru.org.linux.spring.dao.SectionDao;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

@Controller
public class TopicModificationController extends ApplicationObjectSupport {
  @Autowired
  private PrepareService prepareService;

  @Autowired
  private MessageDao messageDao;

  @Autowired
  private SectionDao sectionDao;

  @Autowired
  private GroupDao groupDao;

  @RequestMapping(value="/setpostscore.jsp", method= RequestMethod.GET)
  public ModelAndView showForm(
    ServletRequest request,
    @RequestParam int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    return new ModelAndView("setpostscore", "message", messageDao.getById(msgid));
  }

  @RequestMapping(value="/setpostscore.jsp", method= RequestMethod.POST)
  public ModelAndView modifyTopic(
    ServletRequest request,
    @RequestParam int msgid,
    @RequestParam int postscore,
    @RequestParam(defaultValue="false") boolean sticky,
    @RequestParam(defaultValue="false") boolean notop,
    @RequestParam(defaultValue="false") boolean minor
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    if (postscore < Message.POSTSCORE_UNRESTRICTED) {
      throw new UserErrorException("invalid postscore " + postscore);
    }

    if (postscore > Message.POSTSCORE_UNRESTRICTED && postscore < Message.POSTSCORE_REGISTERED_ONLY) {
      throw new UserErrorException("invalid postscore " + postscore);
    }

    if (postscore > Message.POSTSCORE_MODERATORS_ONLY) {
      throw new UserErrorException("invalid postscore " + postscore);
    }

    User user = tmpl.getCurrentUser();
    user.checkCommit();

    Message msg = messageDao.getById(msgid);

    messageDao.setTopicOptions(msg, postscore, sticky, notop, minor);

    StringBuilder out = new StringBuilder();

    if (msg.getPostScore() != postscore) {
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

  @RequestMapping(value="/mtn.jsp", method=RequestMethod.GET)
  public ModelAndView moveTopicForm(
    ServletRequest request,
    @RequestParam int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new IllegalAccessException("Not authorized");
    }

    ModelAndView mv = new ModelAndView("mtn");

    Message message = messageDao.getById(msgid);
    Section section = sectionDao.getSection(message.getSectionId());

    mv.getModel().put("message", message);

    mv.getModel().put("groups", groupDao.getGroups(section));

    return mv;
  }

  @RequestMapping(value="/mt.jsp", method=RequestMethod.POST)
  public ModelAndView moveTopic(
    ServletRequest request,
    @RequestParam int msgid,
    @RequestParam("moveto") int newgr
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    Message msg = messageDao.getById(msgid);

    if (msg.isDeleted()) {
      throw new AccessViolationException("Сообщение удалено");
    }

    Group newGrp = groupDao.getGroup(newgr);

    Connection db = null;

    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      Statement st1 = db.createStatement();

      String url = msg.getUrl();

      PreparedStatement movePst = db.prepareStatement("UPDATE topics SET groupid=?,lastmod=CURRENT_TIMESTAMP WHERE id=?");
      movePst.setInt(1, newGrp.getId());
      movePst.setInt(2, msgid);

      movePst.executeUpdate();

      if (url != null && !newGrp.isLinksAllowed() && !newGrp.isImagePostAllowed()) {
        String sSql = "UPDATE topics SET linktext=null, url=null WHERE id=" + msgid;

        String title = msg.getGroupTitle();
        String linktext = msg.getLinktext();

        st1.executeUpdate(sSql);

        /* if url is not null, update the topic text */
        PreparedStatement pst1 = db.prepareStatement("UPDATE msgbase SET message=message||? WHERE id=?");

        String link;
        if (msg.isLorcode()) {
          link = "\n[url=" + url + ']' + linktext + "[/url]\n";
        } else {
          link = "<br><a href=\"" + url + "\">" + linktext + "</a>\n<br>\n";
        }

        if (msg.isLorcode()) {
          pst1.setString(1, '\n' + link + "\n[i]Перемещено " + tmpl.getNick() + " из " + title + "[/i]\n");
        } else {
          pst1.setString(1, '\n' + link + "<br><i>Перемещено " + tmpl.getNick() + " из " + title + "</i>\n");
        }

        pst1.setInt(2, msgid);
        pst1.executeUpdate();
      }

      logger.info("topic " + msgid + " moved" +
              " by " + tmpl.getNick() + " from news/forum " + msg.getGroupTitle() + " to forum " + newGrp.getTitle());
      db.commit();

      return new ModelAndView(new RedirectView(msg.getLinkLastmod()));
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }

  @RequestMapping(value="/mt.jsp", method=RequestMethod.GET)
  public ModelAndView moveTopicFormForum(
    ServletRequest request,
    @RequestParam int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new IllegalAccessException("Not authorized");
    }

    ModelAndView mv = new ModelAndView("mtn");

    Message message = messageDao.getById(msgid);

    mv.getModel().put("message", message);

    Section section = sectionDao.getSection(Section.SECTION_FORUM);

    mv.getModel().put("groups", groupDao.getGroups(section));

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

    Message message = messageDao.getById(msgid);

    checkUncommitable(message);

    ModelAndView mv = new ModelAndView("uncommit");
    mv.getModel().put("message", message);
    mv.getModel().put("preparedMessage", prepareService.prepareMessage(message, true));

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

    Message message = messageDao.getById(msgid);

    checkUncommitable(message);

    messageDao.uncommit(message);

    logger.info("Отменено подтверждение сообщения " + msgid + " пользователем " + tmpl.getNick());

    return new ModelAndView("action-done", "message", "Подтверждение отменено");
  }

  private static void checkUncommitable(Message message) throws AccessViolationException {
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
    String info = Message.getPostScoreInfo(postscore);
    if (info.isEmpty()) {
      return "без ограничений";
    } else {
      return info;
    }
  }
}
