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
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.site.BadInputException;
import ru.org.linux.site.Template;
import ru.org.linux.spring.Configuration;
import ru.org.linux.util.StringUtil;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;

@Controller
public class LostPasswordController {
  @Autowired
  private UserDao userDao;

  @Autowired
  private Configuration configuration;

  @RequestMapping(value="/lostpwd.jsp", method= RequestMethod.GET)
  public ModelAndView showForm() {
    return new ModelAndView("lostpwd-form");
  }

  @RequestMapping(value="/reset-password", method= RequestMethod.GET)
  public ModelAndView showCodeForm() {
    return new ModelAndView("reset-password-form");
  }

  @RequestMapping(value="/lostpwd.jsp", method= RequestMethod.POST)
  public ModelAndView sendPassword(@RequestParam("email") String email, HttpServletRequest request) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (Strings.isNullOrEmpty(email)) {
      throw new BadInputException("email не задан");
    }

    User user = userDao.getByEmail(email, true);
    if (user==null) {
      throw new BadInputException("Ваш email не зарегистрирован");
    }

    user.checkBlocked();
    user.checkAnonymous();

    if (user.isModerator() && !tmpl.isModeratorSession()) {
      throw new AccessViolationException("этот пароль могут сбросить только модераторы");
    }

    if (!tmpl.isModeratorSession() && !userDao.canResetPassword(user)) {
      throw new AccessViolationException("нельзя запрашивать пароль чаще одного раза в неделю");
    }

    Timestamp now = new Timestamp(System.currentTimeMillis());

    try {
      sendEmail(user, email, now);
      userDao.updateResetDate(user, now);

      return new ModelAndView("action-done", "message", "Инструкция по сбросу пароля была отправлена на ваш email");
    } catch (AddressException ex) {
      throw new UserErrorException("Incorrect email address");
    }
  }

  @RequestMapping(value="/reset-password", method= RequestMethod.POST)
  public ModelAndView resetPassword(
    @RequestParam("nick") String nick,
    @RequestParam("code") String formCode,
    HttpServletRequest request
  ) throws Exception {
    User user = userDao.getUser(nick);

    user.checkBlocked();
    user.checkAnonymous();

    if (user.isAdministrator()) {
      throw new AccessViolationException("this feature is not for you, ask me directly");
    }

    Timestamp resetDate = userDao.getResetDate(user);

    String resetCode = getResetCode(configuration.getSecret(), user.getNick(), user.getEmail(), resetDate);

    if (!resetCode.equals(formCode)) {
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

  private static String getResetCode(String base, String nick, String email, Timestamp tm) {
    return StringUtil.md5hash(base + ':' + nick + ':' + email+ ':' +Long.toString(tm.getTime())+":reset");
  }

  private void sendEmail(User user, String email, Timestamp resetDate) throws MessagingException {
    Properties props = new Properties();
    props.put("mail.smtp.host", "localhost");
    Session mailSession = Session.getDefaultInstance(props, null);

    MimeMessage msg = new MimeMessage(mailSession);
    msg.setFrom(new InternetAddress("no-reply@linux.org.ru"));

    String resetCode = getResetCode(configuration.getSecret(), user.getNick(), email, resetDate);

    msg.addRecipient(RecipientType.TO, new InternetAddress(email));
    msg.setSubject("Your password @linux.org.ru");
    msg.setSentDate(new Date());
    msg.setText(
      "Здравствуйте!\n\n" +
      "Для сброса вашего пароля перейдите по ссылке http://www.linux.org.ru/reset-password\n\n" +
      "Ваш ник "+user.getNick()+", код подтверждения: " + resetCode + "\n\n" +
      "Удачи!"
    );

    Transport.send(msg);
  }
}
