/*
 * Copyright 1998-2022 Linux.org.ru
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
import org.springframework.core.io.ResourceLoader;
import org.springframework.validation.Errors;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.URLUtil;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public class EditRegisterRequestValidator extends RegisterRequestValidator {
  public EditRegisterRequestValidator(ResourceLoader resourceLoader) {
    super(resourceLoader);
  }

  @Override
  public boolean supports(Class aClass) {
    return EditRegisterRequest.class.equals(aClass);
  }

  @Override
  public void validate(Object o, Errors errors) {
    EditRegisterRequest form = (EditRegisterRequest) o;
    if (!Strings.isNullOrEmpty(form.getTown())) {
      if (StringUtil.escapeHtml(form.getTown()).length() > TOWN_LENGTH) {
        errors.rejectValue("town", null, "Слишком длиное название города (максимум "+TOWN_LENGTH+" символов)");
      }
    }

    if (!Strings.isNullOrEmpty(form.getUrl()) && !URLUtil.isUrl(form.getUrl())) {
      errors.rejectValue("url", null, "Некорректный URL");
    }

    if (form.getPassword2() != null &&
        form.getPassword() != null &&
        !form.getPassword().equals(form.getPassword2())) {
      errors.reject(null, "введенные пароли не совпадают");
    }

    if (!Strings.isNullOrEmpty(form.getPassword()) && form.getPassword().length()< MIN_PASSWORD_LEN) {
      errors.reject(null, "слишком короткий пароль, минимальная длина: "+MIN_PASSWORD_LEN);
    }

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

  }
}
