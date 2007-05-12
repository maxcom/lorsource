package ru.org.linux.site.boxes;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Properties;

import ru.org.linux.boxlet.Boxlet;
import ru.org.linux.site.config.SQLConfig;
import ru.org.linux.util.BadDateException;
import ru.org.linux.util.DateUtil;
import ru.org.linux.util.ProfileHashtable;

public final class archive extends Boxlet {
  public String getContentImpl(ProfileHashtable profile) throws IOException, SQLException, BadDateException {
    Connection db = null;

    try {
      db = ((SQLConfig) config).getConnection("archive");
      StringBuffer out = new StringBuffer();

      out.append("<h2><a href=\"view-news-archive.jsp?section=1\">Архив Новостей</a></h2>");
      Statement st = db.createStatement();
      ResultSet rs = st.executeQuery("select year, month, c from monthly_stats where section=1 order by year desc, month desc limit 13");

      while (rs.next()) {
        int year = rs.getInt("year");
        int month = rs.getInt("month");
        out.append("<a href=\"view-news.jsp?year=" + year + "&amp;month=" + month + "&amp;section=1\">" + year + ' ' + DateUtil.getMonth(month) + "</a> (" + rs.getInt("c") + ")<br>");
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

  public long getVersionID(ProfileHashtable profile, Properties request) {
    long time = new Date().getTime();

    return time - time % (5 * 60 * 1000); // 5 min
  }
}
