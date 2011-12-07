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

import com.google.common.base.Strings;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.search.SearchQueueSender;
import ru.org.linux.dao.GroupDao;
import ru.org.linux.dao.MessageDao;
import ru.org.linux.dao.PollDao;
import ru.org.linux.dao.TagCloudDao;
import ru.org.linux.dto.GroupDto;
import ru.org.linux.dto.MessageDto;
import ru.org.linux.dto.SectionDto;
import ru.org.linux.dto.UserDto;
import ru.org.linux.exception.AccessViolationException;
import ru.org.linux.exception.BadInputException;
import ru.org.linux.exception.PollNotFoundException;
import ru.org.linux.exception.UserErrorException;
import ru.org.linux.site.*;
import ru.org.linux.spring.validators.EditMessageRequestValidator;
import ru.org.linux.util.ExceptionBindingErrorProcessor;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;

@Controller
public class EditController {
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
  private TagCloudDao tagCloudDao;

  @Autowired
  private PollDao pollDao;

  @RequestMapping(value = "/commit.jsp", method = RequestMethod.GET)
  public ModelAndView showCommitForm(
      HttpServletRequest request,
      @RequestParam("msgid") int msgid,
      @ModelAttribute("form") EditMessageRequest form
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not authorized");
    }

    MessageDto messageDto = messageDao.getById(msgid);

    if (messageDto.isCommited()) {
      throw new UserErrorException("Сообщение уже подтверждено");
    }

    PreparedMessage preparedMessage = prepareService.prepareMessage(messageDto, false, request.isSecure());

    if (!preparedMessage.getSectionDto().isPremoderated()) {
      throw new UserErrorException("Раздел не премодерируемый");
    }

    ModelAndView mv = prepareModel(preparedMessage, form);

    mv.getModel().put("commit", true);

    return mv;
  }

  @RequestMapping(value = "/edit.jsp", method = RequestMethod.GET)
  public ModelAndView showEditForm(
      ServletRequest request,
      @RequestParam("msgid") int msgid,
      @ModelAttribute("form") EditMessageRequest form
  ) throws Exception {

    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    MessageDto messageDto = messageDao.getById(msgid);

    UserDto user = tmpl.getCurrentUser();

    PreparedMessage preparedMessage = prepareService.prepareMessage(messageDto, false, request.isSecure());

    if (!preparedMessage.isEditable(user)) {
      throw new AccessViolationException("это сообщение нельзя править");
    }

    return prepareModel(preparedMessage, form);
  }

  private ModelAndView prepareModel(
      PreparedMessage preparedMessage,
      EditMessageRequest form
  ) throws PollNotFoundException {
    Map<String, Object> params = new HashMap<String, Object>();

    MessageDto messageDto = preparedMessage.getMessage();

    params.put("message", messageDto);
    params.put("preparedMessage", preparedMessage);

    GroupDto groupDto = preparedMessage.getGroupDto();
    params.put("group", groupDto);

    params.put("groups", groupDao.getGroups(preparedMessage.getSectionDto()));

    params.put("newMsg", messageDto);
    params.put("newPreparedMessage", preparedMessage);

    List<EditInfoDTO> editInfoList = messageDao.getEditInfo(messageDto.getId());
    if (!editInfoList.isEmpty()) {
      params.put("editInfo", editInfoList.get(0));
    }

    params.put("commit", false);

    if (groupDto.isModerated()) {
      params.put("topTags", tagCloudDao.getTopTags());
    }

    if (messageDto.isHaveLink()) {
      form.setLinktext(messageDto.getLinktext());
      form.setUrl(messageDto.getUrl());
    }

    form.setTitle(StringEscapeUtils.unescapeHtml(messageDto.getTitle()));
    form.setMsg(messageDto.getMessage());

    if (messageDto.getSectionId() == SectionDto.SECTION_NEWS) {
      form.setMinor(messageDto.isMinor());
    }

    if (!preparedMessage.getTags().isEmpty()) {
      form.setTags(TagCloudDao.toString(preparedMessage.getTags()));
    }

    if (messageDto.isVotePoll()) {
      Poll poll = pollDao.getPollByTopicId(messageDto.getId());

      form.setPoll(PollVariant.toMap(pollDao.getPollVariants(poll, Poll.ORDER_ID)));

      form.setMultiselect(poll.isMultiSelect());
    }

    return new ModelAndView("edit", params);
  }

  @RequestMapping(value = "/edit.jsp", method = RequestMethod.POST)
  public ModelAndView edit(
      HttpServletRequest request,
      @RequestParam("msgid") int msgid,
      @RequestParam(value = "lastEdit", required = false) Long lastEdit,
      @RequestParam(value = "chgrp", required = false) Integer changeGroupId,
      @Valid @ModelAttribute("form") EditMessageRequest form,
      Errors errors
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    Map<String, Object> params = new HashMap<String, Object>();

    MessageDto messageDto = messageDao.getById(msgid);
    PreparedMessage preparedMessage = prepareService.prepareMessage(messageDto, false, request.isSecure());
    GroupDto groupDto = preparedMessage.getGroupDto();

    params.put("message", messageDto);
    params.put("preparedMessage", preparedMessage);
    params.put("group", groupDto);

    if (groupDto.isModerated()) {
      params.put("topTags", tagCloudDao.getTopTags());
    }

    params.put("groups", groupDao.getGroups(preparedMessage.getSectionDto()));

    UserDto user = tmpl.getCurrentUser();

    if (!preparedMessage.isEditable(user)) {
      throw new AccessViolationException("это сообщение нельзя править");
    }

    if (!messageDto.isExpired()) {
      String title = request.getParameter("title");
      if (title == null || title.trim().length() == 0) {
        throw new BadInputException("заголовок сообщения не может быть пустым");
      }
    }

    List<EditInfoDTO> editInfoList = messageDao.getEditInfo(messageDto.getId());

    boolean preview = request.getParameter("preview") != null;
    if (preview) {
      params.put("info", "Предпросмотр");
    }

    if (!editInfoList.isEmpty()) {
      EditInfoDTO dbEditInfo = editInfoList.get(0);
      params.put("editInfo", dbEditInfo);

      if (lastEdit == null || dbEditInfo.getEditdate().getTime() != lastEdit) {
        errors.reject(null, "Сообщение было отредактировано независимо");
      }
    }

    boolean commit = request.getParameter("commit") != null;

    if (commit) {
      user.checkCommit();
      if (messageDto.isCommited()) {
        throw new BadInputException("сообщение уже подтверждено");
      }
    }

    params.put("commit", !messageDto.isCommited() && preparedMessage.getSectionDto().isPremoderated() && user.isModerator());

    MessageDto newMsg = new MessageDto(groupDto, messageDto, form);

    boolean modified = false;

    if (!messageDto.getTitle().equals(newMsg.getTitle())) {
      modified = true;
    }

    if (!messageDto.getMessage().equals(newMsg.getMessage())) {
      modified = true;
    }

    if (messageDto.getLinktext() == null) {
      if (newMsg.getLinktext() != null) {
        modified = true;
      }
    } else if (!messageDto.getLinktext().equals(newMsg.getLinktext())) {
      modified = true;
    }

    if (messageDto.isHaveLink()) {
      if (messageDto.getUrl() == null) {
        if (newMsg.getUrl() != null) {
          modified = true;
        }
      } else if (!messageDto.getUrl().equals(newMsg.getUrl())) {
        modified = true;
      }
    }

    if (messageDto.isExpired() && modified) {
      throw new AccessViolationException("нельзя править устаревшие сообщения");
    }

    if (form.getMinor() != null && !tmpl.isModeratorSession()) {
      throw new AccessViolationException("вы не можете менять статус новости");
    }

    List<String> newTags = null;

    if (form.getTags() != null) {
      newTags = TagCloudDao.parseSanitizeTags(form.getTags());
    }

    if (changeGroupId != null) {
      if (messageDto.getGroupId() != changeGroupId) {
        GroupDto changeGroupDto = groupDao.getGroup(changeGroupId);

        int section = messageDto.getSectionId();

        if (changeGroupDto.getSectionId() != section) {
          throw new AccessViolationException("Can't move topics between sections");
        }
      }
    }

    PreparedPoll newPoll = null;

    if (messageDto.isVotePoll() && form.getPoll() != null && tmpl.isModeratorSession()) {
      Poll poll = pollDao.getPollByTopicId(messageDto.getId());

      PreparedPoll orig = prepareService.preparePoll(poll);

      List<PollVariant> newVariants = new ArrayList<PollVariant>();

      for (PollVariant v : pollDao.getPollVariants(poll, Poll.ORDER_ID)) {
        String label = form.getPoll().get(v.getId());

        if (!Strings.isNullOrEmpty(label)) {
          newVariants.add(new PollVariant(v.getId(), label, v.getVotes()));
        }
      }

      for (String label : form.getNewPoll()) {
        if (!Strings.isNullOrEmpty(label)) {
          newVariants.add(new PollVariant(0, label, 0));
        }
      }

      newPoll = new PreparedPoll(poll, orig.getMaximumValue(), pollDao.getCountUsers(poll), newVariants);
    }

    if (!preview && !errors.hasErrors()) {
      boolean changed = messageDao.updateAndCommit(
          newMsg,
          messageDto,
          user,
          newTags,
          commit,
          changeGroupId,
          form.getBonus(),
          newPoll != null ? newPoll.getVariants() : null,
          form.isMultiselect()
      );

      if (changed || commit) {
        searchQueueSender.updateMessageOnly(newMsg.getId());

        if (commit) {
          feedPinger.pingFeedburner();
        }

        return new ModelAndView(new RedirectView(messageDto.getLinkLastmod()));
      } else {
        errors.reject(null, "Нет изменений");
      }
    }

    params.put("newMsg", newMsg);

    params.put("newPreparedMessage", prepareService.prepareMessage(newMsg, newTags, newPoll, request.isSecure()));

    return new ModelAndView("edit", params);
  }

  public void setCommitController(FeedPinger feedPinger) {
    this.feedPinger = feedPinger;
  }

  @InitBinder("form")
  public void requestValidator(WebDataBinder binder) {
    binder.setValidator(new EditMessageRequestValidator());

    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor());
  }

}
