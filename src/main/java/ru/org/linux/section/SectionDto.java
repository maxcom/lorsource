package ru.org.linux.section;

import java.sql.*;

/**
 * User: slavaz
 * Date: 29.12.11
 */
public class SectionDto {
  private final String title;
  private final boolean imagePost;
  private final boolean moderate;
  private final int id;
  private final boolean votePoll;
  private final String scrollMode;


  public SectionDto(ResultSet rs) throws SQLException {
    title = rs.getString("name");
    imagePost = rs.getBoolean("imagePost");
    votePoll = rs.getBoolean("vote");
    moderate = rs.getBoolean("moderate");
    id = rs.getInt("id");
    scrollMode = rs.getString("scroll_mode");
  }

  public SectionDto(String title, boolean imagePost, boolean moderate, int id, boolean votePoll, String scrollMode) {
    this.title = title;
    this.imagePost = imagePost;
    this.moderate = moderate;
    this.id = id;
    this.votePoll = votePoll;
    this.scrollMode = scrollMode;
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
    return moderate;
  }
}
