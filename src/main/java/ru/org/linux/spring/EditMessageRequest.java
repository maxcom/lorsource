package ru.org.linux.spring;

import java.util.Map;

public class EditMessageRequest {
  private String url;
  private String linktext;
  private String title;
  private String msg;
  private Boolean minor;
  private int bonus = 3;
  private String tags;

  private Map<Integer, String> poll;
  private String[] newPoll = new String[3];
  private boolean multiselect;

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

  public String getTags() {
    return tags;
  }

  public void setTags(String tags) {
    this.tags = tags;
  }

  public Map<Integer, String> getPoll() {
    return poll;
  }

  public void setPoll(Map<Integer, String> poll) {
    this.poll = poll;
  }

  public String[] getNewPoll() {
    return newPoll;
  }

  public void setNewPoll(String[] newPoll) {
    this.newPoll = newPoll;
  }

  public boolean isMultiselect() {
    return multiselect;
  }

  public void setMultiselect(boolean multiselect) {
    this.multiselect = multiselect;
  }
}
