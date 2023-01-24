/*
 * Copyright 1998-2023 Linux.org.ru
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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.email.EmailService;
import ru.org.linux.site.BadInputException;
import ru.org.linux.site.Template;

import javax.mail.internet.AddressException;
import java.sql.Timestamp;

@Controller
@RequestMapping("/lostpwd.jsp")
public class LostPasswordController {
  private final UserDao userDao;

  private final UserService userService;
  private final EmailService emailService;

  public LostPasswordController(UserDao userDao, UserService userService, EmailService emailService) {
    this.userDao = userDao;
    this.userService = userService;
    this.emailService = emailService;
  }

  @RequestMapping(method=RequestMethod.GET)
  public ModelAndView showForm() {
    return new ModelAndView("lostpwd-form");
  }

  @RequestMapping(method=RequestMethod.POST)
  public ModelAndView sendPassword(@RequestParam("email") String email) throws Exception {
    Template tmpl = Template.getTemplate();

    if (Strings.isNullOrEmpty(email)) {
      throw new BadInputException("email не задан");
    }

    User user = userDao.getByEmail(email, true);
    if (user==null) {
      throw new BadInputException("Этот email не зарегистрирован!");
    }

    user.checkBlocked();
    if (user.isAnonymous()) {
      throw new AccessViolationException("Anonymous user");
    }

    if (user.isModerator() && !tmpl.isModeratorSession()) {
      throw new AccessViolationException("этот пароль могут сбросить только модераторы");
    }

    if (!tmpl.isModeratorSession() && !userDao.canResetPassword(user)) {
      throw new BadInputException("Нельзя запрашивать пароль чаще одного раза в неделю!");
    }

    Timestamp now = new Timestamp(System.currentTimeMillis());

    try {
      String resetCode = userService.getResetCode(user.getNick(), user.getEmail(), now);

      emailService.sendPasswordReset(user, resetCode);
      userDao.updateResetDate(user, now);

      return new ModelAndView("action-done", "message",
              "Инструкция по сбросу пароля была отправлена на ваш email");
    } catch (AddressException ex) {
      throw new UserErrorException("Incorrect email address");
    }
  }

  @ExceptionHandler(UserErrorException.class)
  public ModelAndView handleUserError(UserErrorException ex) {
    return new ModelAndView("lostpwd-form", "error", ex.getMessage());
  }
}
