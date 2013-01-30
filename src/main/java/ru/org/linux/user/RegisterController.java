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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.*;
import ru.org.linux.site.Template;
import ru.org.linux.spring.Configuration;
import ru.org.linux.util.EmailService;
import ru.org.linux.util.ExceptionBindingErrorProcessor;
import ru.org.linux.util.LorHttpUtils;

import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

@SuppressWarnings("ProhibitedExceptionDeclared")
@Controller
public class RegisterController {
  private static final Log logger = LogFactory.getLog(RegisterController.class);

  private CaptchaService captcha;
  private IPBlockDao ipBlockDao;

  @Autowired
  RememberMeServices rememberMeServices;

  @Autowired
  @Qualifier("authenticationManager")
  private AuthenticationManager authenticationManager;

  @Autowired
  private UserDetailsServiceImpl userDetailsService;


  @Autowired
  private UserDao userDao;

  @Autowired
  private EmailService emailService;

  @Autowired
  private Configuration configuration;

  @Autowired
  private PasswordVerify passwordVerify;

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
    HttpServletResponse response
  ) {
      response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
      return new ModelAndView("register");
  }

  @RequestMapping(value = "/register.jsp", method = RequestMethod.POST)
  public ModelAndView doRegister(
    HttpServletRequest request,
    @Valid @ModelAttribute("form") RegisterRequest form,
    Errors errors
  ) throws Exception {
    HttpSession session = request.getSession();
    Template tmpl = Template.getTemplate(request);

    if (!errors.hasErrors()) {
      captcha.checkCaptcha(request, errors);

      if (session.getAttribute("register-visited") == null) {
        logger.info("Flood protection (not visited register.jsp) " + request.getRemoteAddr());
        errors.reject(null, "Временная ошибка, попробуйте еще раз");
      }

      ipBlockDao.checkBlockIP(request.getRemoteAddr(), errors, tmpl.getCurrentUser());

      if (userDao.isUserExists(form.getNick())) {
        errors.rejectValue("nick", null, "пользователь " + form.getNick() + " уже существует");
      }

      if (userDao.getByEmail(new InternetAddress(form.getEmail()).getAddress().toLowerCase(), false) != null) {
        errors.rejectValue("email", null, "пользователь с таким e-mail уже зарегистрирован. " +
                "Если вы забыли параметры своего аккаунта, воспользуйтесь формой восстановления пароля");
      }
    }

    if (!errors.hasErrors()) {
      InternetAddress mail = new InternetAddress(form.getEmail().toLowerCase());

      int userid = userDao.createUser("", form.getNick(), form.getPassword(), "", mail, "", "");

      String logmessage = "Зарегистрирован пользователь " + form.getNick() + " (id=" + userid + ") " + LorHttpUtils.getRequestIP(request);
      logger.info(logmessage);

      emailService.sendEmail(form.getNick(), mail.getAddress(), true);
    } else {
      return new ModelAndView("register");
    }

    return new ModelAndView("action-done", "message", "Добавление пользователя прошло успешно. Ожидайте письма с кодом активации.");
  }


  @RequestMapping(value="/activate.jsp", method= RequestMethod.GET)
  public ModelAndView activateForm() {
    return new ModelAndView("activate");
  }

  @RequestMapping(value = "/activate.jsp", method = RequestMethod.POST, params = "action")
  public ModelAndView activateNew(
    HttpServletRequest request,
    HttpServletResponse response,
    @RequestParam String activation,
    @RequestParam String nick,
    @RequestParam String passwd
  ) throws Exception {
    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(nick, passwd);
    try {
      UserDetailsImpl details = (UserDetailsImpl) userDetailsService.loadUserByUsername(nick);
      if(details.getUser().isActivated()) {
        return new ModelAndView(new RedirectView("/"));
      }
      token.setDetails(details);
      Authentication auth = authenticationManager.authenticate(token);
      UserDetailsImpl userDetails = (UserDetailsImpl)auth.getDetails();
      String regcode = userDetails.getUser().getActivationCode(configuration.getSecret());
      if(regcode.equals(activation)) {
        userDao.activateUser(userDetails.getUser());
        details = (UserDetailsImpl) userDetailsService.loadUserByUsername(nick);
        token.setDetails(details);
        auth = authenticationManager.authenticate(token);
      } else {
        throw new AccessViolationException("Bad activation code");
      }
      SecurityContextHolder.getContext().setAuthentication(auth);
      rememberMeServices.loginSuccess(request, response, auth);
      AuthUtil.updateLastLogin(auth, userDao);
    } catch (Exception e) {
      throw new AccessViolationException(e.getMessage());
    }
    return new ModelAndView(new RedirectView("/"));
  }


  @RequestMapping(value = "/activate.jsp", method = RequestMethod.POST, params = "!action")
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
    binder.setValidator(new RegisterRequestValidator(passwordVerify));
    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor());
  }
}
