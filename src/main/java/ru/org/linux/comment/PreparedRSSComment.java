package ru.org.linux.comment;

import ru.org.linux.user.User;

public class PreparedRSSComment {
  private final Comment comment;
  private final User author;
  private final String processedMessage;

  public PreparedRSSComment(Comment comment, User author, String processedMessage) {
    this.comment = comment;
    this.author = author;
    this.processedMessage = processedMessage;
  }

  public Comment getComment() {
    return comment;
  }

  public User getAuthor() {
    return author;
  }

  public String getProcessedMessage() {
    return processedMessage;
  }
}
