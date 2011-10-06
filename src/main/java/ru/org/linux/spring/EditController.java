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
import ru.org.linux.spring.dao.TagDao;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class EditController extends ApplicationObjectSupport {
  @Autowired
  private SearchQueueSender searchQueueSender;

  @Autowired
  private FeedPinger feedPinger;

  @Autowired
  private MessageDao messageDao;

  @Autowired
  private PrepareService prepareService;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private TagDao tagDao;

  @RequestMapping(value = "/commit.jsp", method = RequestMethod.GET)
  public ModelAndView showCommitForm(
    HttpServletRequest request,
    @RequestParam("msgid") int msgid)
  throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not authorized");
    }

    Message message = messageDao.getById(msgid);

    if (message.isCommited()) {
      throw new UserErrorException("Сообщение уже подтверждено");
    }

    PreparedMessage preparedMessage = prepareService.prepareMessage(message, true);

    if (!preparedMessage.getSection().isPremoderated()) {
      throw new UserErrorException("Раздел не премодерируемый");
    }

    ModelAndView mv = prepareModel(preparedMessage);

    mv.getModel().put("commit", true);

    return mv;
  }

  @RequestMapping(value = "/edit.jsp", method = RequestMethod.GET)
  public ModelAndView showEditForm(
    ServletRequest request,
    @RequestParam("msgid") int msgid)
    throws Exception {

    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    Message message = messageDao.getById(msgid);

    User user = tmpl.getCurrentUser();

    PreparedMessage preparedMessage = prepareService.prepareMessage(message, true);

    if (!preparedMessage.isEditable(user)) {
      throw new AccessViolationException("это сообщение нельзя править");
    }

    return prepareModel(preparedMessage);
  }

  private ModelAndView prepareModel(
    PreparedMessage preparedMessage
  ) {
    Map<String, Object> params = new HashMap<String, Object>();

    Message message = preparedMessage.getMessage();

    params.put("message", message);
    params.put("preparedMessage", preparedMessage);

    Group group = preparedMessage.getGroup();
    params.put("group", group);

    params.put("groups", groupDao.getGroups(preparedMessage.getSection()));

    params.put("newMsg", message);
    params.put("newPreparedMessage", preparedMessage);

    List<EditInfoDTO> editInfoList = messageDao.getEditInfo(message.getId());
    if (!editInfoList.isEmpty()) {
      params.put("editInfo", editInfoList.get(0));
    }

    params.put("commit", false);

    if (group.isModerated()) {
      params.put("topTags", tagDao.getTopTags());
    }

    return new ModelAndView("edit", params);
  }

  @RequestMapping(value = "/edit.jsp", method = RequestMethod.POST)
  public ModelAndView edit(
    HttpServletRequest request,
    @RequestParam("msgid") int msgid,
    @RequestParam(value="lastEdit", required=false) Long lastEdit,
    @RequestParam(value="bonus", required=false, defaultValue="3") int bonus,
    @RequestParam(value="chgrp", required=false) Integer changeGroupId,
    @RequestParam(value="minor", required=false) Boolean minor
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    Map<String, Object> params = new HashMap<String, Object>();

    Message message = messageDao.getById(msgid);
    PreparedMessage preparedMessage = prepareService.prepareMessage(message, true);
    Group group = preparedMessage.getGroup();

    params.put("message", message);
    params.put("preparedMessage", preparedMessage);
    params.put("group", group);

    if (group.isModerated()) {
      params.put("topTags", tagDao.getTopTags());
    }

    params.put("groups", groupDao.getGroups(preparedMessage.getSection()));

    User user = tmpl.getCurrentUser();

    if (!preparedMessage.isEditable(user)) {
      throw new AccessViolationException("это сообщение нельзя править");
    }

    if (!message.isExpired()) {
      String title = request.getParameter("title");
      if (title == null || title.trim().length() == 0) {
        throw new BadInputException("заголовок сообщения не может быть пустым");
      }
    }

    List<EditInfoDTO> editInfoList = messageDao.getEditInfo(message.getId());

    boolean preview = request.getParameter("preview") != null;
    if (preview) {
      params.put("info", "Предпросмотр");
    }

    if (!editInfoList.isEmpty()) {
      EditInfoDTO dbEditInfo = editInfoList.get(0);
      params.put("editInfo", dbEditInfo);

      if (lastEdit == null || dbEditInfo.getEditdate().getTime()!=lastEdit) {
        params.put("info", "Сообщение было отредактировано независимо");
        preview = true;
      }
    }

    boolean commit = request.getParameter("commit") != null;

    if (commit) {
      user.checkCommit();
      if (message.isCommited()) {
        throw new BadInputException("сообщение уже подтверждено");
      }
    }

    params.put("commit", !message.isCommited() && preparedMessage.getSection().isPremoderated() && user.canModerate());

    Message newMsg = new Message(group, message, request);

    boolean modified = false;

    if (!message.getTitle().equals(newMsg.getTitle())) {
      modified = true;
    }

    if (minor==null) {
      minor = message.isMinor();
    }

    if (minor!=message.isMinor()) {
      modified = true;
    }

    if (!message.getMessage().equals(newMsg.getMessage())) {
      modified = true;
    }

    if (message.getLinktext() == null) {
      if (newMsg.getLinktext() != null) {
        modified = true;
      }
    } else if (!message.getLinktext().equals(newMsg.getLinktext())) {
      modified = true;
    }

    if (message.isHaveLink()) {
      if (message.getUrl() == null) {
        if (newMsg.getUrl() != null) {
          modified = true;
        }
      } else if (!message.getUrl().equals(newMsg.getUrl())) {
        modified = true;
      }
    }

    if (message.isExpired() && (modified)) {
      throw new AccessViolationException("нельзя править устаревшие сообщения");
    }

    List<String> newTags = null;

    if (request.getParameter("tags")!=null) {
      newTags = TagDao.parseTags(request.getParameter("tags"));
    }

    if (changeGroupId != null) {
      if (message.getGroupId() != changeGroupId) {
        Group changeGroup = groupDao.getGroup(changeGroupId);

        int section = message.getSectionId();

        if (changeGroup.getSectionId() != section) {
          throw new AccessViolationException("Can't move topics between sections");
        }
      }
    }

    Connection db = null;

    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      if (!preview) {
        PreparedStatement pst = db.prepareStatement("UPDATE topics SET linktext=?, url=?, minor=? WHERE id=?");

        pst.setString(1, newMsg.getLinktext());
        pst.setString(2, newMsg.getUrl());
        pst.setBoolean(3, minor);
        pst.setInt(4, message.getId());

        if (newMsg.updateMessageText(db, user, newTags)) {
          modified = true;
        }

        if (modified) {
          pst.executeUpdate();
        }

        params.put("modified", modified);

        if (commit) {
          if (changeGroupId != null) {
            if (message.getGroupId() != changeGroupId) {
              message.changeGroup(db, changeGroupId);
            }
          }

          message.commit(db, user, bonus);
        }

        if (modified || commit) {
          if (modified) {
            logger.info("сообщение " + message.getId() + " исправлено " + user.getNick());
          }

          searchQueueSender.updateMessageOnly(newMsg.getId());

          db.commit();

          if (commit) {
            feedPinger.pingFeedburner();
          }

          return new ModelAndView(new RedirectView(message.getLinkLastmod()));
        } else {
          params.put("info", "nothing changed");
        }
      }

      params.put("newMsg", newMsg);
      params.put("newPreparedMessage", new PreparedMessage(db, newMsg, newTags));

      return new ModelAndView("edit", params);
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  public void setCommitController(FeedPinger feedPinger) {
    this.feedPinger = feedPinger;
  }
}
