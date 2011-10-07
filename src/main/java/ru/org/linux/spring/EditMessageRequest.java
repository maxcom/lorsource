package ru.org.linux.spring;

public class EditMessageRequest {
  private String url;
  private String linktext;
  private String title;
  private String msg;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getLinktext() {
    return linktext;
  }

  public void setLinktext(String linktext) {
    this.linktext = linktext;
  }

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
}
