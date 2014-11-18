/*
 * Copyright 1998-2014 Linux.org.ru
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


package ru.org.linux.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.spring.SiteConfig;
import ru.org.linux.user.User;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

@Service
public class EmailService {
  private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
  private static final String EMAIL_SENT = "Произошла непредвиденная ошибка. Администраторы получили об этом сигнал.";
  private static final String EMAIL_NOT_SENT = "Произошла непредвиденная ошибка. К сожалению сервер временно не принимает сообщения об ошибках.";

  @Autowired
  private SiteConfig siteConfig;

  public void sendEmail(String nick, String email, boolean isNew) throws MessagingException {
    StringBuilder text = new StringBuilder();

    text.append("Здравствуйте!\n\n");
    if (isNew) {
      text.append("\tНа форуме по адресу http://www.linux.org.ru/ появилась регистрационная запись,\n");
    } else {
      text.append("\tНа форуме по адресу http://www.linux.org.ru/ была изменена регистрационная запись,\n");
    }

    text.append("в которой был указал ваш электронный адрес (e-mail).\n\n");
    text.append("При заполнении регистрационной формы было указано следующее имя пользователя: '");
    text.append(nick);
    text.append("'\n\n");
    text.append("Если вы не понимаете, о чем идет речь - просто проигнорируйте это сообщение!\n\n");

    if (isNew) {
      text.append("Если же именно вы решили зарегистрироваться на форуме по адресу http://www.linux.org.ru/,\n");
      text.append("то вам следует подтвердить свою регистрацию и тем самым активировать вашу учетную запись.\n\n");
    } else {
      text.append("Если же именно вы решили изменить свою регистрационную запись http://www.linux.org.ru/,\n");
      text.append("то вам следует подтвердить свое изменение.\n\n");
    }

    String regcode = User.getActivationCode(siteConfig.getSecret(), nick, email);

    text.append("Для активации перейдите по ссылке https://www.linux.org.ru/activate.jsp\n\n");
    text.append("Код активации: ").append(regcode).append("\n\n");
    text.append("Благодарим за регистрацию!\n");

    MimeMessage emailMessage = prepareMimeMessage();
    emailMessage.setFrom(new InternetAddress("no-reply@linux.org.ru"));

    emailMessage.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(email));
    emailMessage.setSubject("Linux.org.ru registration");
    emailMessage.setSentDate(new Date());
    emailMessage.setText(text.toString(), "UTF-8");

    Transport.send(emailMessage);
  }

  private MimeMessage prepareMimeMessage() {
    Properties props = new Properties();
    props.put("mail.smtp.host", "localhost");
    Session mailSession = Session.getDefaultInstance(props, null);
    return new MimeMessage(mailSession);
  }

  /**
   * Отсылка E-mail администраторам.
   *
   * @param request   данные запроса от web-клиента
   * @param exception исключение
   * @return Строку, содержащую состояние отсылки письма
   */
  public String sendExceptionReport(
    HttpServletRequest request,
    Exception exception
  ) {
    StringBuilder text = new StringBuilder();

    if (exception.getMessage() == null) {
      text.append(exception.getClass().getName());
    } else {
      text.append(exception.getMessage());
    }
    text.append("\n\n");

    Object attributeUrl = request.getAttribute("javax.servlet.error.request_uri");

    if (attributeUrl!=null) {
      text.append("Attribute URL: ").append(attributeUrl).append("\n");
    }

    Object forwardUrl = request.getAttribute("javax.servlet.forward.request_uri");
    if (forwardUrl!=null) {
      text.append("Forward URL: ").append(forwardUrl).append("\n");
    }

    String mainUrl;

    mainUrl = siteConfig.getMainUrlWithoutSlash();

    text.append("Main URL: ").append(mainUrl).append(request.getServletPath());

    if (request.getQueryString() != null) {
      text.append('?').append(request.getQueryString()).append('\n');
    }
    text.append('\n');

    text.append("IP: " + request.getRemoteAddr() + '\n');

    text.append(" Headers: ");
    Enumeration<String> enu = request.getHeaderNames();
    while (enu.hasMoreElements()) {
      String paramName = enu.nextElement();
      text.append("\n         ").append(paramName).append(": ").append(request.getHeader(paramName));
    }
    text.append("\n\n");

    StringWriter exceptionStackTrace = new StringWriter();
    exception.printStackTrace(new PrintWriter(exceptionStackTrace));
    text.append(exceptionStackTrace.toString());

    if (sendErrorMail("Linux.org.ru: " + exception.getClass(), text)) {
      return EMAIL_SENT;
    } else {
      return EMAIL_NOT_SENT;
    }
  }

  private boolean sendErrorMail(String subject, CharSequence text) {
    InternetAddress mail;
    String adminEmailAddress = siteConfig.getAdminEmailAddress();

    try {
      mail = new InternetAddress(adminEmailAddress, true);
    } catch (AddressException e) {
      logger.warn("Неправильный e-mail адрес: " + adminEmailAddress);
      return false;
    }

    MimeMessage emailMessage = prepareMimeMessage();
    try {
      emailMessage.setFrom(new InternetAddress("no-reply@linux.org.ru"));
      emailMessage.addRecipient(Message.RecipientType.TO, mail);
      emailMessage.setSubject(subject);
      emailMessage.setSentDate(new Date());
      emailMessage.setText(text.toString(), "UTF-8");

      Transport.send(emailMessage);
      return true;
    } catch (Exception e) {
      logger.error("An error occured while sending e-mail!", e);
      return false;
    }
  }
}
