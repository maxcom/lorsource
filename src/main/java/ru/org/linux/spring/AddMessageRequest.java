package ru.org.linux.spring;

import org.jdom.Verifier;
import org.springframework.validation.Errors;
import ru.org.linux.site.AccessViolationException;
import ru.org.linux.site.Group;

public class AddMessageRequest {
  private String title;
  private String msg;
  private String url;
  private Group group;

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
  }

  public Group getGroup() {
    return group;
  }

  public void setGroup(Group group) {
    this.group = group;
  }
}
