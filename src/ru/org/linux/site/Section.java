package ru.org.linux.site;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Section {
  private final String name;
  private final boolean browsable;
  private final boolean linkup;
  private final boolean imagepost;

  public Section(Connection db, int id) throws SQLException, BadSectionException {
    Statement st = db.createStatement();
    ResultSet rs = st.executeQuery("SELECT name, browsable, linkup, imagepost FROM sections WHERE id="+id);

    if (!rs.next()) {
      throw new BadSectionException(id);
    }

    name = rs.getString("name");
    browsable = rs.getBoolean("browsable");
    linkup = rs.getBoolean("linkup");
    imagepost = rs.getBoolean("imagepost");
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
}
