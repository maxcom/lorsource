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
package ru.org.linux.topic

import com.google.common.base.Strings
import org.jdom2.Verifier
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import ru.org.linux.tag.TagName
import ru.org.linux.util.URLUtil

@Component
object AddTopicRequestValidator {
  private val MaxTitleLength = 140
  private val MaxUrlLength = 255
}

@Component
class AddTopicRequestValidator extends Validator {
  override def supports(clazz: Class[?]): Boolean = classOf[AddTopicRequest] == clazz

  override def validate(target: AnyRef, errors: Errors): Unit = {
    val form = target.asInstanceOf[AddTopicRequest]

    if (form.group == null) {
      errors.rejectValue("group", null, "Группа не задана")
    }

    val title = form.title

    if (title != null) {
      if (title.trim.isEmpty) {
        errors.rejectValue("title", null, "заголовок сообщения не может быть пустым")
      }
      if (title.length > AddTopicRequestValidator.MaxTitleLength) {
        errors.rejectValue("title", null, "Слишком большой заголовок")
      }
      if (title.trim.startsWith("[")) {
        errors.rejectValue("title", null, "Не добавляйте теги в заголовки, используйте предназначенное для тегов поле ввода")
      }
    }

    if (form.msg != null) {
      val error = Verifier.checkCharacterData(form.msg)
      if (error != null) {
        errors.rejectValue("msg", null, error)
      }
    }

    if (!Strings.isNullOrEmpty(form.url)) {
      if (form.url.length > AddTopicRequestValidator.MaxUrlLength) {
        errors.rejectValue("url", null, "Слишком длинный URL")
      }

      if (!URLUtil.isUrl(form.url)) {
        errors.rejectValue("url", null, "Некорректный URL")
      }

      if (form.linktext == null || form.linktext.isEmpty) {
        errors.rejectValue("linktext", null, "URL указан без текста ссылки")
      }
    }

    if (form.tags != null) {
      TagName.parseAndValidateTags(form.tags, errors, TagName.MaxTagsPerTopic)
    }
  }
}