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

package ru.org.linux.auth;

import com.google.common.base.Strings;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.URLUtil;

public class RegisterRequestValidator implements Validator {
  private static final int TOWN_LENGTH = 100;
  private static final int MIN_PASSWORD_LEN = 4;

  @Override
  public boolean supports(Class<?> aClass) {
    return RegisterRequest.class.equals(aClass);
  }

  @Override
  public void validate(Object o, Errors errors) {
    RegisterRequest form = (RegisterRequest) o;

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
  }
}
