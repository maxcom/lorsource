package ru.org.linux.section;

import java.sql.*;

/**
 * DTO-класс, отражающий структуру таблицы.
 */
public class SectionDto {
  private final String title;
  private final boolean imagePost;
  private final boolean premoderated;
  private final int id;
  private final boolean votePoll;
  private final String scrollMode;
  private final String name;
  private final String link;
  private final String feedLink;
  private final int minCommentScore;



  public SectionDto(ResultSet rs) throws SQLException {
    this(
      rs.getString("title"),
      rs.getBoolean("moderate"),
      rs.getBoolean("imagePost"),
      rs.getInt("id"),
      rs.getBoolean("vote"),
      rs.getString("scroll_mode"),
      rs.getString("name"),
      rs.getString("link"),
      rs.getString("feed_link"),
      rs.getInt("min_comment_score")
    );
  }

  public SectionDto(
    String title,
    boolean imagePost,
    boolean premoderated,
    int id,
    boolean votePoll,
    String scrollMode,
    String name,
    String link,
    String feedLink,
    int minCommentScore
  ) {
    this.title = title;
    this.imagePost = imagePost;
    this.premoderated = premoderated;
    this.id = id;
    this.votePoll = votePoll;
    this.scrollMode = scrollMode;
    this.name = name;
    this.link = link;
    this.feedLink = feedLink;
    this.minCommentScore = minCommentScore;
  }

  public String getTitle() {
    return title;
  }

  public boolean isImagePost() {
    return imagePost;
  }

  public boolean isVotePoll() {
    return votePoll;
  }

  public String getScrollMode() {
    return scrollMode;
  }

  public int getId() {
    return id;
  }

  public boolean isPremoderated() {
    return premoderated;
  }

  public String getName() {
    return name;
  }

  public String getLink() {
    return link;
  }

  public String getFeedLink() {
    return feedLink;
  }

  public int getMinCommentScore() {
    return minCommentScore;
  }
}
