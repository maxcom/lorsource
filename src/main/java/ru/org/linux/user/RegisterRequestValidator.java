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
import com.google.common.collect.ImmutableSet;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.URLUtil;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public class RegisterRequestValidator implements Validator {
  protected static final int TOWN_LENGTH = 100;
  protected static final int MIN_PASSWORD_LEN = 4;

  protected static final ImmutableSet<String> BAD_DOMAINS = ImmutableSet.of(
          "asdasd.ru",
          "nepwk.com",
          "klzlk.com",
          "nwldx.com",
          "mailinator.com",
          "mytrashmail.com",
          "temporaryinbox.com",
          "10minutemail.com",
          "pookmail.com",
          "dodgeit.com",
          "mailexpire.com",
          "spambox.us",
          "jetable.org",
          "maileater.com",
          "gapmail.ru",
          "mintemail.com",
          "mailinator2.com",
          "rppkn.com"
  );

  protected void checkEmail(InternetAddress email, Errors errors) {
    if (BAD_DOMAINS.contains(email.getAddress().replaceFirst("^[^@]+@", "").toLowerCase())) {
      errors.reject("email", "некорректный email домен");
    }
  }

  @Override
  public boolean supports(Class aClass) {
    return RegisterRequest.class.equals(aClass);
  }

  @Override
  public void validate(Object o, Errors errors) {
    RegisterRequest form = (RegisterRequest) o;

    /*
    Nick validate
     */
    String nick = form.getNick();

    if (Strings.isNullOrEmpty(nick)) {
      errors.rejectValue("nick", null, "не задан nick");
    }

    if (nick!=null && !StringUtil.checkLoginName(nick)) {
      errors.rejectValue("nick", null, "некорректное имя пользователя");
    }

    if (nick!=null && nick.length() > User.MAX_NICK_LENGTH) {
      errors.rejectValue("nick", null, "слишком длинное имя пользователя");
    }

    /*
    Password validate
     */

    String password = Strings.emptyToNull(form.getPassword());
    String password2 = Strings.emptyToNull(form.getPassword2());

    if (Strings.isNullOrEmpty(password)) {
      errors.reject("password", null, "пароль не может быть пустым");
    }
    if (Strings.isNullOrEmpty(password2)) {
      errors.reject("password2", null, "пароль не может быть пустым");
    }

    if (password!=null && password.equalsIgnoreCase(nick)) {
      errors.reject(password, null, "пароль не может совпадать с логином");
    }

    if (form.getPassword2() != null &&
            form.getPassword() != null &&
            !form.getPassword().equals(form.getPassword2())) {
      errors.reject(null, "введенные пароли не совпадают");
    }

    if (!Strings.isNullOrEmpty(form.getPassword()) && form.getPassword().length()< MIN_PASSWORD_LEN) {
      errors.reject("password", null, "слишком короткий пароль, минимальная длина: "+MIN_PASSWORD_LEN);
    }

    /*
    Email validate
     */

    if (Strings.isNullOrEmpty(form.getEmail())) {
      errors.rejectValue("email", null, "Не указан e-mail");
    } else {
      try {
        InternetAddress mail = new InternetAddress(form.getEmail());
        checkEmail(mail, errors);
      } catch (AddressException e) {
        errors.rejectValue("email", null, "Некорректный e-mail: " + e.getMessage());
      }
    }

    /*
    Rules validate
     */

    if(Strings.isNullOrEmpty(form.getRules())) {
      errors.reject("rules", null, "Вы не согласились с правилами");
    } else if(!"okay".equals(form.getRules())) {
      errors.reject("rules", null, "Вы не согласились с правилами");
    }
  }
}
