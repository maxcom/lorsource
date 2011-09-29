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

import com.google.common.collect.ImmutableMap;
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
import ru.org.linux.site.*;
import ru.org.linux.spring.dao.*;
import ru.org.linux.spring.validators.AddMessageRequestValidator;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.HTMLFormatter;
import ru.org.linux.util.UtilException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.beans.PropertyEditorSupport;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Controller
public class AddMessageController extends ApplicationObjectSupport {
  private SearchQueueSender searchQueueSender;
  private CaptchaService captcha;
  private DupeProtector dupeProtector;
  private IPBlockDao ipBlockDao;
  private GroupDao groupDao;
  private SectionDao sectionDao;
  private TagDao tagDao;
  private UserDao userDao;
  private PrepareService prepareService;
  private MessageDao messageDao;
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
  public void setDupeProtector(DupeProtector dupeProtector) {
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
  public void setSectionDao(SectionDao sectionDao) {
    this.sectionDao = sectionDao;
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
  public void setPrepareService(PrepareService prepareService) {
    this.prepareService = prepareService;
  }

  @Autowired
  public void setMessageDao(MessageDao messageDao) {
    this.messageDao = messageDao;
  }

  @RequestMapping(value = "/add.jsp", method = RequestMethod.GET)
  public ModelAndView add(@Valid @ModelAttribute("form") AddMessageRequest form, HttpServletRequest request) throws Exception {
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

    params.put("addportal", sectionDao.getAddInfo(group.getSectionId()));

    return new ModelAndView("add", params);
  }

  private static String processMessage(String msg, String mode) {
    if (msg == null) {
      return "";
    }

    if ("lorcode".equals(mode)) {
      return msg;
    } else {
      // Format message
      HTMLFormatter formatter = new HTMLFormatter(msg);

      formatter.setMaxLength(80);
      formatter.setOutputLorcode(true);

      if ("ntobr".equals(mode)) {
        formatter.enableNewLineMode();
      }

      return formatter.process();
    }
  }


  @RequestMapping(value="/add.jsp", method=RequestMethod.POST)
  public ModelAndView doAdd(
          HttpServletRequest request,
          @Valid @ModelAttribute("form") AddMessageRequest form,
          BindingResult errors
  ) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();

    Template tmpl = Template.getTemplate(request);
    HttpSession session = request.getSession();

    String image = processUploadImage(request, tmpl);

    Group group = form.getGroup();
    params.put("group", group);

    if (group!=null && group.isModerated()) {
      params.put("topTags", tagDao.getTopTags());
    }

    if (group!=null) {
      params.put("addportal", sectionDao.getAddInfo(group.getSectionId()));
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

    ipBlockDao.checkBlockIP(request.getRemoteAddr(), errors);

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
      scrn = processUpload(session, tmpl, image, errors);

      if (scrn!=null) {
        form.setLinktext("gallery/preview/" + scrn.getIconFile().getName());
        form.setUrl("gallery/preview/" + scrn.getMainFile().getName());
      } else {
        if (!errors.hasErrors()) {
          errors.reject(null, "Изображение отсутствует");
        }
      }
    }

    Message previewMsg = null;

    if (group!=null) {
      previewMsg = new Message(form, user, message, request.getRemoteAddr());
      params.put("message", prepareService.prepareMessage(previewMsg, true));
    }

    if (!form.isPreviewMode() && !errors.hasErrors() && !session.getId().equals(request.getParameter("session"))) {
      logger.info("Flood protection (session variable differs: " + session.getId() + ") " + request.getRemoteAddr());
      errors.reject(null, "сбой добавления");
    }

    if (!form.isPreviewMode() && !errors.hasErrors() && !Template.isSessionAuthorized(session)) {
      captcha.checkCaptcha(request, errors);
    }

    if (!form.isPreviewMode() && !errors.hasErrors()) {
      dupeProtector.checkDuplication(request.getRemoteAddr(), false, errors);
    }

    if (!form.isPreviewMode() && !errors.hasErrors() && group!=null) {
      session.removeAttribute("image");

      int msgid = messageDao.addMessage(request, form, tmpl, group, user, scrn, previewMsg);

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
      return new ModelAndView("add", params);
    }
  }

  @RequestMapping(value = "/add-section.jsp")
  public ModelAndView showForm(@RequestParam("section") int sectionId) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();

    params.put("sectionId", sectionId);

    Section section = sectionDao.getSection(sectionId);

    params.put("section", section);

    params.put("info", sectionDao.getAddInfo(section.getId()));

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
    binder.setValidator(new AddMessageRequestValidator());

    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor());
  }

  @ModelAttribute("modes")
  public Map<String, String> getModes() {
    return ImmutableMap.of("lorcode", "LORCODE", "ntobr", "User line break");
  }

  /**
   *
   * @param session
   * @param tmpl
   * @return <icon, image, previewImagePath> or null
   * @throws IOException
   * @throws UtilException
   */
  private Screenshot processUpload(
          HttpSession session,
          Template tmpl,
          String image,
          Errors errors
  ) throws IOException, UtilException {
    if (session==null) {
      return null;
    }

    Screenshot screenshot = null;

    if (image != null && !image.isEmpty()) {
      File uploadedFile = new File(image);

      try {
        screenshot = Screenshot.createScreenshot(
                uploadedFile,
                errors,
                tmpl.getObjectConfig().getHTMLPathPrefix() + "/gallery/preview"
        );

        if (screenshot != null) {
          logger.info("SCREEN: " + uploadedFile.getAbsolutePath() + "\nINFO: SCREEN: " + image);

          session.setAttribute("image", screenshot);
        }
      } catch (BadImageException e) {
        errors.reject(null, "Некорректное изображение: " + e.getMessage());
      }
    } else if (session.getAttribute("image") != null && !"".equals(session.getAttribute("image"))) {
      screenshot = (Screenshot) session.getAttribute("image");

      if (!screenshot.getMainFile().exists()) {
        screenshot = null;
      }
    }

    return screenshot;
  }

  private String processUploadImage(HttpServletRequest request, Template tmpl) throws IOException, ScriptErrorException {
    if (request instanceof MultipartHttpServletRequest) {
      MultipartFile multipartFile = ((MultipartRequest) request).getFile("image");
      if (multipartFile != null && !multipartFile.isEmpty()) {
        File uploadedFile = File.createTempFile("preview", "", new File(tmpl.getObjectConfig().getPathPrefix() + "/linux-storage/tmp/"));
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
