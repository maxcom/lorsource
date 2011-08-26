package ru.org.linux.spring;

import ru.org.linux.site.Comment;
import ru.org.linux.site.Message;
import ru.org.linux.site.User;

public class AddCommentRequest {
  private String preview;
  private String mode;
  private String msg;
  private Comment replyto;
  private String title;
  private Message topic;

  private User nick;
  private String password;

  public void setPreview(String preview) {
    this.preview = preview;
  }

  public String getPreview() {
    return preview;
  }

  public boolean isPreviewMode() {
    return preview!=null;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public String getMsg() {
    return msg;
  }

  public void setMsg(String msg) {
    this.msg = msg;
  }

  public Comment getReplyto() {
    return replyto;
  }

  public void setReplyto(Comment replyto) {
    this.replyto = replyto;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Message getTopic() {
    return topic;
  }

  public void setTopic(Message topic) {
    this.topic = topic;
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