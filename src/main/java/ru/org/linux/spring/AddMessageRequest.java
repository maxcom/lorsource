package ru.org.linux.spring;

import ru.org.linux.poll.Poll;
import ru.org.linux.site.Group;
import ru.org.linux.site.User;

public class AddMessageRequest {
  private String title;
  private String msg;
  private String url;
  private Group group;
  private String linktext;
  private String mode;
  private String tags;
  private boolean noinfo;
  private String[] poll = new String[Poll.MAX_POLL_SIZE];
  private boolean multiSelect;

  private User nick;
  private String password;

  private String preview;

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

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public String getTags() {
    return tags;
  }

  public void setTags(String tags) {
    this.tags = tags;
  }

  public boolean isNoinfo() {
    return noinfo;
  }

  public void setNoinfo(boolean noinfo) {
    this.noinfo = noinfo;
  }

  public String getPreview() {
    return preview;
  }

  public void setPreview(String preview) {
    this.preview = preview;
  }

  public boolean isPreviewMode() {
    return preview!=null;
  }

  public String[] getPoll() {
    return poll;
  }

  public void setPoll(String[] poll) {
    this.poll = poll;
  }

  public boolean isMultiSelect() {
    return multiSelect;
  }

  public void setMultiSelect(boolean multiSelect) {
    this.multiSelect = multiSelect;
  }
}
