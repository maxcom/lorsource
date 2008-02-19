package ru.org.linux.site;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Section implements Serializable {
  private final String name;
  private final boolean browsable;
  private final boolean linkup;
  private final boolean imagepost;
  private final boolean moderate;
  private final int id;
  private final boolean votepoll;
  public static final int SCROLL_NOSCROLL = 0;
  public static final int SCROLL_SECTION = 1;
  public static final int SCROLL_GROUP = 2;
  public static final int SECTION_LINKS = 4;

  public Section(Connection db, int id) throws SQLException, BadSectionException {
    this.id = id;

    Statement st = db.createStatement();
    ResultSet rs = st.executeQuery(
        "SELECT name, browsable, linkup, imagepost, vote, moderate " +
            "FROM sections " +
            "WHERE id="+id
    );

    if (!rs.next()) {
      throw new BadSectionException(id);
    }

    name = rs.getString("name");
    browsable = rs.getBoolean("browsable");
    linkup = rs.getBoolean("linkup");
    imagepost = rs.getBoolean("imagepost");
    votepoll = rs.getBoolean("vote");
    moderate = rs.getBoolean("moderate");
  }

  public String getName() {
    return name;
  }

  public boolean isBrowsable() {
    return browsable;
  }

  public boolean isLinkup() {
    return linkup;
  }

  public boolean isImagepost() {
    return imagepost;
  }

  public boolean isVotePoll() {
    return votepoll;
  }

  public static int getScrollMode(int sectionid) {
    switch (sectionid) {
      case 1: /* news*/
      case 3: /* screenshots */
      case 5: /* poll */
        return SCROLL_SECTION;
      case 2: /* forum */
        return SCROLL_GROUP;
      default:
        return SCROLL_NOSCROLL;
    }
  }

  public int getId() {
    return id;
  }

  public boolean isPremoderated() {
    return moderate;
  }

  public String getAddText() {
    if (id==4) {
      return "Добавить ссылку";
    } else {
      return "Добавить сообщение";
    }
  }

  public boolean isForum() {
    return id==2;
  }

  public String getTitle() {
    return name;
  }
}
