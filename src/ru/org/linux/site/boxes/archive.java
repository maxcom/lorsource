package ru.org.linux.site.boxes;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import ru.org.linux.boxlet.Boxlet;
import ru.org.linux.site.config.SQLConfig;
import ru.org.linux.util.BadDateException;
import ru.org.linux.util.DateUtil;
import ru.org.linux.util.ProfileHashtable;

public final class archive extends Boxlet {
  public String getContentImpl(ProfileHashtable profile) throws IOException, SQLException, BadDateException {
    Connection db = null;

    try {
      db = ((SQLConfig) config).getConnection();
      StringBuffer out = new StringBuffer();

      out.append("<h2><a href=\"view-news-archive.jsp?section=1\">Архив Новостей</a></h2>");
      Statement st = db.createStatement();
      ResultSet rs = st.executeQuery("select year, month, c from monthly_stats where section=1 order by year desc, month desc limit 13");

      while (rs.next()) {
        int year = rs.getInt("year");
        int month = rs.getInt("month");
        out.append("<a href=\"view-news.jsp?year=").append(year).append("&amp;month=").append(month).append("&amp;section=1\">").append(year).append(' ').append(DateUtil.getMonth(month)).append("</a> (").append(rs.getInt("c")).append(")<br>");
      }
      rs.close();

      out.append("<br>&gt;&gt;&gt; <a href=\"view-news-archive.jsp?section=1\"> Предыдущие месяцы</a> (с октября 1998)");

      return out.toString();
    } finally {
      if (db != null) {
        ((SQLConfig) config).SQLclose();
      }
    }
  }

  public String getInfo() {
    return "Архив новостей по месяцам";
  }

  public Date getExpire() {
    return new Date(new Date().getTime() + 5*60*1000);
  }
}
