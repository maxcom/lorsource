package ru.org.linux.site;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Group {
  private final boolean moderate;
  private final boolean preformat;
  private final boolean lineonly;
  private final boolean imagepost;
  private final boolean votepoll;
  private final boolean havelink;
  private final boolean linkup;
  private final int section;
  private final String linktext;
  private final String sectionName;
  private final String title;
  private final String image;
  private final int restrictTopics;
  private final int id;
  private final boolean browsable;

  public Group(Connection db, int id) throws SQLException, BadGroupException {
    Statement st = null;
    ResultSet rs = null;

    this.id = id;

    try {
      st = db.createStatement();

      rs = st.executeQuery("SELECT sections.moderate, sections.preformat, lineonly, imagepost, vote, section, havelink, linkup, linktext, sections.name, title, image, restrict_topics, sections.browsable FROM groups, sections WHERE groups.id=" + id + " AND groups.section=sections.id");

      if (!rs.next()) {
        throw new BadGroupException("Группа " + id + " не существует");
      }

      moderate = rs.getBoolean("moderate");
      preformat = rs.getBoolean("preformat");
      lineonly = rs.getBoolean("lineonly");
      imagepost = rs.getBoolean("imagepost");
      votepoll = rs.getBoolean("vote");
      section = rs.getInt("section");
      havelink = rs.getBoolean("havelink");
      linkup = rs.getBoolean("linkup");
      linktext = rs.getString("linktext");
      sectionName = rs.getString("name");
      title = rs.getString("title");
      image = rs.getString("image");
      restrictTopics = rs.getInt("restrict_topics");
      browsable = rs.getBoolean("browsable");
    } finally {
      if (st != null) {
        st.close();
      }
      if (rs != null) {
        rs.close();
      }
    }
  }

  public boolean isPreformatAllowed() {
    return preformat;
  }

  public boolean isLineOnly() {
    return lineonly;
  }

  public boolean isImagePostAllowed() {
    return imagepost;
  }

  public boolean isPollPostAllowed() {
    return votepoll;
  }

  public int getSectionId() {
    return section;
  }

  public boolean isModerated() {
    return moderate;
  }

  public boolean isLinksAllowed() {
    return havelink;
  }

  public boolean isLinksUp() {
    return linkup;
  }

  public String getDefaultLinkText() {
    return linktext;
  }

  public String getSectionName() {
    return sectionName;
  }

  public String getTitle() {
    return title;
  }

  public String getImage() {
    return image;
  }

  public boolean isTopicsRestricted() {
    return restrictTopics != 0;
  }

  public int getTopicsRestriction() {
    return restrictTopics;
  }

  public boolean isTopicPostingAllowed(User currentUser) {
    if (!isTopicsRestricted()) {
      return true;
    }

    if (currentUser==null) {
      return false;
    }

    if (currentUser.isBlocked()) {
      return false;
    }

    if (currentUser.getMaxScore()>=100) {
      return true;
    }

    return currentUser.getScore() >= restrictTopics;
  }

  public int getId() {
    return id;
  }

  public boolean isBrowsable() {
    return browsable;
  }
}