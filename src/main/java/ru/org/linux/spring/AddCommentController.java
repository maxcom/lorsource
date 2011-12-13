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
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.search.SearchQueueSender;
import ru.org.linux.site.*;
import ru.org.linux.spring.dao.CommentDao;
import ru.org.linux.spring.dao.IPBlockDao;
import ru.org.linux.spring.dao.MessageDao;
import ru.org.linux.spring.dao.UserDao;
import ru.org.linux.spring.validators.AddCommentRequestValidator;
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
public class AddCommentController extends ApplicationObjectSupport {
  private SearchQueueSender searchQueueSender;
  private CaptchaService captcha;
  private DupeProtector dupeProtector;
  private CommentDao commentDao;
  private MessageDao messageDao;
  private UserDao userDao;
  private IPBlockDao ipBlockDao;
  private CommentPrepareService prepareService;
  private LorCodeService lorCodeService;
  private ToLorCodeFormatter toLorCodeFormatter;
  private ToLorCodeTexFormatter toLorCodeTexFormatter;

  @Autowired
  private MessagePrepareService messagePrepareService;

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
  public void setCommentDao(CommentDao commentDao) {
    this.commentDao = commentDao;
  }

  @Autowired
  public void setMessageDao(MessageDao messageDao) {
    this.messageDao = messageDao;
  }

  @Autowired
  public void setUserDao(UserDao userDao) {
    this.userDao = userDao;
  }

  @Autowired
  public void setIpBlockDao(IPBlockDao ipBlockDao) {
    this.ipBlockDao = ipBlockDao;
  }

  @Autowired
  public void setPrepareService(CommentPrepareService prepareService) {
    this.prepareService = prepareService;
  }

  @Autowired
  public void setLorCodeService(LorCodeService lorCodeService) {
    this.lorCodeService = lorCodeService;
  }

  @Autowired
  public void setToLorCodeFormatter(ToLorCodeFormatter toLorCodeFormatter) {
    this.toLorCodeFormatter = toLorCodeFormatter;
  }

  @Autowired
  public void setToLorCodeTexFormatter(ToLorCodeTexFormatter toLorCodeTexFormatter) {
    this.toLorCodeTexFormatter = toLorCodeTexFormatter;
  }

  @RequestMapping(value = "/add_comment.jsp", method = RequestMethod.GET)
  public ModelAndView showFormReply(
    @ModelAttribute("add") @Valid AddCommentRequest add,
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

    return new ModelAndView("add_comment", params);
  }

  @RequestMapping("/comment-message.jsp")
  public ModelAndView showFormTopic(
    @ModelAttribute("add") @Valid AddCommentRequest add,
    HttpServletRequest request
  ) {
    Template tmpl = Template.getTemplate(request);

    if (add.getMode()==null) {
      add.setMode(tmpl.getFormatMode());
    }

    return new ModelAndView(
            "comment-message",
            "preparedMessage",
            messagePrepareService.prepareMessage(add.getTopic(), false, request.isSecure())
    );
  }

  private String processMessage(String msg, String mode) {
    if ("lorcode".equals(mode)) {
      return msg;
    }else if("ntobr".equals(mode)) {
      return toLorCodeFormatter.format(msg, true);
    } else {
      return toLorCodeTexFormatter.format(msg, true);
    }
  }

  @RequestMapping(value = "/add_comment.jsp", method = RequestMethod.POST)
  public ModelAndView addComment(
    @ModelAttribute("add") @Valid AddCommentRequest add,
    Errors errors,
    HttpServletRequest request
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (add.getMsg()==null || add.getMsg().trim().isEmpty()) {
      errors.rejectValue("msg", null, "комментарий не может быть пустым");
      add.setMsg("");
    }

    if (add.getMode()==null) {
      add.setMode(tmpl.getFormatMode());
    }

    String msg = processMessage(add.getMsg(), add.getMode());

    HttpSession session = request.getSession();

    if (!add.isPreviewMode() && !Template.isSessionAuthorized(session)) {
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

    ipBlockDao.checkBlockIP(request.getRemoteAddr(), errors);

    Map<String, Object> formParams = new HashMap<String, Object>();

    prepareReplyto(add, formParams, request);

    User user;

    if (!Template.isSessionAuthorized(session)) {
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
      add.getTopic().checkCommentsAllowed(user, errors);

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
    }

    return new ModelAndView("add_comment", formParams);
  }

  private void prepareReplyto(AddCommentRequest add, Map<String, Object> formParams, HttpServletRequest request) throws UserNotFoundException {
    if (add.getReplyto()!=null) {
      formParams.put("onComment", prepareService.prepareComment(add.getReplyto(), request.isSecure()));
    }
  }

  @InitBinder("add")
  public void requestValidator(WebDataBinder binder) {
    binder.setValidator(new AddCommentRequestValidator());

    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor());
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(Message.class, new PropertyEditorSupport() {
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
