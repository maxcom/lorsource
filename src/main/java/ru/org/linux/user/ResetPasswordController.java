/*
 * Copyright 1998-2013 Linux.org.ru
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

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.util.StringUtil;

import java.sql.Timestamp;

@Controller
@RequestMapping("/reset-password")
public class ResetPasswordController {
  private static final Logger logger = LoggerFactory.getLogger(ResetPasswordController.class);

  @Autowired
  private UserDao userDao;

  @Autowired
  private UserService userService;

  @RequestMapping(method=RequestMethod.GET)
  public ModelAndView showCodeForm() {
    return new ModelAndView("reset-password-form");
  }

  @RequestMapping(method=RequestMethod.POST)
  public ModelAndView resetPassword(
    @RequestParam("nick") String nick,
    @RequestParam("code") String formCode
  ) throws Exception {
    User user = userDao.getUser(nick);

    user.checkBlocked();
    user.checkAnonymous();

    if (user.isAdministrator()) {
      throw new AccessViolationException("this feature is not for you, ask me directly");
    }

    Timestamp resetDate = userDao.getResetDate(user);

    String resetCode = userService.getResetCode(user.getNick(), user.getEmail(), resetDate);

    if (!resetCode.equals(formCode)) {
      logger.warn("Код проверки не совпадает; login={} formCode={} resetCode={}", nick, formCode, resetCode);

      throw new UserErrorException("Код не совпадает");
    }

    String password = userDao.resetPassword(user);

    return new ModelAndView(
            "action-done",
            ImmutableMap.of(
                    "message", "Установлен новый пароль",
                    "bigMessage", "Ваш новый пароль: " + StringUtil.escapeHtml(password)
            )
    );
  }

  @ExceptionHandler({UserNotFoundException.class, UserErrorException.class})
  public ModelAndView handleUserError(Exception ex) {
    return new ModelAndView("reset-password-form", "error", ex.getMessage());
  }
}
