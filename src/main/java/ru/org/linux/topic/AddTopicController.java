/*
 * Copyright 1998-2022 Linux.org.ru
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

import akka.actor.ActorRef;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;
import ru.org.linux.auth.*;
import ru.org.linux.csrf.CSRFNoAuto;
import ru.org.linux.csrf.CSRFProtectionService;
import ru.org.linux.gallery.Image;
import ru.org.linux.gallery.ImageService;
import ru.org.linux.gallery.UploadedImagePreview;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.group.GroupPermissionService;
import ru.org.linux.markup.MarkupPermissions;
import ru.org.linux.markup.MessageTextService;
import ru.org.linux.poll.Poll;
import ru.org.linux.poll.PollVariant;
import ru.org.linux.realtime.RealtimeEventHub;
import ru.org.linux.search.SearchQueueSender;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.Template;
import ru.org.linux.markup.MarkupType;
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.tag.TagName;
import ru.org.linux.tag.TagService;
import ru.org.linux.user.User;
import ru.org.linux.user.UserErrorException;
import ru.org.linux.user.UserPropertyEditor;
import ru.org.linux.user.UserService;
import ru.org.linux.util.ExceptionBindingErrorProcessor;
import ru.org.linux.util.markdown.MarkdownFormatter;
import scala.Tuple2;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.beans.PropertyEditorSupport;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Controller
public class AddTopicController {
  private final SearchQueueSender searchQueueSender;

  private final CaptchaService captcha;

  private FloodProtector dupeProtector;
  private IPBlockDao ipBlockDao;
  private final GroupDao groupDao;
  private final SectionService sectionService;

  private final TagService tagService;

  private final UserService userService;

  private final TopicPrepareService prepareService;

  private final GroupPermissionService groupPermissionService;

  private final AddTopicRequestValidator addTopicRequestValidator;

  private final ImageService imageService;

  private final TopicService topicService;

  private final ActorRef realtimeHubWS;
  private final MarkdownFormatter renderService;

  private static final int MAX_MESSAGE_LENGTH_ANONYMOUS = 8196;
  private static final int MAX_MESSAGE_LENGTH = 65536;

  public AddTopicController(SearchQueueSender searchQueueSender, CaptchaService captcha, SectionService sectionService,
                            TagService tagService, UserService userService, TopicPrepareService prepareService,
                            GroupPermissionService groupPermissionService,
                            AddTopicRequestValidator addTopicRequestValidator, ImageService imageService,
                            TopicService topicService, @Qualifier("realtimeHubWS") ActorRef realtimeHubWS,
                            MarkdownFormatter renderService, GroupDao groupDao) {
    this.searchQueueSender = searchQueueSender;
    this.captcha = captcha;
    this.sectionService = sectionService;
    this.tagService = tagService;
    this.userService = userService;
    this.prepareService = prepareService;
    this.groupPermissionService = groupPermissionService;
    this.addTopicRequestValidator = addTopicRequestValidator;
    this.imageService = imageService;
    this.topicService = topicService;
    this.realtimeHubWS = realtimeHubWS;
    this.renderService = renderService;
    this.groupDao = groupDao;
  }

  @Autowired
  public void setDupeProtector(FloodProtector dupeProtector) {
    this.dupeProtector = dupeProtector;
  }

  @Autowired
  public void setIpBlockDao(IPBlockDao ipBlockDao) {
    this.ipBlockDao = ipBlockDao;
  }

  @ModelAttribute("ipBlockInfo")
  private IPBlockInfo loadIPBlock(HttpServletRequest request) {
    return ipBlockDao.getBlockInfo(request.getRemoteAddr());
  }

  @RequestMapping(value = "/add.jsp", method = RequestMethod.GET)
  public ModelAndView add(@Valid @ModelAttribute("form") AddTopicRequest form, HttpServletRequest request) {
    Template tmpl = Template.getTemplate();

    if (form.getMode()==null) {
      form.setMode(tmpl.getFormatMode());
    }

    Map<String, Object> params = new HashMap<>(prepareModel(form.getGroup(), AuthUtil.getCurrentUser(), form.getGroup().getSectionId(), request));

    Group group = form.getGroup();

    if (tmpl.isSessionAuthorized() && !groupPermissionService.isTopicPostingAllowed(group, AuthUtil.getCurrentUser())) {
      ModelAndView errorView = new ModelAndView("errors/good-penguin");
      errorView.addObject("msgHeader", "Недостаточно прав для постинга тем в эту группу");
      errorView.addObject("msgMessage", groupPermissionService.getPostScoreInfo(group));

      return errorView;
    }

    return new ModelAndView("add", params);
  }

  private ImmutableMap<String, Object> prepareModel(@Nullable Group group, @Nullable User currentUser, int sectionId,
                                                    HttpServletRequest request) {
    ImmutableMap.Builder<String, Object> params = ImmutableMap.builder();

    Section section = sectionService.getSection(sectionId);

    try {
      URL helpResource = request.getServletContext().getResource("/help/new-topic-" + Section.getUrlName(sectionId) + ".md");
      if (helpResource != null) {
        String helpRawText = IOUtils.toString(helpResource, StandardCharsets.UTF_8);

        String addInfo = renderService.renderToHtml(helpRawText, false);

        params.put("addportal", addInfo);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    params.put("sectionId", sectionId);
    params.put("section", section);

    if (group!=null) {
      params.put("group", group);
      params.put("postscoreInfo", groupPermissionService.getPostScoreInfo(group));
      params.put("showAllowAnonymous", groupPermissionService.enableAllowAnonymousCheckbox(group, currentUser));

      params.put("imagepost", groupPermissionService.isImagePostingAllowed(section, currentUser));
    }

    return params.build();
  }

  private User postingUser(Template tmpl, AddTopicRequest form) {
    User user;

    if (!tmpl.isSessionAuthorized()) {
      if (form.getNick() != null) {
        user = form.getNick();
      } else {
        user = userService.getAnonymous();
      }
    } else {
      user = AuthUtil.getCurrentUser();
    }

    return user;
  }

  @RequestMapping(value="/add.jsp", method=RequestMethod.POST)
  @CSRFNoAuto
  public ModelAndView doAdd(
          HttpServletRequest request,
          @Valid @ModelAttribute("form") AddTopicRequest form,
          BindingResult errors,
          @ModelAttribute("ipBlockInfo") IPBlockInfo ipBlockInfo
  ) {
    Template tmpl = Template.getTemplate();
    HttpSession session = request.getSession();

    Group group = form.getGroup();

    Map<String, Object> params = new HashMap<>(prepareModel(form.getGroup(), AuthUtil.getCurrentUser(), form.getGroup().getSectionId(), request));

    Section section = null;

    if (group!=null) {
      section = sectionService.getSection(group.getSectionId());
    }

    User user = postingUser(tmpl, form);

    user.checkBlocked(errors);
    user.checkFrozen(errors);

    IPBlockDao.checkBlockIP(ipBlockInfo, errors, user);

    if (group!=null && !groupPermissionService.isTopicPostingAllowed(group, user)) {
      errors.reject(null, "Недостаточно прав для постинга тем в эту группу");
    }

    if (form.getMode()==null) {
      form.setMode(tmpl.getFormatMode());
    }

    if (group!=null && !groupPermissionService.enableAllowAnonymousCheckbox(group, user)) {
      form.setAllowAnonymous(true);
    }

    if (MarkupPermissions.allowedFormatsJava(AuthUtil.getCurrentUser()).stream().map(MarkupType::formId).noneMatch(s -> s.equals(form.getMode()))) {
      errors.rejectValue("mode", null, "Некорректный режим разметки");
      form.setMode(MarkupType.Lorcode$.MODULE$.formId());
    }

    MessageText message = MessageTextService.processPostingText(Strings.nullToEmpty(form.getMsg()), form.getMode());

    if (user.isAnonymous()) {
      if (message.text().length() > MAX_MESSAGE_LENGTH_ANONYMOUS) {
        errors.rejectValue("msg", null, "Слишком большое сообщение");
      }
    } else {
      if (message.text().length() > MAX_MESSAGE_LENGTH) {
        errors.rejectValue("msg", null, "Слишком большое сообщение");
      }
    }

    UploadedImagePreview imagePreview = null;

    if (section!=null && groupPermissionService.isImagePostingAllowed(section, user)) {
      if (groupPermissionService.isTopicPostingAllowed(group, user)) {
        File image = imageService.processUploadImage(request);

        imagePreview = imageService.processUpload(user, session, image, errors);
      }

      if (section.isImagepost() && imagePreview == null && !errors.hasErrors()) {
        errors.reject(null, "Изображение отсутствует");
      }
    }

    Poll poll = null;
    
    if (section!=null && section.isPollPostAllowed()) {
      poll = preparePollPreview(form);
    }

    Topic previewMsg = null;

    if (group!=null) {
      previewMsg = Topic.fromAddRequest(form, user, request.getRemoteAddr());

      Image imageObject = null;

      if (imagePreview!=null) {
        imageObject = new Image(0, 0, "gallery/preview/" + imagePreview.mainFile().getName());
      }

      List<String> tagNames = TagName.parseAndSanitizeTags(form.getTags());

      if (!groupPermissionService.canCreateTag(section, user)) {
        List<String> newTags = tagService.getNewTags(tagNames);

        if (!newTags.isEmpty()) {
          errors.rejectValue("tags", null, "Вы не можете создавать новые теги ("+ TagService.tagsToString(newTags)+")");
        }
      }

      PreparedTopic preparedTopic = prepareService.prepareTopicPreview(
              previewMsg,
              TagService.namesToRefs(tagNames),
              poll,
              message,
              imageObject
      );

      params.put("message", preparedTopic);

      TopicMenu topicMenu = prepareService.getTopicMenu(
              preparedTopic,
              AuthUtil.getCurrentUser(),
              tmpl.getProf(),
              true
      );

      params.put("topicMenu", topicMenu);
    }

    if (!form.isPreviewMode() && !errors.hasErrors()) {
      CSRFProtectionService.checkCSRF(request, errors);
    }

    if (!form.isPreviewMode() && !errors.hasErrors() && !tmpl.isSessionAuthorized()
      || ipBlockInfo.isCaptchaRequired()) {
      captcha.checkCaptcha(request, errors);
    }

    if (!form.isPreviewMode() && !errors.hasErrors()) {
      dupeProtector.checkDuplication(FloodProtector.Action.ADD_TOPIC, request.getRemoteAddr(), user.getScore() >= 100, errors);
    }

    if (!form.isPreviewMode() && !errors.hasErrors() && group != null) {
      return createNewTopic(request, form, session, group, params, section, user, message, imagePreview, previewMsg);
    } else {
      return new ModelAndView("add", params);
    }
  }

  private ModelAndView createNewTopic(HttpServletRequest request, AddTopicRequest form, HttpSession session,
                                      Group group, Map<String, Object> params, Section section, User user,
                                      MessageText message, UploadedImagePreview scrn, Topic previewMsg) {
    session.removeAttribute("image");

    Tuple2<Integer, Set<Integer>> result = topicService.addMessage(
            request,
            form,
            message,
            group,
            user,
            scrn,
            previewMsg
    );

    int msgid = result._1;

    if (!previewMsg.isDraft())  {
      searchQueueSender.updateMessageOnly(msgid);
      RealtimeEventHub.notifyEvents(realtimeHubWS, result._2);
    }

    String topicUrl = previewMsg.withId(msgid).getLink();

    if (!section.isPremoderated() || previewMsg.isDraft()) {
      return new ModelAndView(new RedirectView(topicUrl, false, false));
    } else {
      params.put("url", topicUrl);
      params.put("authorized", AuthUtil.isSessionAuthorized());

      return new ModelAndView("add-done-moderated", params);
    }
  }

  private static Poll preparePollPreview(AddTopicRequest form) {
    List<PollVariant> variants = new ArrayList<>(form.getPoll().length);

    for (String item : form.getPoll()) {
      if (!Strings.isNullOrEmpty(item)) {
        variants.add(new PollVariant(0, item));
      }
    }

    return Poll.apply(0, 0, form.isMultiSelect(), variants);
  }

  @RequestMapping("/add-section.jsp")
  public ModelAndView showForm(
          @RequestParam("section") int sectionId,
          @RequestParam(value="tag", required = false) String tag,
          HttpServletRequest request
  ) throws UserErrorException {
    Map<String, Object> params = new HashMap<>(prepareModel(null, AuthUtil.getCurrentUser(), sectionId, request));

    if (tag!=null) {
      TagName.checkTag(tag);
      params.put("tag", tag);
    }

    Section section = sectionService.getSection(sectionId);

    params.put("groups", groupDao.getGroups(section));

    return new ModelAndView("add-section", params);
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(Group.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String text) throws IllegalArgumentException {
        setValue(groupDao.getGroup(Integer.parseInt(text)));
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

    binder.registerCustomEditor(User.class, new UserPropertyEditor(userService));
  }

  @InitBinder("form")
  public void requestValidator(WebDataBinder binder) {
    binder.setValidator(addTopicRequestValidator);

    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor());
  }

  @ModelAttribute("modes")
  public Map<String, String> getModes() {
    Template tmpl = Template.getTemplate();

    return MessageTextService.postingModeSelector(AuthUtil.getCurrentUser(), tmpl.getFormatMode());
  }

  public static String getAddUrl(Section section, String tag) {
    UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/add-section.jsp");
    builder.queryParam("section", section.getId());
    builder.queryParam("tag", tag);

    return builder.build().toUriString();
  }

  public static String getAddUrl(Section section) {
    UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/add-section.jsp");
    builder.queryParam("section", section.getId());

    return builder.build().toUriString();
  }
}
