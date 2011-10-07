package ru.org.linux.spring;

public class EditMessageRequest {
  private String url;
  private String linktext;
  private String title;
  private String msg;
  private Boolean minor;
  private int bonus = 3;

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

  public Boolean getMinor() {
    return minor;
  }

  public void setMinor(Boolean minor) {
    this.minor = minor;
  }

  public int getBonus() {
    return bonus;
  }

  public void setBonus(int bonus) {
    this.bonus = bonus;
  }
}
