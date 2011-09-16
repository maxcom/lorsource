package ru.org.linux.spring.validators;

import org.jdom.Verifier;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import ru.org.linux.site.BadPasswordException;
import ru.org.linux.spring.AddMessageForm;
import ru.org.linux.spring.AddMessageRequest;

public class AddMessageRequestValidator implements Validator {
  @Override
  public boolean supports(Class<?> clazz) {
    return AddMessageRequest.class.equals(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    AddMessageRequest form = (AddMessageRequest) target;

    if (form.getGroup()==null) {
      errors.rejectValue("group", null, "Группа не задана");
    }

    String title = form.getTitle();

    if (title!=null && "".equals(title.trim())) {
      errors.rejectValue("title", null, "заголовок сообщения не может быть пустым");
    }

    if (title!=null && title.length() > AddMessageForm.MAX_TITLE_LENGTH) {
      errors.rejectValue("title", null, "Слишком большой заголовок");
    }

    if (form.getMsg() != null) {
      String error = Verifier.checkCharacterData(form.getMsg());
      if (error != null) {
        errors.rejectValue("msg", null, error);
      }
    }

    if (form.getUrl()!=null && form.getUrl().length() > AddMessageForm.MAX_URL_LENGTH) {
      errors.rejectValue("url", null, "Слишком длинный URL");
    }

    if (form.getUrl()!=null && !form.getUrl().isEmpty() && (form.getLinktext()==null || form.getLinktext().isEmpty())) {
      errors.rejectValue("linktext", null, "URL указан без текста ссылки");
    }

    if (form.getNick()!=null) {
      try {
        form.getNick().checkPassword(form.getPassword());
      } catch (BadPasswordException e) {
        errors.rejectValue("password", null, e.getMessage());
        form.setNick(null);
      }
    }
  }
}
