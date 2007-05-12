package ru.org.linux.site;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import ru.org.linux.site.config.SQLConfig;

public class BannerRotator2 {
  private final int width;
  private final int height;
  private final SQLConfig config;

  public BannerRotator2(SQLConfig Config, int Width, int Height) {
    width = Width;
    height = Height;
    config = Config;
  }

  public String getBanner() throws SQLException {
    Connection db = config.getConnection("banner rotator");

    Statement st = db.createStatement();
    ResultSet rs = st.executeQuery("SELECT id, banner FROM banners WHERE today>0 AND total>0 ORDER BY total DESC LIMIT 1");
    if (!rs.next()) {
      return "";
    }

    String banner = rs.getString("banner");
    int id = rs.getInt("id");

    rs.close();

    st.executeUpdate("UPDATE banners SET total=total-1,today=today-1 WHERE id=" + id);

    return ("<a href=\"/forward.jsp?id=" + id + "\"><img src=\"" + banner + "\" width=" + width + " height=" + height + " alt=adv border=0></a>");
  }
}
