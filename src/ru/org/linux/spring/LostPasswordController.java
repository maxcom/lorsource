/*
 * Copyright 1998-2009 Linux.org.ru
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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.AccessViolationException;
import ru.org.linux.site.LorDataSource;
import ru.org.linux.site.User;

@Controller
public class LostPasswordController {
  @RequestMapping(value="/lostpwd.jsp", method= RequestMethod.GET)
  public ModelAndView showForm() {
    return new ModelAndView("lostpwd-form");
  }

  @RequestMapping(value="/lostpwd.jsp", method= RequestMethod.POST)
  public ModelAndView sendPassword(@RequestParam("nick") String nick, @RequestParam("email") String useremail) throws Exception {
    Connection db = null;
    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      User user = User.getUser(db, nick);
      user.checkAnonymous();

      Statement st = db.createStatement();
      ResultSet rs = st.executeQuery("SELECT passwd, canmod, email, name, lostpwd>CURRENT_TIMESTAMP-'1 week'::interval as datecheck FROM users WHERE id=" + user.getId());

      rs.next();

      if (rs.getBoolean("canmod")) {
        throw new AccessViolationException("this feature is not for you, ask me directly");
      }

      if (rs.getBoolean("datecheck")) {
        throw new AccessViolationException("mail flood");
      }
      if (rs.getString("email") == null || !rs.getString("email").equals(useremail)) {
        throw new AccessViolationException("mail не совпадает с указанным при регистрации");
      }


      String password = rs.getString("passwd");
      InternetAddress email = new InternetAddress(rs.getString("email"));
      String name = rs.getString("name");
      if (name != null && !"".equals(name)) {
        email.setPersonal(name);
      }
      else {
        email.setPersonal(nick);
      }

      rs.close();
      st.executeUpdate("UPDATE users SET lostpwd=CURRENT_TIMESTAMP WHERE id=" + user.getId());

      Properties props = new Properties();
      props.put("mail.smtp.host", "localhost");
      Session mailSession = Session.getDefaultInstance(props, null);

      MimeMessage msg = new MimeMessage(mailSession);
      msg.setFrom(new InternetAddress("no-reply@linux.org.ru"));

      msg.addRecipient(MimeMessage.RecipientType.TO, email);
      msg.setSubject("Your password @linux.org.ru");
      msg.setSentDate(new Date());
      msg.setText("Hello!\n\nThis messages was sent as a reply to \"Lost password\" request at WWW.LINUX.ORG.RU website.\n\nHere is your password: " + password + "\n\nBest wishes!");

      Transport.send(msg);

      db.commit();

      st.close();

      return new ModelAndView("action-done", "message", "Ваш пароль был выслан по вашему email'у");
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }
}
