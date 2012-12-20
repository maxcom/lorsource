package ru.org.linux.comment;

public class ApiCommentTopicInfo {
  private final int id;
  private final String link;

  private final boolean commentsAllowed;

  public ApiCommentTopicInfo(int id, String link, boolean commentsAllowed) {
    this.id = id;
    this.link = link;
    this.commentsAllowed = commentsAllowed;
  }

  public int getId() {
    return id;
  }

  public String getLink() {
    return link;
  }

  public boolean isCommentsAllowed() {
    return commentsAllowed;
  }
}
