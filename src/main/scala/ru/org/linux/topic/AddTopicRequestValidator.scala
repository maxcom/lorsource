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
import ru.org.linux.topic.AddTopicRequestValidator.{validateMsg, validateTags, validateTitle, validateUrl}
import ru.org.linux.util.URLUtil

object AddTopicRequestValidator {
  private val MaxTitleLength = 140
  private val MaxUrlLength = 255

  def validateUrl(url: String, linktext: String, errors: Errors): Unit = {
    if (!Strings.isNullOrEmpty(url)) {
      if (url.length > AddTopicRequestValidator.MaxUrlLength) {
        errors.rejectValue("url", null, "Слишком длинный URL")
      }

      if (!URLUtil.isUrl(url)) {
        errors.rejectValue("url", null, "Некорректный URL")
      }

      if (linktext == null || linktext.isEmpty) {
        errors.rejectValue("linktext", null, "URL указан без текста ссылки")
      }
    }
  }

  def validateTitle(title: String, errors: Errors): Unit = {
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
  }

  def validateMsg(msg: String, errors: Errors): Unit = {
    if (msg != null) {
      val error = Verifier.checkCharacterData(msg)
      if (error != null) {
        errors.rejectValue("msg", null, error)
      }
    }
  }

  def validateTags(tags: String, errors: Errors): Unit = {
    if (tags != null) {
      TagName.parseAndValidateTags(tags, errors, TagName.MaxTagsPerTopic)
    }
  }
}

@Component
class AddTopicRequestValidator extends Validator {
  override def supports(clazz: Class[?]): Boolean = classOf[AddTopicRequest] == clazz

  override def validate(target: AnyRef, errors: Errors): Unit = {
    val form = target.asInstanceOf[AddTopicRequest]

    if (form.group == null) {
      errors.rejectValue("group", null, "Группа не задана")
    }

    validateTitle(form.title, errors)
    validateMsg(form.msg, errors)
    validateUrl(form.url, form.linktext, errors)
    validateTags(form.tags, errors)
  }
}