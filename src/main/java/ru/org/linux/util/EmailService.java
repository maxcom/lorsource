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


package ru.org.linux.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.spring.Configuration;
import ru.org.linux.user.User;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

@Service
public class EmailService {

  @Autowired
  private Configuration configuration;

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

    String regcode = User.getActivationCode(configuration.getSecret(), nick, email);

    text.append("Для активации перейдите по ссылке https://www.linux.org.ru/activate.jsp\n\n");
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

}
