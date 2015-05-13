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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;
import ru.org.linux.auth.CaptchaService;
import ru.org.linux.auth.FloodProtector;
import ru.org.linux.auth.IPBlockDao;
import ru.org.linux.auth.IPBlockInfo;
import ru.org.linux.csrf.CSRFNoAuto;
import ru.org.linux.csrf.CSRFProtectionService;
import ru.org.linux.gallery.Image;
import ru.org.linux.gallery.Screenshot;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.group.GroupPermissionService;
import ru.org.linux.poll.Poll;
import ru.org.linux.poll.PollVariant;
import ru.org.linux.search.SearchQueueSender;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.ScriptErrorException;
import ru.org.linux.site.Template;
import ru.org.linux.spring.SiteConfig;
import ru.org.linux.tag.TagName;
import ru.org.linux.tag.TagService;
import ru.org.linux.user.User;
import ru.org.linux.user.UserErrorException;
import ru.org.linux.user.UserPropertyEditor;
import ru.org.linux.user.UserService;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ExceptionBindingErrorProcessor;
import ru.org.linux.util.formatter.ToLorCodeFormatter;
import ru.org.linux.util.formatter.ToLorCodeTexFormatter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.beans.PropertyEditorSupport;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class AddTopicController {
  private static final Logger logger = LoggerFactory.getLogger(AddTopicController.class);

  private SearchQueueSender searchQueueSender;

  @Autowired
  private CaptchaService captcha;

  private FloodProtector dupeProtector;
  private IPBlockDao ipBlockDao;
  private GroupDao groupDao;
  @Autowired
  private SectionService sectionService;

  @Autowired
  private TagService tagService;

  @Autowired
  private UserService userService;

  @Autowired
  private TopicPrepareService prepareService;

  @Autowired
  private ToLorCodeFormatter toLorCodeFormatter;

  @Autowired
  private ToLorCodeTexFormatter toLorCodeTexFormatter;

  @Autowired
  private GroupPermissionService groupPermissionService;

  @Autowired
  private SiteConfig siteConfig;

  @Autowired
  private AddTopicRequestValidator addTopicRequestValidator;

  @Autowired
  private TopicService topicService;

  public static final int MAX_MESSAGE_LENGTH_ANONYMOUS = 8196;
  public static final int MAX_MESSAGE_LENGTH = 32768;

  @Autowired
  public void setSearchQueueSender(SearchQueueSender searchQueueSender) {
    this.searchQueueSender = searchQueueSender;
  }

  @Autowired
  public void setDupeProtector(FloodProtector dupeProtector) {
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

  @ModelAttribute("ipBlockInfo")
  private IPBlockInfo loadIPBlock(HttpServletRequest request) {
    return ipBlockDao.getBlockInfo(request.getRemoteAddr());
  }

  @RequestMapping(value = "/add.jsp", method = RequestMethod.GET)
  public ModelAndView add(
          @Valid @ModelAttribute("form") AddTopicRequest form,
          HttpServletRequest request
  ) {
    Template tmpl = Template.getTemplate(request);

    if (form.getMode()==null) {
      form.setMode(tmpl.getFormatMode());
    }

    Map<String, Object> params = new HashMap<>();
    params.putAll(prepareModel(form, tmpl.getCurrentUser()));

    Group group = form.getGroup();

    if (tmpl.isSessionAuthorized() && !groupPermissionService.isTopicPostingAllowed(group, tmpl.getCurrentUser())) {
      ModelAndView errorView = new ModelAndView("errors/good-penguin");
      errorView.addObject("msgHeader", "Недостаточно прав для постинга тем в эту группу");
      errorView.addObject("msgMessage", groupPermissionService.getPostScoreInfo(group));

      return errorView;
    }

    return new ModelAndView("add", params);
  }

  private String processMessage(String msg, String mode) {
    if (msg == null) {
      return "";
    }

    if ("ntobr".equals(mode)) {
      return toLorCodeFormatter.format(msg);
    } else {
      return toLorCodeTexFormatter.format(msg);
    }
  }

  private ImmutableMap<String, Object> prepareModel(AddTopicRequest form, User currentUser) {
    ImmutableMap.Builder<String, Object> params = ImmutableMap.builder();

    Group group = form.getGroup();

    if (group!=null) {
      params.put("group", group);
      params.put("postscoreInfo", groupPermissionService.getPostScoreInfo(group));
      Section section = sectionService.getSection(group.getSectionId());

      params.put("section", section);

      String addInfo = sectionService.getAddInfo(group.getSectionId());

      if (addInfo!=null) {
        params.put("addportal", addInfo);
      }

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
      user = tmpl.getCurrentUser();
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
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);
    HttpSession session = request.getSession();

    String image = processUploadImage(request);

    Group group = form.getGroup();

    Map<String, Object> params = new HashMap<>();

    params.putAll(prepareModel(form, tmpl.getCurrentUser()));

    Section section = null;

    if (group!=null) {
      section = sectionService.getSection(group.getSectionId());
    }

    User user = postingUser(tmpl, form);

    user.checkBlocked(errors);

    IPBlockDao.checkBlockIP(ipBlockInfo, errors, user);

    if (group!=null && !groupPermissionService.isTopicPostingAllowed(group, user)) {
      errors.reject(null, "Недостаточно прав для постинга тем в эту группу");
    }

    String message = processMessage(form.getMsg(), form.getMode());

    if (user.isAnonymous()) {
      if (message.length() > MAX_MESSAGE_LENGTH_ANONYMOUS) {
        errors.rejectValue("msg", null, "Слишком большое сообщение");
      }
    } else {
      if (message.length() > MAX_MESSAGE_LENGTH) {
        errors.rejectValue("msg", null, "Слишком большое сообщение");
      }
    }

    Screenshot scrn = null;

    if (section!=null && groupPermissionService.isImagePostingAllowed(section, tmpl.getCurrentUser())) {
      scrn = processUpload(session, image, errors);

      if (section.isImagepost() && scrn == null && !errors.hasErrors()) {
        errors.reject(null, "Изображение отсутствует");
      }
    }

    Poll poll = null;
    
    if (section!=null && section.isPollPostAllowed()) {
      poll = preparePollPreview(form);
    }

    Topic previewMsg = null;

    if (group!=null) {
      previewMsg = new Topic(form, user, request.getRemoteAddr());

      Image imageObject = null;

      if (scrn!=null) {
        imageObject = new Image(
                0,
                0,
                "gallery/preview/" + scrn.getMainFile().getName(),
                "gallery/preview/" + scrn.getIconFile().getName()
        );
      }

      List<String> tagNames = TagName.parseAndSanitizeTags(form.getTags());

      if (!groupPermissionService.canCreateTag(section, tmpl.getCurrentUser())) {
        List<String> newTags = tagService.getNewTags(tagNames);

        if (!newTags.isEmpty()) {
          errors.rejectValue("tags", null, "Вы не можете создавать новые теги ("+ TagService.tagsToString(newTags)+")");
        }
      }

      PreparedTopic preparedTopic = prepareService.prepareTopicPreview(
              previewMsg,
              TagService.namesToRefs(tagNames),
              poll,
              request.isSecure(),
              message,
              imageObject
      );

      params.put("message", preparedTopic);

      TopicMenu topicMenu = prepareService.getTopicMenu(
              preparedTopic,
              tmpl.getCurrentUser(),
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
      return createNewTopic(request, form, session, group, params, section, user, message, scrn, previewMsg);
    } else {
      return new ModelAndView("add", params);
    }
  }

  private ModelAndView createNewTopic(HttpServletRequest request, AddTopicRequest form, HttpSession session, Group group, Map<String, Object> params, Section section, User user, String message, Screenshot scrn, Topic previewMsg) throws IOException, ScriptErrorException {
    session.removeAttribute("image");

    int msgid = topicService.addMessage(
            request,
            form,
            message,
            group,
            user,
            scrn,
            previewMsg
    );

    if (!previewMsg.isDraft())  {
      searchQueueSender.updateMessageOnly(msgid);
    }

    String messageUrl = "view-message.jsp?msgid=" + msgid;

    if (!section.isPremoderated() || previewMsg.isDraft()) {
      return new ModelAndView(new RedirectView(messageUrl));
    }

    params.put("moderated", section.isPremoderated());
    params.put("url", messageUrl);

    return new ModelAndView("add-done-moderated", params);
  }

  private static Poll preparePollPreview(AddTopicRequest form) {
    List<PollVariant> variants = new ArrayList<>(form.getPoll().length);

    for (String item : form.getPoll()) {
      if (!Strings.isNullOrEmpty(item)) {
        variants.add(new PollVariant(0, item));
      }
    }

    return new Poll(0, 0, form.isMultiSelect(), false, variants);
  }

  @RequestMapping("/add-section.jsp")
  public ModelAndView showForm(
          @RequestParam("section") int sectionId,
          @RequestParam(value="tag", required = false) String tag
  ) throws UserErrorException {
    Map<String, Object> params = new HashMap<>();

    if (tag!=null) {
      TagName.checkTag(tag);
      params.put("tag", tag);
    }

    params.put("sectionId", sectionId);

    Section section = sectionService.getSection(sectionId);

    params.put("section", section);

    params.put("info", sectionService.getAddInfo(section.getId()));

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
    return ImmutableMap.of("lorcode", "LORCODE", "ntobr", "User line break");
  }

  /**
   *
   *
   * @return <icon, image, previewImagePath> or null
   * @throws IOException
   */
  private Screenshot processUpload(
          HttpSession session,
          String image,
          Errors errors
  ) throws IOException {
    if (session==null) {
      return null;
    }

    Screenshot screenShot = null;

    if (image != null && !image.isEmpty()) {
      File uploadedFile = new File(image);

      try {
        screenShot = Screenshot.createScreenshot(
                uploadedFile,
                errors,
                siteConfig.getHTMLPathPrefix() + "/gallery/preview"
        );

        if (screenShot != null) {
          logger.info("SCREEN: " + uploadedFile.getAbsolutePath() + "\nINFO: SCREEN: " + image);

          session.setAttribute("image", screenShot);
        }
      } catch (BadImageException e) {
        errors.reject(null, "Некорректное изображение: " + e.getMessage());
      }
    } else if (session.getAttribute("image") != null && !"".equals(session.getAttribute("image"))) {
      screenShot = (Screenshot) session.getAttribute("image");

      if (!screenShot.getMainFile().exists()) {
        screenShot = null;
      }
    }

    return screenShot;
  }

  private String processUploadImage(HttpServletRequest request) throws IOException, ScriptErrorException {
    if (request instanceof MultipartHttpServletRequest) {
      MultipartFile multipartFile = ((MultipartRequest) request).getFile("image");
      if (multipartFile != null && !multipartFile.isEmpty()) {
        File uploadedFile = File.createTempFile("preview", "", new File(siteConfig.getPathPrefix() + "/linux-storage/tmp/"));
        String image = uploadedFile.getPath();
        if ((uploadedFile.canWrite() || uploadedFile.createNewFile())) {
          try {
            logger.debug("Transfering upload to: " + image);
            multipartFile.transferTo(uploadedFile);
            return image;
          } catch (Exception e) {
            throw new ScriptErrorException("Failed to write uploaded file", e);
          }
        } else {
          logger.info("Bad target file name: " + image);
        }
      }
    }

    return null;
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
