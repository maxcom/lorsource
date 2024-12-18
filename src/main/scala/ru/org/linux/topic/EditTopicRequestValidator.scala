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

import org.springframework.stereotype.Component
import org.springframework.validation.{Errors, Validator}
import ru.org.linux.edithistory.{EditHistoryObjectTypeEnum, EditHistoryService}
import ru.org.linux.topic.AddTopicRequestValidator.{validateMsg, validateTags, validateTitle, validateUrl}

import scala.jdk.CollectionConverters.MapHasAsScala

object EditTopicRequestValidator {
  private val MaxCommitBonus = 20
  private val MaxEditorBonus = 5
}

@Component
class EditTopicRequestValidator(editHistoryService: EditHistoryService) extends Validator {
  override def supports(clazz: Class[?]): Boolean = classOf[EditTopicRequest] == clazz

  override def validate(target: AnyRef, errors: Errors): Unit = {
    val form = target.asInstanceOf[EditTopicRequest]

    validateTitle(form.title, errors)
    validateMsg(form.msg, errors)
    validateUrl(form.url, form.linktext, errors)
    validateTags(form.tags, errors)

    if (form.bonus < 0 || form.bonus > EditTopicRequestValidator.MaxCommitBonus) {
      errors.rejectValue("bonus", null, "Некорректное значение bonus")
    }

    if (form.editorBonus != null) {
      for (value <- form.editorBonus.asScala.values) {
        if (value == null || value < 0 || value > EditTopicRequestValidator.MaxEditorBonus) {
          errors.rejectValue("editorBonus", null, "Некорректное значение editorBonus")
        }
      }
    }

    val editInfoList = editHistoryService.getEditInfo(form.topic.id, EditHistoryObjectTypeEnum.TOPIC)

    if (editInfoList.nonEmpty) {
      val editHistoryRecord = editInfoList.head

      if (form.lastEdit == null || editHistoryRecord.getEditdate.getTime.toString != form.lastEdit) {
        errors.reject(null, "Сообщение было отредактировано независимо")
      }
    }

    if (form.editorBonus != null) {
      val editors = editHistoryService.getEditorUsers(form.topic, editInfoList)

      form.editorBonus.asScala.keySet.diff(editors).foreach { _ =>
        errors.reject("editorBonus", "некорректный корректор?!")
      }
    }
  }
}