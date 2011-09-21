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

import java.sql.*;
import java.util.Date;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jasypt.util.password.BasicPasswordEncryptor;
import org.jasypt.util.password.PasswordEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ru.org.linux.site.*;
import ru.org.linux.spring.dao.IPBlockDao;
import ru.org.linux.util.*;

@SuppressWarnings({"ProhibitedExceptionDeclared"})
@Controller
public class RegisterController extends ApplicationObjectSupport {
  private CaptchaService captcha;
  private IPBlockDao ipBlockDao;

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
    HttpServletRequest request
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (tmpl.isSessionAuthorized()) {
      Connection db = null;

      try {
        db = LorDataSource.getConnection();
        db.setAutoCommit(false);

        tmpl.updateCurrentUser(db);

        User user = tmpl.getCurrentUser();
        user.checkAnonymous();

        UserInfo userInfo = new UserInfo(db, user.getId());

        ModelAndView mv = new ModelAndView("register-update");

        mv.getModel().put("user", user);
        mv.getModel().put("userInfo", userInfo);
        mv.getModel().put("userInfoText", user.getUserinfo(db));

        db.commit();

        return mv;
      } finally {
        JdbcUtils.closeConnection(db);
      }
    } else {
      return new ModelAndView("register");
    }
  }

  @RequestMapping(value = "/register.jsp", method = RequestMethod.POST)
  public ModelAndView doRegister(
    HttpServletRequest request,
    @RequestParam String email,
    @RequestParam(required=false) String town,
    @RequestParam(required=false) String info,
    @RequestParam(required=false) String name,
    @RequestParam(required=false) String url,
    @RequestParam(required=false) String password,
    @RequestParam(required=false) String password2    
  ) throws Exception {
    HttpSession session = request.getSession();
    Template tmpl = Template.getTemplate(request);

    Connection db = null;
    try {
      boolean changeMode = "change".equals(request.getParameter("mode"));

      String nick;

      if (changeMode) {
        if (!tmpl.isSessionAuthorized()) {
          throw new AccessViolationException("Not authorized");
        }
        
        nick = tmpl.getNick();
      } else {
        nick = request.getParameter("nick");
        User.checkNick(nick);
      }

      if (password != null && password.length() == 0) {
        password = null;
      }

      if (password2 != null && password2.length() == 0) {
        password2 = null;
      }

      if (email == null || "".equals(email)) {
        throw new BadInputException("Не указан e-mail");
      }

      InternetAddress mail = new InternetAddress(email);
      if (url != null && "".equals(url)) {
        url = null;
      }

      if (url != null) {
        url = URLUtil.checkAndFixURL(url);
      }

      if (!changeMode) {
        if (password == null) {
          throw new BadInputException("пароль не может быть пустым");
        }

        if (password2 == null || !password.equals(password2)) {
          throw new BadInputException("введенные пароли не совпадают");
        }
      } else {
        if (password2 != null && password != null && !password.equals(password2)) {
          throw new BadInputException("введенные пароли не совпадают");
        }
      }

      if (name != null && "".equals(name)) {
        name = null;
      }

      if (town != null && "".equals(town)) {
        town = null;
      }

      if (info != null && "".equals(info)) {
        info = null;
      }

      if (name != null) {
        name = HTMLFormatter.htmlSpecialChars(name);
      }

      if (town != null) {
        town = HTMLFormatter.htmlSpecialChars(town);
      }

      if (info != null) {
        info = HTMLFormatter.htmlSpecialChars(info);
      }

      if (!changeMode) {
        captcha.checkCaptcha(request);

        if (session.getAttribute("register-visited") == null) {
          logger.info("Flood protection (not visited register.jsp) " + request.getRemoteAddr());
          throw new BadInputException("сбой");
        }
      }

      ipBlockDao.checkBlockIP(request.getRemoteAddr());

      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      int userid;

      boolean emailChanged = false;

      if (changeMode) {
        User user = User.getUser(db, nick);
        userid = user.getId();
        user.checkPassword(request.getParameter("oldpass"));
        user.checkAnonymous();

        PreparedStatement ist = db.prepareStatement(
          "UPDATE users SET  name=?, url=?, new_email=?, town=? WHERE id=" + userid);
        ist.setString(1, name);
        
        if (password != null) {
          user.setPassword(db, password);
        }

        if (url != null) {
          ist.setString(2, url);
        } else {
          ist.setString(2, null);
        }

        if (user.getEmail()!=null && user.getEmail().equals(email)) {
          ist.setString(3, null);
        } else {
          int emailCount = getUserCount(db, mail.getAddress());

          if (emailCount != 0) {
            throw new BadInputException("такой email уже используется");
          }

          ist.setString(3, mail.getAddress());

          sendEmail(tmpl, user.getNick(), mail.getAddress(), false);

          emailChanged = true;
        }

        ist.setString(4, town);

        ist.executeUpdate();

        ist.close();

        if (info != null) {
          user.setUserinfo(db, info);
        }
      } else {
        PreparedStatement pst = db.prepareStatement("SELECT count(*) as c FROM users WHERE nick=?");
        pst.setString(1, nick);
        ResultSet rs = pst.executeQuery();
        rs.next();

        if (rs.getInt("c") != 0) {
          throw new BadInputException("пользователь " + nick + " уже существует");
        }
        
        rs.close();
        pst.close();

        int emailCount = getUserCount(db, mail.getAddress());

        if (emailCount != 0) {
          throw new BadInputException("пользователь с таким e-mail уже зарегистрирован.<br>" +
            "Если вы забыли параметры своего аккаунта, воспользуйтесь <a href=\"/lostpwd.jsp\">формой восстановления пароля</a>");
        }

        Statement st = db.createStatement();
        rs = st.executeQuery("select nextval('s_uid') as userid");
        rs.next();
        userid = rs.getInt("userid");
        rs.close();
        st.close();

        PreparedStatement ist = db.prepareStatement(
          "INSERT INTO users " +
            "(id, name, nick, passwd, url, email, town, score, max_score,regdate) " +
            "VALUES (?,?,?,?,?,?,?,45,45,current_timestamp)"
        );
        ist.setInt(1, userid);
        ist.setString(2, name);
        ist.setString(3, nick);

        PasswordEncryptor encryptor = new BasicPasswordEncryptor();
        ist.setString(4, encryptor.encryptPassword(password));

        if (url != null) {
          ist.setString(5, URLUtil.checkAndFixURL(url));
        } else {
          ist.setString(5, null);
        }

        ist.setString(6, mail.getAddress());

        ist.setString(7, town);
        ist.executeUpdate();
        ist.close();

        String logmessage = "Зарегистрирован пользователь " + nick + " (id=" + userid + ") " + LorHttpUtils.getRequestIP(request);
        logger.info(logmessage);

        if (info != null) {
          User.setUserinfo(db, userid, info);
        }

        sendEmail(tmpl, nick, email, true);
      }

      db.commit();

      if (changeMode) {
        if (emailChanged) {
          String msg = "Обновление регистрации прошло успешно. Ожидайте письма с кодом активации смены email.";

          return new ModelAndView("action-done", "message", msg);
        } else {
          return new ModelAndView(new RedirectView("/people/"+nick+"/profile"));
        }
      } else {
        return new ModelAndView("action-done", "message", "Добавление пользователя прошло успешно. Ожидайте письма с кодом активации.");
      }
    } catch (BadInputException e) {
      return new ModelAndView("register", "error", e.getMessage());
    } catch (BadURLException e) {
      return new ModelAndView("register", "error", e.getMessage());
    } catch (AddressException e) {
      return new ModelAndView("register", "error", "Некорректный e-mail: "+e.getMessage());
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  private static int getUserCount(Connection db, String email) throws SQLException {
    PreparedStatement pst2 = db.prepareStatement("SELECT count(*) as c FROM users WHERE email=?");
    ResultSet rs = null;

    try {
      pst2.setString(1, email);
      rs = pst2.executeQuery();
      if (!rs.next()) {
        return 0;
      }

      return rs.getInt("c");
    } finally {
      JdbcUtils.closeResultSet(rs);
      JdbcUtils.closeStatement(pst2);
    }
  }

  private static void sendEmail(Template tmpl, String nick, String email, boolean isNew) throws MessagingException {
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

    String regcode = User.getActivationCode(tmpl.getSecret(), nick, email);

    text.append("Для активации перейдите по ссылке http://www.linux.org.ru/activate.jsp\n\n");
    text.append("Код активации: ").append(regcode).append("\n\n");
    text.append("Благодарим за регистрацию!\n");

    Properties props = new Properties();
    props.put("mail.smtp.host", "localhost");
    Session mailSession = Session.getDefaultInstance(props, null);

    MimeMessage emailMessage = new MimeMessage(mailSession);
    emailMessage.setFrom(new InternetAddress("no-reply@linux.org.ru"));

    emailMessage.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(email));
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

    Connection db = null;

    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      User user = tmpl.getCurrentUser();

      Statement st = db.createStatement();
      ResultSet rs = st.executeQuery("SELECT new_email FROM users WHERE id=" + user.getId());
      rs.next();

      String newEmail = rs.getString("new_email");
      if (newEmail == null) {
        throw new AccessViolationException("new_email == null?!");
      }

      String regcode = user.getActivationCode(tmpl.getSecret(), newEmail);

      if (regcode.equals(activation)) {
        PreparedStatement pst = db.prepareStatement("UPDATE users SET email=new_email WHERE id=?");
        pst.setInt(1, user.getId());
        pst.executeUpdate();
      } else {
        throw new AccessViolationException("Bad activation code");
      }

      db.commit();

      return new ModelAndView(new RedirectView("/people/"+user.getNick()+"/profile"));
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }
}
