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

import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.auth.AuthUtil;
import ru.org.linux.site.Template;
import ru.org.linux.util.ExceptionBindingErrorProcessor;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Controller
public class DeregisterController {
  private final UserDao userDao;
  private final UserService userService;

  public DeregisterController(UserDao userDao, UserService userService) {
    this.userDao = userDao;
    this.userService = userService;
  }

  @RequestMapping(value = "/deregister.jsp", method = {RequestMethod.GET, RequestMethod.HEAD})
  public ModelAndView show(
    @ModelAttribute("form") DeregisterRequest form
  ) {

    Template tmpl = Template.getTemplate();
    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = AuthUtil.getCurrentUser();

    if (user.getScore() < 100) {
      throw new AccessViolationException("Удаление аккаунта недоступно для пользователей со score < 100");
    }

    if (user.isAdministrator() || user.isModerator()) {
      throw new AccessViolationException("Нельзя удалить модераторский аккаунт");
    }

    return new ModelAndView("deregister");
  }

  @RequestMapping(value = "/deregister.jsp", method = {RequestMethod.POST})
  public ModelAndView deregister(@Valid @ModelAttribute("form") DeregisterRequest form, Errors errors,
                                 HttpServletRequest request) {
    Template tmpl = Template.getTemplate();

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = AuthUtil.getCurrentUser();
    user.checkFrozen(errors);

    if (user.getScore() < 100) {
      throw new AccessViolationException("Удаление аккаунта недоступно для пользователей со score < 100");
    }

    if (!user.matchPassword(form.getPassword())) {
      errors.rejectValue("password", null, "Неверный пароль");
    }

    if (user.isAdministrator() || user.isModerator()) {
      throw new AccessViolationException("Нельзя удалить модераторский аккаунт");
    }

    if (errors.hasErrors()) {
      return new ModelAndView("deregister");
    }

    // Remove user info
    userDao.resetUserpic(user, user);
    userService.updateUser(user, "", "", null, "", null, "", request.getRemoteAddr());

    // Block account
    userDao.block(user, user, "самостоятельная блокировка аккаунта");

    return new ModelAndView(
      "action-done",
      "message",
      "Удаление пользователя прошло успешно."
    );
  }

  @InitBinder("form")
  public void requestValidator(WebDataBinder binder) {
    binder.setValidator(new DeregisterRequestValidator());
    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor());
  }
}
