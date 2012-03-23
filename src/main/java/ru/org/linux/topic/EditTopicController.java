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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.group.GroupPermissionService;
import ru.org.linux.poll.Poll;
import ru.org.linux.poll.PollDao;
import ru.org.linux.poll.PollNotFoundException;
import ru.org.linux.poll.PollVariant;
import ru.org.linux.search.SearchQueueSender;
import ru.org.linux.section.Section;
import ru.org.linux.site.BadInputException;
import ru.org.linux.site.Template;
import ru.org.linux.spring.FeedPinger;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserErrorException;
import ru.org.linux.user.UserNotFoundException;
import ru.org.linux.util.ExceptionBindingErrorProcessor;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class EditTopicController {
  @Autowired
  private SearchQueueSender searchQueueSender;

  @Autowired
  private FeedPinger feedPinger;

  @Autowired
  private TopicDao messageDao;

  @Autowired
  private TopicPrepareService messagePrepareService;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private TagService tagService;

  @Autowired
  private PollDao pollDao;

  @Autowired
  private GroupPermissionService permissionService;
  
  @Autowired
  private MsgbaseDao msgbaseDao;
  
  @Autowired
  private UserDao userDao;

  @Autowired
  private EditTopicRequestValidator editTopicRequestValidator;

  @RequestMapping(value = "/commit.jsp", method = RequestMethod.GET)
  public ModelAndView showCommitForm(
    HttpServletRequest request,
    @RequestParam("msgid") int msgid,
    @ModelAttribute("form") EditTopicRequest form
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not authorized");
    }

    Topic message = messageDao.getById(msgid);

    if (message.isCommited()) {
      throw new UserErrorException("Сообщение уже подтверждено");
    }

    PreparedTopic preparedMessage = messagePrepareService.prepareTopic(message, false, request.isSecure(), tmpl.getCurrentUser());

    if (!preparedMessage.getSection().isPremoderated()) {
      throw new UserErrorException("Раздел не премодерируемый");
    }

    ModelAndView mv = prepareModel(preparedMessage, form, tmpl.getCurrentUser());

    mv.getModel().put("commit", true);

    return mv;
  }

  @RequestMapping(value = "/edit.jsp", method = RequestMethod.GET)
  public ModelAndView showEditForm(
    ServletRequest request,
    @RequestParam("msgid") int msgid,
    @ModelAttribute("form") EditTopicRequest form
  ) throws Exception {

    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    Topic message = messageDao.getById(msgid);

    User user = tmpl.getCurrentUser();

    PreparedTopic preparedMessage = messagePrepareService.prepareTopic(message, false, request.isSecure(), tmpl.getCurrentUser());

    if (!permissionService.isEditable(preparedMessage, user) && !permissionService.isTagsEditable(preparedMessage, user)) {
      throw new AccessViolationException("это сообщение нельзя править");
    }

    return prepareModel(preparedMessage, form, tmpl.getCurrentUser());
  }

  private ModelAndView prepareModel(
    PreparedTopic preparedMessage,
    EditTopicRequest form,
    User currentUser
  ) throws PollNotFoundException {
    Map<String, Object> params = new HashMap<String, Object>();

    final Topic message = preparedMessage.getMessage();

    params.put("message", message);
    params.put("preparedMessage", preparedMessage);

    Group group = preparedMessage.getGroup();
    params.put("group", group);

    params.put("groups", groupDao.getGroups(preparedMessage.getSection()));

    params.put("newMsg", message);
    params.put("newPreparedMessage", preparedMessage);

    params.put("editable", permissionService.isEditable(preparedMessage, currentUser));
    boolean tagsEditable = permissionService.isTagsEditable(preparedMessage, currentUser);
    params.put("tagsEditable", tagsEditable);

    List<EditInfoDto> editInfoList = messageDao.getEditInfo(message.getId());
    if (!editInfoList.isEmpty()) {
      params.put("editInfo", editInfoList.get(0));

      ImmutableSet<User> editors = getEditors(message, editInfoList);

      ImmutableMap.Builder<Integer,Integer> editorBonus = ImmutableMap.builder();
      for (User editor : editors) {
        editorBonus.put(editor.getId(), 1);
      }

      form.setEditorBonus(editorBonus.build());
      
      params.put("editors", editors);
    }

    params.put("commit", false);

    if (tagsEditable) {
      params.put("topTags", tagService.getTopTags());
    }

    if (message.isHaveLink()) {
      form.setLinktext(message.getLinktext());
      form.setUrl(message.getUrl());
    }

    form.setTitle(StringEscapeUtils.unescapeHtml(message.getTitle()));
    form.setMsg(msgbaseDao.getMessageText(message.getId()).getText());

    if (message.getSectionId() == Section.SECTION_NEWS) {
      form.setMinor(message.isMinor());
    }

    if (!preparedMessage.getTags().isEmpty()) {
      form.setTags(TagService.toString(preparedMessage.getTags()));
    }

    if (group.isPollPostAllowed()) {
      Poll poll = pollDao.getPollByTopicId(message.getId());

      form.setPoll(PollVariant.toMap(poll.getVariants()));

      form.setMultiselect(poll.isMultiSelect());
    }

    return new ModelAndView("edit", params);
  }

  private ImmutableSet<User> getEditors(final Topic message, List<EditInfoDto> editInfoList) {
    return ImmutableSet.copyOf(
            Iterables.transform(
                    Iterables.filter(editInfoList, new Predicate<EditInfoDto>() {
                      @Override
                      public boolean apply(EditInfoDto input) {
                        return input.getEditor() != message.getUid();
                      }
                    }),
                    new Function<EditInfoDto, User>() {
                      @Override
                      public User apply(EditInfoDto input) {
                        try {
                          return userDao.getUserCached(input.getEditor());
                        } catch (UserNotFoundException e) {
                          throw new RuntimeException(e);
                        }
                      }
                    })
    );
  }

  @RequestMapping(value = "/edit.jsp", method = RequestMethod.POST)
  public ModelAndView edit(
    HttpServletRequest request,
    @RequestParam("msgid") int msgid,
    @RequestParam(value="lastEdit", required=false) Long lastEdit,
    @RequestParam(value="chgrp", required=false) Integer changeGroupId,
    @Valid @ModelAttribute("form") EditTopicRequest form,
    Errors errors
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    Map<String, Object> params = new HashMap<String, Object>();

    final Topic message = messageDao.getById(msgid);
    PreparedTopic preparedMessage = messagePrepareService.prepareTopic(message, false, request.isSecure(), tmpl.getCurrentUser());
    Group group = preparedMessage.getGroup();

    User user = tmpl.getCurrentUser();

    boolean tagsEditable = permissionService.isTagsEditable(preparedMessage, user);
    boolean editable = permissionService.isEditable(preparedMessage, user);

    if (!editable && !tagsEditable) {
      throw new AccessViolationException("это сообщение нельзя править");
    }

    params.put("message", message);
    params.put("preparedMessage", preparedMessage);
    params.put("group", group);
    params.put("tagsEditable", tagsEditable);
    params.put("editable", editable);

    if (tagsEditable) {
      params.put("topTags", tagService.getTopTags());
    }

    params.put("groups", groupDao.getGroups(preparedMessage.getSection()));

    if (editable) {
      String title = request.getParameter("title");
      if (title == null || title.trim().isEmpty()) {
        throw new BadInputException("заголовок сообщения не может быть пустым");
      }
    }

    List<EditInfoDto> editInfoList = messageDao.getEditInfo(message.getId());

    boolean preview = request.getParameter("preview") != null;
    if (preview) {
      params.put("info", "Предпросмотр");
    }

    if (!editInfoList.isEmpty()) {
      EditInfoDto dbEditInfo = editInfoList.get(0);
      params.put("editInfo", dbEditInfo);

      if (lastEdit == null || dbEditInfo.getEditdate().getTime()!=lastEdit) {
        errors.reject(null, "Сообщение было отредактировано независимо");
      }
    }

    boolean commit = request.getParameter("commit") != null;

    if (commit) {
      user.checkCommit();
      if (message.isCommited()) {
        throw new BadInputException("сообщение уже подтверждено");
      }
    }

    params.put("commit", !message.isCommited() && preparedMessage.getSection().isPremoderated() && user.isModerator());

    Topic newMsg = new Topic(group, message, form);

    boolean modified = false;

    if (!message.getTitle().equals(newMsg.getTitle())) {
      modified = true;
    }
    
    if (form.getMsg()!=null) {
      String oldText = msgbaseDao.getMessageText(message.getId()).getText();
  
      if (!oldText.equals(form.getMsg())) {
        modified = true;
      }
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

    if (!editable && modified) {
      throw new AccessViolationException("нельзя править это сообщение, только теги");
    }

    if (form.getMinor()!=null && !tmpl.isModeratorSession()) {
      throw new AccessViolationException("вы не можете менять статус новости");
    }

    List<String> newTags = null;

    if (form.getTags()!=null) {
      newTags = tagService.parseSanitizeTags(form.getTags());
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

    Poll newPoll = null;

    if (group.isPollPostAllowed() && form.getPoll() != null && tmpl.isModeratorSession()) {
      Poll poll = pollDao.getPollByTopicId(message.getId());

      List<PollVariant> newVariants = new ArrayList<PollVariant>();

      for (PollVariant v : poll.getVariants()) {
        String label = form.getPoll().get(v.getId());

        if (!Strings.isNullOrEmpty(label)) {
          newVariants.add(new PollVariant(v.getId(), label));
        }
      }

      for (String label : form.getNewPoll()) {
        if (!Strings.isNullOrEmpty(label)) {
          newVariants.add(new PollVariant(0, label));
        }
      }
      
      newPoll = poll.createNew(newVariants);
    }

    String newText;

    if (form.getMsg() != null) {
      newText = form.getMsg();
    } else {
      newText = msgbaseDao.getMessageText(message.getId()).getText();
    }

    if (form.getEditorBonus() != null) {
      ImmutableSet<Integer> editors = ImmutableSet.copyOf(
              Iterables.transform(
                      Iterables.filter(editInfoList, new Predicate<EditInfoDto>() {
                        @Override
                        public boolean apply(EditInfoDto input) {
                          return input.getEditor() != message.getUid();
                        }
                      }), new Function<EditInfoDto, Integer>() {
                @Override
                public Integer apply(EditInfoDto input) {
                  return input.getEditor();
                }
              }));

      for (int userid : form.getEditorBonus().keySet()) {
        if (!editors.contains(userid)) {
          errors.reject("editorBonus", "некорректный корректор?!");
        }
      }
    }

    if (!preview && !errors.hasErrors()) {
      boolean changed = messageDao.updateAndCommit(
              newMsg,
              message,
              user,
              newTags,
              newText,
              commit,
              changeGroupId,
              form.getBonus(),
              newPoll!=null?newPoll.getVariants():null,
              form.isMultiselect(),
              form.getEditorBonus()
      );

      if (changed || commit) {
        searchQueueSender.updateMessageOnly(newMsg.getId());

        if (commit) {
          feedPinger.pingFeedburner();
        }

        return new ModelAndView(new RedirectView(message.getLinkLastmod()));
      } else {
        errors.reject(null, "Нет изменений");
      }
    }

    params.put("newMsg", newMsg);

    params.put(
            "newPreparedMessage",
            messagePrepareService.prepareTopicPreview(
                    newMsg,
                    newTags,
                    newPoll,
                    request.isSecure(),
                    newText
            )
    );

    return new ModelAndView("edit", params);
  }

  @InitBinder("form")
  public void requestValidator(WebDataBinder binder) {
    binder.setValidator(editTopicRequestValidator);

    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor());
  }

}
