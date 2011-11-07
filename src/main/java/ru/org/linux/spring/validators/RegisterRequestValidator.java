package ru.org.linux.spring.validators;

import com.google.common.base.Strings;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import ru.org.linux.spring.RegisterRequest;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.URLUtil;

public class RegisterRequestValidator implements Validator {

  private static final int TOWN_LENGTH = 100;

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
  }
}
