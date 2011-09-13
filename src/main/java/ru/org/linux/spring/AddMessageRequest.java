package ru.org.linux.spring;

import org.jdom.Verifier;
import org.springframework.validation.Errors;
import ru.org.linux.site.AccessViolationException;

public class AddMessageRequest {
  private String title;
  private String msg;
  private String url;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getMsg() {
    return msg;
  }

  public void setMsg(String msg) {
    this.msg = msg;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public void validate(Errors errors) throws AccessViolationException {
    if ("".equals(title.trim())) {
      errors.rejectValue("title", null, "заголовок сообщения не может быть пустым");
    }

    if (title!=null && title.length() > AddMessageForm.MAX_TITLE_LENGTH) {
      errors.rejectValue("title", null, "Слишком большой заголовок");
    }

    String error = Verifier.checkCharacterData(msg);
    if (error!=null) {
      errors.rejectValue("msg", null, error);
    }

    if (url!=null && url.length() > AddMessageForm.MAX_URL_LENGTH) {
      errors.rejectValue("url", null, "Слишком длинный URL");
    }
  }
}
