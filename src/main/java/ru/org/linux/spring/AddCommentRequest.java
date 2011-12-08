package ru.org.linux.spring;

import ru.org.linux.dto.CommentDto;
import ru.org.linux.dto.MessageDto;
import ru.org.linux.dto.UserDto;

public class AddCommentRequest {
  private String preview;
  private String mode;
  private String msg;
  private CommentDto replyto;
  private String title;
  private MessageDto topic;

  private UserDto nick;
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

  public CommentDto getReplyto() {
    return replyto;
  }

  public void setReplyto(CommentDto replyto) {
    this.replyto = replyto;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public MessageDto getTopic() {
    return topic;
  }

  public void setTopic(MessageDto topic) {
    this.topic = topic;
  }

  public UserDto getNick() {
    return nick;
  }

  public void setNick(UserDto nick) {
    this.nick = nick;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
