package ru.org.linux.spring;

import ru.org.linux.site.Group;
import ru.org.linux.site.User;

public class AddMessageRequest {
  private String title;
  private String msg;
  private String url;
  private Group group;
  private String linktext;

  private User nick;
  private String password;

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

  public User getNick() {
    return nick;
  }

  public void setNick(User nick) {
    this.nick = nick;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
