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

package ru.org.linux.user;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.auth.CaptchaService;
import ru.org.linux.auth.IPBlockDao;
import ru.org.linux.auth.IPBlockInfo;
import ru.org.linux.site.Template;
import ru.org.linux.spring.Configuration;
import ru.org.linux.util.*;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.Date;
import java.util.Properties;

@SuppressWarnings("ProhibitedExceptionDeclared")
@Controller
public class RegisterController extends ApplicationObjectSupport {
  private CaptchaService captcha;
  private IPBlockDao ipBlockDao;

  @Autowired
  private UserDao userDao;

  @Autowired
  private EmailService emailService;

  @Autowired
  private Configuration configuration;

  @Autowired
  public void setCaptcha(CaptchaService captcha) {
    this.captcha = captcha;
  }

  @Autowired
  public void setIpBlockDao(IPBlockDao ipBlockDao) {
    this.ipBlockDao = ipBlockDao;
  }

  @RequestMapping(value = "/register.jsp", method = RequestMethod.GET)
  public ModelAndView register(
    @ModelAttribute("form") RegisterRequest form,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
      response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
      return new ModelAndView("register");
  }

  @RequestMapping(value = "/register.jsp", method = RequestMethod.POST)
  public ModelAndView doRegister(
    HttpServletRequest request,
    @Valid @ModelAttribute("form") RegisterRequest form,
    Errors errors,
    @RequestParam(required=false) String oldpass
  ) throws Exception {
    HttpSession session = request.getSession();
    Template tmpl = Template.getTemplate(request);

    String nick;

    nick = form.getNick();

    if (Strings.isNullOrEmpty(nick)) {
      errors.rejectValue("nick", null, "не задан nick");
    }

    if (nick!=null && !StringUtil.checkLoginName(nick)) {
      errors.rejectValue("nick", null, "некорректное имя пользователя");
    }

    if (nick!=null && nick.length() > User.MAX_NICK_LENGTH) {
      errors.rejectValue("nick", null, "слишком длинное имя пользователя");
    }

    String password = Strings.emptyToNull(form.getPassword());

    if (password!=null && password.equalsIgnoreCase(nick)) {
      errors.reject(null, "пароль не может совпадать с логином");
    }

    InternetAddress mail = null;

    if (!Strings.isNullOrEmpty(form.getEmail())) {
      try {
        mail = new InternetAddress(form.getEmail());
      } catch (AddressException e) {
        errors.rejectValue("email", null, "Некорректный e-mail: " + e.getMessage());
      }
    }

    if (Strings.isNullOrEmpty(password)) {
      errors.reject(null, "пароль не может быть пустым");
    }

    String name = Strings.emptyToNull(form.getName());

    if (name != null) {
      name = StringUtil.escapeHtml(name);
    }

    captcha.checkCaptcha(request, errors);

    if (session.getAttribute("register-visited") == null) {
      logger.info("Flood protection (not visited register.jsp) " + request.getRemoteAddr());
      errors.reject(null, "Временная ошибка, попробуйте еще раз");
    }

    IPBlockInfo ipBlockInfo = ipBlockDao.getBlockInfo(request.getRemoteAddr());
    ipBlockDao.checkBlockIP(ipBlockInfo, errors, tmpl.getCurrentUser());

    if (userDao.isUserExists(nick)) {
      errors.rejectValue("nick", null, "пользователь " + nick + " уже существует");
    }

    if (mail != null && userDao.getByEmail(mail.getAddress(), false) != null) {
      errors.rejectValue("email", null, "пользователь с таким e-mail уже зарегистрирован. " +
              "Если вы забыли параметры своего аккаунта, воспользуйтесь формой восстановления пароля");
    }

    if (!errors.hasErrors()) {
      int userid = userDao.createUser(name, nick, password, "", mail, "", "");

      String logmessage = "Зарегистрирован пользователь " + nick + " (id=" + userid + ") " + LorHttpUtils.getRequestIP(request);
      logger.info(logmessage);

      emailService.sendEmail(nick, mail.getAddress(), true);
    } else {
      return new ModelAndView("register");
    }

    return new ModelAndView("action-done", "message", "Добавление пользователя прошло успешно. Ожидайте письма с кодом активации.");
  }


  @RequestMapping(value="/activate.jsp", method= RequestMethod.GET)
  public ModelAndView activateForm() {
    return new ModelAndView("activate");
  }

  @RequestMapping(value = "/activate.jsp", method = RequestMethod.POST)
  public ModelAndView activate(
    HttpServletRequest request,
    @RequestParam String activation
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized!");
    }

    User user = tmpl.getCurrentUser();

    String newEmail = userDao.getNewEmail(user);

    if (newEmail == null) {
      throw new AccessViolationException("new_email == null?!");
    }

    String regcode = user.getActivationCode(configuration.getSecret(), newEmail);

    if (!regcode.equals(activation)) {
      throw new AccessViolationException("Bad activation code");
    }

    userDao.acceptNewEmail(user);

    return new ModelAndView(new RedirectView("/people/" + user.getNick() + "/profile"));
  }

  @InitBinder("form")
  public void requestValidator(WebDataBinder binder) {
    binder.setValidator(new RegisterRequestValidator());
    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor());
  }
}
