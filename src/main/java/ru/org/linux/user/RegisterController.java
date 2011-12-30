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
import ru.org.linux.util.ExceptionBindingErrorProcessor;
import ru.org.linux.util.LorHttpUtils;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.URLUtil;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.servlet.http.HttpServletRequest;
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
    HttpServletRequest request
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (tmpl.isSessionAuthorized()) {
      User user = tmpl.getCurrentUser();

      user.checkAnonymous();

      UserInfo userInfo = userDao.getUserInfoClass(user);

      ModelAndView mv = new ModelAndView("register-update");

      form.setEmail(user.getEmail());
      form.setUrl(userInfo.getUrl());
      form.setTown(userInfo.getTown());
      form.setName(user.getName());
      form.setInfo(StringEscapeUtils.unescapeHtml(userDao.getUserInfo(user)));

      return mv;
    } else {
      return new ModelAndView("register");
    }
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

    boolean changeMode = "change".equals(request.getParameter("mode"));

    String nick;

    if (changeMode) {
      if (!tmpl.isSessionAuthorized()) {
        throw new AccessViolationException("Not authorized");
      }

      nick = tmpl.getNick();
    } else {
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
    }

    String password = Strings.emptyToNull(form.getPassword());

    if (password!=null && password.equalsIgnoreCase(nick)) {
      errors.reject(null, "пароль не может совпадать с логином");
    }

    InternetAddress mail = null;

    if (Strings.isNullOrEmpty(form.getEmail())) {
      errors.rejectValue("email", null, "Не указан e-mail");
    } else {
      try {
        mail = new InternetAddress(form.getEmail());
      } catch (AddressException e) {
        errors.rejectValue("email", null, "Некорректный e-mail: " + e.getMessage());
      }
    }

    String url = null;

    if (!Strings.isNullOrEmpty(form.getUrl())) {
      url = URLUtil.fixURL(form.getUrl());
    }

    if (!changeMode) {
      if (Strings.isNullOrEmpty(password)) {
        errors.reject(null, "пароль не может быть пустым");
      }
    }

    String name = Strings.emptyToNull(form.getName());

    if (name != null) {
      name = StringUtil.escapeHtml(name);
    }

    String town = null;

    if (!Strings.isNullOrEmpty(form.getTown())) {
      town = StringUtil.escapeHtml(form.getTown());
    }

    String info = null;

    if (!Strings.isNullOrEmpty(form.getInfo())) {
      info = StringUtil.escapeHtml(form.getInfo());
    }

    if (!changeMode && !errors.hasErrors()) {
      captcha.checkCaptcha(request, errors);

      if (session.getAttribute("register-visited") == null) {
        logger.info("Flood protection (not visited register.jsp) " + request.getRemoteAddr());
        errors.reject(null, "Временная ошибка, попробуйте еще раз");
      }
    }

    IPBlockInfo ipBlockInfo = ipBlockDao.getBlockInfo(request.getRemoteAddr());
    ipBlockDao.checkBlockIP(ipBlockInfo, errors);

    boolean emailChanged = false;

    if (changeMode) {
      User user = userDao.getUser(nick);

      if (!user.matchPassword(oldpass)) {
        errors.reject(null, "Неверный пароль");
      }

      user.checkAnonymous();

      String newEmail = null;

      if (mail != null) {
        if (user.getEmail()!=null && user.getEmail().equals(form.getEmail())) {
          newEmail = null;
        } else {
          if (userDao.getByEmail(mail.getAddress()) != null) {
            errors.rejectValue("email", null, "такой email уже используется");
          }

          newEmail = mail.getAddress();

          emailChanged = true;
        }
      }

      if (!errors.hasErrors()) {
        userDao.updateUser(
                user,
                name,
                url,
                newEmail,
                town,
                password,
                info
        );

        if (emailChanged) {
          sendEmail(user.getNick(), mail.getAddress(), false);
        }
      } else {
        return new ModelAndView("register-update");
      }
    } else {
      if (userDao.isUserExists(nick)) {
        errors.rejectValue("nick", null, "пользователь " + nick + " уже существует");
      }

      if (url != null && !URLUtil.isUrl(url)) {
        errors.rejectValue("url", null, "Некорректный URL");
      }

      if (mail != null && userDao.getByEmail(mail.getAddress()) != null) {
        errors.rejectValue("email", null, "пользователь с таким e-mail уже зарегистрирован. " +
                "Если вы забыли параметры своего аккаунта, воспользуйтесь формой восстановления пароля");
      }

      if (!errors.hasErrors()) {
        int userid = userDao.createUser(name, nick, password, url, mail, town, info);

        String logmessage = "Зарегистрирован пользователь " + nick + " (id=" + userid + ") " + LorHttpUtils.getRequestIP(request);
        logger.info(logmessage);

        sendEmail(nick, mail.getAddress(), true);
      } else {
        return new ModelAndView("register");
      }
    }

    if (changeMode) {
      if (emailChanged) {
        String msg = "Обновление регистрации прошло успешно. Ожидайте письма с кодом активации смены email.";

        return new ModelAndView("action-done", "message", msg);
      } else {
        return new ModelAndView(new RedirectView("/people/" + nick + "/profile"));
      }
    } else {
      return new ModelAndView("action-done", "message", "Добавление пользователя прошло успешно. Ожидайте письма с кодом активации.");
    }
  }

  private void sendEmail(String nick, String email, boolean isNew) throws MessagingException {
    StringBuilder text = new StringBuilder();

    text.append("Здравствуйте!\n\n");
    if (isNew) {
      text.append("\tВ форуме по адресу http://www.linux.org.ru/ появилась регистрационная запись,\n");
    } else {
      text.append("\tВ форуме по адресу http://www.linux.org.ru/ была изменена регистрационная запись,\n");
    }

    text.append("в которой был указал ваш электронный адрес (e-mail).\n\n");
    text.append("При заполнении регистрационной формы было указано следующее имя пользователя: '");
    text.append(nick);
    text.append("'\n\n");
    text.append("Если вы не понимаете, о чем идет речь - просто проигнорируйте это сообщение!\n\n");

    if (isNew) {
      text.append("Если же именно вы решили зарегистрироваться в форуме по адресу http://www.linux.org.ru/,\n");
      text.append("то вам следует подтвердить свою регистрацию и тем самым активировать вашу учетную запись.\n\n");
    } else {
      text.append("Если же именно вы решили изменить свою регистрационную запись http://www.linux.org.ru/,\n");
      text.append("то вам следует подтвердить свое изменение.\n\n");
    }

    String regcode = User.getActivationCode(configuration.getSecret(), nick, email);

    text.append("Для активации перейдите по ссылке http://www.linux.org.ru/activate.jsp\n\n");
    text.append("Код активации: ").append(regcode).append("\n\n");
    text.append("Благодарим за регистрацию!\n");

    Properties props = new Properties();
    props.put("mail.smtp.host", "localhost");
    Session mailSession = Session.getDefaultInstance(props, null);

    MimeMessage emailMessage = new MimeMessage(mailSession);
    emailMessage.setFrom(new InternetAddress("no-reply@linux.org.ru"));

    emailMessage.addRecipient(RecipientType.TO, new InternetAddress(email));
    emailMessage.setSubject("Linux.org.ru registration");
    emailMessage.setSentDate(new Date());
    emailMessage.setText(text.toString(), "UTF-8");

    Transport.send(emailMessage);
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
