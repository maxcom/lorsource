package ru.org.linux.spring;

public class AddCommentRequest {
  private String preview;
  private String mode;
  private String msg;
  private Integer replyto;
  private String title;
  private int topic;

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

  public Integer getReplyto() {
    return replyto;
  }

  public void setReplyto(Integer replyto) {
    this.replyto = replyto;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public int getTopic() {
    return topic;
  }

  public void setTopic(int topic) {
    this.topic = topic;
  }
}
