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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import ru.org.linux.auth.CaptchaService;
import ru.org.linux.auth.FloodProtector;
import ru.org.linux.auth.IPBlockDao;
import ru.org.linux.auth.IPBlockInfo;
import ru.org.linux.csrf.CSRFNoAuto;
import ru.org.linux.csrf.CSRFProtectionService;
import ru.org.linux.gallery.Image;
import ru.org.linux.gallery.Screenshot;
import ru.org.linux.group.BadGroupException;
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
import ru.org.linux.spring.Configuration;
import ru.org.linux.tag.TagService;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserPropertyEditor;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ExceptionBindingErrorProcessor;
import ru.org.linux.util.UtilException;
import ru.org.linux.util.bbcode.LorCodeService;
import ru.org.linux.util.formatter.ToLorCodeFormatter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.beans.PropertyEditorSupport;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Controller
public class AddTopicController {
  private static final Log logger = LogFactory.getLog(AddTopicController.class);

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

  private UserDao userDao;

  @Autowired
  private TopicPrepareService prepareService;

  private TopicDao messageDao;
  private ToLorCodeFormatter toLorCodeFormatter;

  @Autowired
  private LorCodeService lorCodeService;

  @Autowired
  private GroupPermissionService groupPermissionService;

  @Autowired
  private Configuration configuration;

  @Autowired
  private AddTopicRequestValidator addTopicRequestValidator;

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

  @Autowired
  public void setUserDao(UserDao userDao) {
    this.userDao = userDao;
  }

  @Autowired
  public void setMessageDao(TopicDao messageDao) {
    this.messageDao = messageDao;
  }

  @Autowired
  public void setToLorCodeFormatter(ToLorCodeFormatter toLorCodeFormatter) {
    this.toLorCodeFormatter = toLorCodeFormatter;
  }

  @ModelAttribute("ipBlockInfo")
  private IPBlockInfo loadIPBlock(HttpServletRequest request) {
    return ipBlockDao.getBlockInfo(request.getRemoteAddr());
  }

  @RequestMapping(value = "/add.jsp", method = RequestMethod.GET)
  public ModelAndView add(@Valid @ModelAttribute("form") AddTopicRequest form, HttpServletRequest request) {
    Map<String, Object> params = new HashMap<String, Object>();

    Template tmpl = Template.getTemplate(request);

    if (form.getMode()==null) {
      form.setMode(tmpl.getFormatMode());
    }

    prepareModel(form, params, tmpl.getCurrentUser());

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
      return toLorCodeFormatter.format(msg, false);
    } else {
      return msg;
    }
  }

  private void prepareModel(AddTopicRequest form, Map<String, Object> params, User currentUser) {
    Group group = form.getGroup();

    if (group!=null) {
      params.put("group", group);
      params.put("postscoreInfo", groupPermissionService.getPostScoreInfo(group));
      Section section = sectionService.getSection(group.getSectionId());

      params.put("section", section);

      params.put("addportal", sectionService.getAddInfo(group.getSectionId()));
      params.put("imagepost", groupPermissionService.isImagePostingAllowed(section, currentUser));
    }

    params.put("topTags", tagService.getTopTags());
  }

  @RequestMapping(value="/add.jsp", method=RequestMethod.POST)
  @CSRFNoAuto
  public ModelAndView doAdd(
          HttpServletRequest request,
          @Valid @ModelAttribute("form") AddTopicRequest form,
          BindingResult errors,
          @ModelAttribute("ipBlockInfo") IPBlockInfo ipBlockInfo
  ) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();

    Template tmpl = Template.getTemplate(request);
    HttpSession session = request.getSession();

    String image = processUploadImage(request);

    Group group = form.getGroup();

    prepareModel(form, params, tmpl.getCurrentUser());

    Section section = null;

    if (group!=null) {
      section = sectionService.getSection(group.getSectionId());
    }

    User user;

    if (!tmpl.isSessionAuthorized()) {
      if (form.getNick() != null) {
        user = form.getNick();
      } else {
        user = userDao.getAnonymous();
      }

      if (form.getPassword()==null) {
        errors.rejectValue("password", null, "Требуется авторизация");
      }
    } else {
      user = tmpl.getCurrentUser();
    }

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
    TopicMenu topicMenu = null;

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

      PreparedTopic preparedTopic = prepareService.prepareTopicPreview(
              previewMsg,
              tagService.parseSanitizeTags(form.getTags()),
              poll,
              request.isSecure(),
              message,
              imageObject
      );

      params.put("message", preparedTopic);

      topicMenu = prepareService.getTopicMenu(
              preparedTopic,
              tmpl.getCurrentUser(),
              request.isSecure(),
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
      dupeProtector.checkDuplication(request.getRemoteAddr(), false, errors);
    }

    if (!form.isPreviewMode() && !errors.hasErrors() && group!=null && section!=null) {
      session.removeAttribute("image");

      Set<User> userRefs = lorCodeService.getReplierFromMessage(message);

      int msgid = messageDao.addMessage(
              request,
              form,
              message,
              group,
              user,
              scrn,
              previewMsg,
              userRefs
      );

      searchQueueSender.updateMessageOnly(msgid);

      Random random = new Random();

      String messageUrl = "view-message.jsp?msgid=" + msgid;

      if (!section.isPremoderated()) {
        return new ModelAndView(new RedirectView(messageUrl + "&nocache=" + random.nextInt()));
      }

      params.put("moderated", section.isPremoderated());
      params.put("url", messageUrl);

      return new ModelAndView("add-done-moderated", params);
    } else {
      return new ModelAndView("add", params);
    }
  }
  
  private static Poll preparePollPreview(AddTopicRequest form) {
    List<PollVariant> variants = new ArrayList<PollVariant>(form.getPoll().length);

    for (String item : form.getPoll()) {
      if (!Strings.isNullOrEmpty(item)) {
        variants.add(new PollVariant(0, item));
      }
    }

    return new Poll(0, 0, form.isMultiSelect(), false, variants);
  }

  @RequestMapping("/add-section.jsp")
  public ModelAndView showForm(@RequestParam("section") int sectionId) {
    Map<String, Object> params = new HashMap<String, Object>();

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

    binder.registerCustomEditor(User.class, new UserPropertyEditor(userDao));
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
   * @param session
   * @return <icon, image, previewImagePath> or null
   * @throws IOException
   * @throws UtilException
   */
  private Screenshot processUpload(
          HttpSession session,
          String image,
          Errors errors
  ) throws IOException, UtilException {
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
                configuration.getHTMLPathPrefix() + "/gallery/preview"
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
        File uploadedFile = File.createTempFile("preview", "", new File(configuration.getPathPrefix() + "/linux-storage/tmp/"));
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
}
