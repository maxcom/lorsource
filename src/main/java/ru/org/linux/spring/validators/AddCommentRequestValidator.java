package ru.org.linux.spring.validators;

import org.jdom.Verifier;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import ru.org.linux.site.Comment;
import ru.org.linux.spring.AddCommentRequest;
import ru.org.linux.util.HTMLFormatter;

public class AddCommentRequestValidator implements Validator {
  @Override
  public boolean supports(Class<?> clazz) {
    return AddCommentRequest.class.equals(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    AddCommentRequest add = (AddCommentRequest) target;

    if (add.getTitle() != null) {
      String title = HTMLFormatter.htmlSpecialChars(add.getTitle());

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
  }
}
