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

package ru.org.linux.comment;

import org.jdom.Verifier;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import ru.org.linux.auth.BadPasswordException;
import ru.org.linux.topic.Topic;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.bbcode.LorCodeService;

public class CommentRequestValidator implements Validator {

  private final LorCodeService service;

  public CommentRequestValidator(LorCodeService lorCodeService) {
    service = lorCodeService;
  }

  @Override
  public boolean supports(Class<?> clazz) {
    return CommentRequest.class.equals(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    CommentRequest add = (CommentRequest) target;

    if (add.getTitle() != null) {
      String title = StringUtil.escapeHtml(add.getTitle());

      if (title.length() > Comment.TITLE_LENGTH) {
        errors.rejectValue("title", null, "заголовок превышает " + Comment.TITLE_LENGTH + " символов");
      }
    }

    if (add.getMsg() != null) {
      String error = Verifier.checkCharacterData(add.getMsg());
      if (error != null) {
        errors.rejectValue("msg", null, error);
      }

      if (add.getMsg().trim().isEmpty()) {
        errors.rejectValue("msg", null, "комментарий не может быть пустым");
      }

      if(service.isEmptyTextComment(add.getMsg())) {
        errors.rejectValue("msg", null, "комментарий не может быть пустым");
      }
    }

    Topic topic = add.getTopic();

    if (topic == null) {
      errors.rejectValue("topic", null, "тема не задана");
    } else {
      if (topic.isExpired()) {
        errors.reject(null, "нельзя добавлять в устаревшие темы");
      }

      if (topic.isDeleted()) {
        errors.reject(null, "нельзя добавлять в удаленные темы");
      }
    }

    if (add.getReplyto()!=null) {
      if (add.getReplyto().isDeleted()) {
        errors.reject(null, "нельзя комментировать удаленные комментарии");
      }

      if (topic==null || add.getReplyto().getTopicId() != topic.getId()) {
        errors.reject(null, "некорректная тема");
      }
    }

    if (add.getNick()!=null) {
      try {
        add.getNick().checkPassword(add.getPassword());
      } catch (BadPasswordException e) {
        errors.rejectValue("password", null, e.getMessage());
        add.setNick(null);
      }
    }
  }
}
