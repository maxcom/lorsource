/*
 * Copyright 1998-2024 Linux.org.ru
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

package ru.org.linux.topic;

import com.google.common.base.Strings;
import org.jdom2.Verifier;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import ru.org.linux.auth.BadPasswordException;
import ru.org.linux.tag.TagName;
import ru.org.linux.util.URLUtil;

@Component
public class AddTopicRequestValidator implements Validator {
  private static final int MAX_TITLE_LENGTH = 140;
  private static final int MAX_URL_LENGTH = 255;

  @Override
  public boolean supports(Class<?> clazz) {
    return AddTopicRequest.class.equals(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    AddTopicRequest form = (AddTopicRequest) target;

    if (form.getGroup()==null) {
      errors.rejectValue("group", null, "Группа не задана");
    }

    String title = form.getTitle();

    if (title!=null) {
      if (title.trim().isEmpty()) {
        errors.rejectValue("title", null, "заголовок сообщения не может быть пустым");
      }

      if (title.length() > MAX_TITLE_LENGTH) {
        errors.rejectValue("title", null, "Слишком большой заголовок");
      }

      if (title.trim().startsWith("[")) {
        errors.rejectValue("title", null, "Не добавляйте теги в заголовки, используйте предназначенное для тегов поле ввода");
      }
    }

    if (form.getMsg() != null) {
      String error = Verifier.checkCharacterData(form.getMsg());
      if (error != null) {
        errors.rejectValue("msg", null, error);
      }
    }

    if (!Strings.isNullOrEmpty(form.getUrl())) {
      if (form.getUrl().length() > MAX_URL_LENGTH) {
        errors.rejectValue("url", null, "Слишком длинный URL");
      }

      if (!URLUtil.isUrl(form.getUrl())) {
        errors.rejectValue("url", null, "Некорректный URL");
      }

      if (form.getLinktext()==null || form.getLinktext().isEmpty()) {
        errors.rejectValue("linktext", null, "URL указан без текста ссылки");
      }
    }

    if (form.getNick()!=null) {
      try {
        form.getNick().checkPassword(form.getPassword());
      } catch (BadPasswordException e) {
        errors.rejectValue("password", null, e.getMessage());
        form.setNick(null);
      }
    }

    if (form.getTags()!=null) {
      TagName.parseAndValidateTags(form.getTags(), errors, TagName.MaxTagsPerTopic());
    }
  }
}
