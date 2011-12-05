package ru.org.linux.spring.validators;

import org.jdom.Verifier;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import ru.org.linux.dto.MessageDto;
import ru.org.linux.site.BadPasswordException;
import ru.org.linux.site.Comment;
import ru.org.linux.spring.AddCommentRequest;
import ru.org.linux.util.StringUtil;

public class AddCommentRequestValidator implements Validator {
  @Override
  public boolean supports(Class<?> clazz) {
    return AddCommentRequest.class.equals(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    AddCommentRequest add = (AddCommentRequest) target;

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
    }

    MessageDto topic = add.getTopic();

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

      if (topic==null || add.getReplyto().getTopic() != topic.getId()) {
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
