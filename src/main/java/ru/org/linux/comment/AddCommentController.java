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

package ru.org.linux.comment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.CaptchaService;
import ru.org.linux.auth.FloodProtector;
import ru.org.linux.auth.IPBlockDao;
import ru.org.linux.auth.IPBlockInfo;
import ru.org.linux.search.SearchQueueSender;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.site.Template;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.topic.TopicPermissionService;
import ru.org.linux.topic.TopicPrepareService;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserNotFoundException;
import ru.org.linux.user.UserPropertyEditor;
import ru.org.linux.util.ExceptionBindingErrorProcessor;
import ru.org.linux.util.ServletParameterException;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.bbcode.LorCodeService;
import ru.org.linux.util.formatter.ToLorCodeFormatter;
import ru.org.linux.util.formatter.ToLorCodeTexFormatter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.beans.PropertyEditorSupport;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Controller
public class AddCommentController {
  private static final Log logger = LogFactory.getLog(AddCommentController.class);

  @Autowired
  private SearchQueueSender searchQueueSender;

  @Autowired
  private CaptchaService captcha;

  @Autowired
  private FloodProtector dupeProtector;

  @Autowired
  private CommentDao commentDao;

  @Autowired
  private TopicDao messageDao;

  @Autowired
  private UserDao userDao;

  @Autowired
  private IPBlockDao ipBlockDao;

  @Autowired
  private CommentPrepareService prepareService;

  @Autowired
  private LorCodeService lorCodeService;

  @Autowired
  private ToLorCodeFormatter toLorCodeFormatter;

  @Autowired
  private ToLorCodeTexFormatter toLorCodeTexFormatter;

  @Autowired
  private TopicPermissionService permissionService;

  @Autowired
  private TopicPrepareService messagePrepareService;

  @RequestMapping(value = "/add_comment.jsp", method = RequestMethod.GET)
  public ModelAndView showFormReply(
    @ModelAttribute("add") @Valid CommentRequest add,
    HttpServletRequest request
  ) throws Exception {
    if (add.getTopic()==null) {
      throw new ServletParameterException("тема на задана");
    }

    Template tmpl = Template.getTemplate(request);
    
    Map<String, Object> params = new HashMap<String, Object>();

    if (add.getMode()==null) {
      add.setMode(tmpl.getFormatMode());
    }

    prepareReplyto(add, params, request);
    
    params.put("postscoreInfo", TopicPermissionService.getPostScoreInfo(add.getTopic().getPostScore()));

    return new ModelAndView("add_comment", params);
  }

  @RequestMapping("/comment-message.jsp")
  public ModelAndView showFormTopic(
    @ModelAttribute("add") @Valid CommentRequest add,
    HttpServletRequest request
  ) {
    Template tmpl = Template.getTemplate(request);

    if (add.getMode()==null) {
      add.setMode(tmpl.getFormatMode());
    }

    ModelAndView modelAndView = new ModelAndView(
      "comment-message",
      "preparedMessage",
      messagePrepareService.prepareTopic(add.getTopic(), false, request.isSecure(), tmpl.getCurrentUser())
    );

    IPBlockInfo ipBlockInfo = ipBlockDao.getBlockInfo(request.getRemoteAddr());
    modelAndView.addObject("ipBlockInfo", ipBlockInfo);
    return modelAndView;
  }

  private String processMessage(String msg, String mode) {
    if("ntobr".equals(mode)) {
      return toLorCodeFormatter.format(msg, true);
    } else {
      return toLorCodeTexFormatter.format(msg);
    }
  }

  @RequestMapping(value = "/add_comment.jsp", method = RequestMethod.POST)
  public ModelAndView addComment(
    @ModelAttribute("add") @Valid CommentRequest add,
    Errors errors,
    HttpServletRequest request
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (add.getMsg()==null) {
      errors.rejectValue("msg", null, "комментарий не задан");
      add.setMsg("");
    }

    if (add.getMode()==null) {
      add.setMode(tmpl.getFormatMode());
    }

    String msg = processMessage(add.getMsg(), add.getMode());

    HttpSession session = request.getSession();

    IPBlockInfo ipBlockInfo = ipBlockDao.getBlockInfo(request.getRemoteAddr());

    if (!add.isPreviewMode() &&
      (!tmpl.isSessionAuthorized() || ipBlockInfo.isCaptchaRequired())) {
      captcha.checkCaptcha(request, errors);
    }

    if (!add.isPreviewMode() && !errors.hasErrors() && !session.getId().equals(request.getParameter("session"))) {
      logger.info(String.format(
              "Flood protection (session variable differs: session=%s var=%s) ip=%s",
              session.getId(),
              request.getParameter("session"),
              request.getRemoteAddr()
      ));

      errors.reject(null, "сбой добавления, попробуйте еще раз");
    }

    Map<String, Object> formParams = new HashMap<String, Object>();

    prepareReplyto(add, formParams, request);

    User user;

    if (!tmpl.isSessionAuthorized()) {
      if (add.getNick() != null) {
        user = add.getNick();
      } else {
        user = userDao.getAnonymous();
      }

      if (add.getPassword()==null) {
        errors.reject(null, "Требуется авторизация");
      }
    } else {
      user = tmpl.getCurrentUser();
    }

    user.checkBlocked(errors);

    if (ipBlockInfo.isBlocked()) {
      ipBlockDao.checkBlockIP(ipBlockInfo, errors, user);
    }

    if (user.isAnonymous()) {
      if (msg.length() > 4096) {
        errors.rejectValue("msg", null, "Слишком большое сообщение");
      }
    } else {
      if (msg.length() > 8192) {
        errors.rejectValue("msg", null, "Слишком большое сообщение");
      }
    }

    Comment comment = null;

    if (add.getTopic()!=null) {
      formParams.put("postscoreInfo", TopicPermissionService.getPostScoreInfo(add.getTopic().getPostScore()));

      permissionService.checkCommentsAllowed(add.getTopic(), user, errors);

      String title = add.getTitle();

      if (title==null) {
        title="";
      }

      Integer replyto = add.getReplyto()!=null?add.getReplyto().getId():null;

      comment = new Comment(
              replyto,
              StringUtil.escapeHtml(title),
              add.getTopic().getId(),
              user.getId(),
              request.getHeader("user-agent"),
              request.getRemoteAddr()
      );

      formParams.put("comment", prepareService.prepareComment(comment, msg, request.isSecure()));
    }

    if (!add.isPreviewMode() && !errors.hasErrors()) {
      dupeProtector.checkDuplication(request.getRemoteAddr(), user.getScore() > 100, errors);
    }

    if (!add.isPreviewMode() && !errors.hasErrors() && comment != null) {
      Set<User> userRefs = lorCodeService.getReplierFromMessage(msg);

      int msgid = commentDao.saveNewMessage(comment, msg, userRefs);

      String logmessage = "Написан комментарий " + msgid + " ip:" + request.getRemoteAddr();
      if (request.getHeader("X-Forwarded-For") != null) {
        logmessage = logmessage + " XFF:" + request.getHeader(("X-Forwarded-For"));
      }

      logger.info(logmessage);

      // TODO надо разобраться с транзакциями и засунуть это в saveNewMessage
      searchQueueSender.updateComment(msgid);

      String returnUrl = "jump-message.jsp?msgid=" + add.getTopic().getId() + "&cid=" + msgid;

      return new ModelAndView(new RedirectView(returnUrl));
    } else {
          add.setMsg(StringUtil.escapeForceHtml(add.getMsg()));
    }
    ModelAndView modelAndView = new ModelAndView("add_comment", formParams);
    modelAndView.addObject("ipBlockInfo", ipBlockInfo);
    return modelAndView;
  }

  private void prepareReplyto(CommentRequest add, Map<String, Object> formParams, HttpServletRequest request) throws UserNotFoundException {
    if (add.getReplyto()!=null) {
      formParams.put("onComment", prepareService.prepareComment(add.getReplyto(), request.isSecure()));
    }
  }

  @InitBinder("add")
  public void requestValidator(WebDataBinder binder) {
    binder.setValidator(new CommentRequestValidator());

    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor());
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(Topic.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String text) throws IllegalArgumentException {
        try {
          setValue(messageDao.getById(Integer.parseInt(text)));
        } catch (MessageNotFoundException e) {
          throw new IllegalArgumentException(e);
        }
      }
    });

    binder.registerCustomEditor(Comment.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String text) throws IllegalArgumentException {
        if (text.isEmpty() || "0".equals(text)) {
          setValue(null);
          return;
        }

        try {
          setValue(commentDao.getById(Integer.parseInt(text)));
        } catch (MessageNotFoundException e) {
          throw new IllegalArgumentException(e);
        }
      }
    });

    binder.registerCustomEditor(User.class, new UserPropertyEditor(userDao));
  }
}
