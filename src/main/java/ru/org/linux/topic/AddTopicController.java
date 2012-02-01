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
import org.springframework.context.support.ApplicationObjectSupport;
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
import ru.org.linux.auth.*;
import ru.org.linux.gallery.Screenshot;
import ru.org.linux.group.BadGroupException;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.poll.Poll;
import ru.org.linux.poll.PollVariant;
import ru.org.linux.search.SearchQueueSender;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.ScriptErrorException;
import ru.org.linux.site.Template;
import ru.org.linux.spring.Configuration;
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
public class AddTopicController extends ApplicationObjectSupport {
  private static final Log logger = LogFactory.getLog(AddTopicController.class);

  private SearchQueueSender searchQueueSender;
  private CaptchaService captcha;
  private FloodProtector dupeProtector;
  private IPBlockDao ipBlockDao;
  private GroupDao groupDao;
  @Autowired
  private SectionService sectionService;

  private TagDao tagDao;
  private UserDao userDao;

  @Autowired
  private TopicPrepareService prepareService;

  private TopicDao messageDao;
  private ToLorCodeFormatter toLorCodeFormatter;

  @Autowired
  private LorCodeService lorCodeService;

  @Autowired
  private Configuration configuration;

  public static final int MAX_MESSAGE_LENGTH_ANONYMOUS = 4096;
  public static final int MAX_MESSAGE_LENGTH = 16384;

  @Autowired
  public void setSearchQueueSender(SearchQueueSender searchQueueSender) {
    this.searchQueueSender = searchQueueSender;
  }

  @Autowired
  public void setCaptcha(CaptchaService captcha) {
    this.captcha = captcha;
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
  public void setTagDao(TagDao tagDao) {
    this.tagDao = tagDao;
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

  @RequestMapping(value = "/add.jsp", method = RequestMethod.GET)
  public ModelAndView add(@Valid @ModelAttribute("form") AddTopicRequest form, HttpServletRequest request) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();

    Template tmpl = Template.getTemplate(request);

    if (form.getMode()==null) {
      form.setMode(tmpl.getFormatMode());
    }

    Group group = form.getGroup();

    if (!group.isTopicPostingAllowed(tmpl.getCurrentUser())) {
      throw new AccessViolationException("Не достаточно прав для постинга тем в эту группу");
    }

    params.put("group", group);

    if (group.isModerated()) {
      params.put("topTags", tagDao.getTopTags());
    }

    params.put("addportal", sectionService.getAddInfo(group.getSectionId()));
    IPBlockInfo ipBlockInfo = ipBlockDao.getBlockInfo(request.getRemoteAddr());
    params.put("ipBlockInfo", ipBlockInfo);
    return new ModelAndView("add", params);
  }

  private String processMessage(String msg, String mode) {
    if (msg == null) {
      return "";
    }

    if ("lorcode".equals(mode)) {
      return msg;
    } else {
      return toLorCodeFormatter.format(msg, false);
    }
  }


  @RequestMapping(value="/add.jsp", method=RequestMethod.POST)
  public ModelAndView doAdd(
          HttpServletRequest request,
          @Valid @ModelAttribute("form") AddTopicRequest form,
          BindingResult errors
  ) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();

    Template tmpl = Template.getTemplate(request);
    HttpSession session = request.getSession();

    String image = processUploadImage(request);

    Group group = form.getGroup();
    params.put("group", group);

    if (group!=null && group.isModerated()) {
      params.put("topTags", tagDao.getTopTags());
    }

    if (group!=null) {
      params.put("addportal", sectionService.getAddInfo(group.getSectionId()));
    }

    User user;

    if (!Template.isSessionAuthorized(session)) {
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

    if (user.isAnonymous()) {
      errors.reject(null, "Анонимный пользователь");
    }

    IPBlockInfo ipBlockInfo = ipBlockDao.getBlockInfo(request.getRemoteAddr());
    if (ipBlockInfo.isBlocked() && ! ipBlockInfo.isAllowRegistredPosting()) {
      ipBlockDao.checkBlockIP(ipBlockInfo, errors, user);
    }

    if (group!=null && !group.isTopicPostingAllowed(user)) {
      errors.reject(null, "Не достаточно прав для постинга тем в эту группу");
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

    if (group!=null && group.isImagePostAllowed()) {
      scrn = processUpload(session, image, errors);

      if (scrn!=null) {
        form.setLinktext("gallery/preview/" + scrn.getIconFile().getName());
        form.setUrl("gallery/preview/" + scrn.getMainFile().getName());
      } else {
        form.setLinktext(null);
        form.setUrl(null);
        if (!errors.hasErrors()) {
          errors.reject(null, "Изображение отсутствует");
        }
      }
    }

    Poll poll = null;
    
    if (group.isPollPostAllowed()) {
      poll = preparePollPreview(form);
    }

    Topic previewMsg = null;

    if (group!=null) {
      previewMsg = new Topic(form, user, request.getRemoteAddr());
      params.put("message", prepareService.prepareTopicPreview(previewMsg, TagDao.parseSanitizeTags(form.getTags()), poll, request.isSecure(), message));
    }

    if (!form.isPreviewMode() && !errors.hasErrors() && !session.getId().equals(request.getParameter("session"))) {
      logger.info("Flood protection (session variable differs: " + session.getId() + ") " + request.getRemoteAddr());
      errors.reject(null, "сбой добавления");
    }

    if (!form.isPreviewMode() && !errors.hasErrors() && !Template.isSessionAuthorized(session)
      || ipBlockInfo.isCaptchaRequired()) {
      captcha.checkCaptcha(request, errors);
    }

    if (!form.isPreviewMode() && !errors.hasErrors()) {
      dupeProtector.checkDuplication(request.getRemoteAddr(), false, errors);
    }

    if (!form.isPreviewMode() && !errors.hasErrors() && group!=null) {
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

      if (!group.isModerated()) {
        return new ModelAndView(new RedirectView(messageUrl + "&nocache=" + random.nextInt()));
      }

      params.put("moderated", group.isModerated());
      params.put("url", messageUrl);

      return new ModelAndView("add-done-moderated", params);
    } else {
      params.put("ipBlockInfo", ipBlockInfo);
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
  public ModelAndView showForm(@RequestParam("section") int sectionId) throws Exception {
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
    binder.setValidator(new AddTopicRequestValidator());

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
