package ru.org.linux.spring;

import ru.org.linux.site.Group;

public class AddMessageRequest {
  private String title;
  private String msg;
  private String url;
  private Group group;
  private String linktext;

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

  public Group getGroup() {
    return group;
  }

  public void setGroup(Group group) {
    this.group = group;
  }

  public String getLinktext() {
    if (linktext==null && group!=null) {
      return group.getDefaultLinkText();
    } else {
      return linktext;
    }
  }

  public void setLinktext(String linktext) {
    this.linktext = linktext;
  }
}
