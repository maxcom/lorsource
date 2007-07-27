package ru.org.linux.site.boxes;

import java.io.IOException;
import java.sql.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ru.org.linux.boxlet.Boxlet;
import ru.org.linux.site.config.SQLConfig;
import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.UtilException;

public final class top10 extends Boxlet {
  public String getContentImpl(ProfileHashtable profile) throws IOException, SQLException, UtilException {
    Connection db = null;
    try {
      db = ((SQLConfig) config).getConnection("top10");
      Map ht = new HashMap();
      StringBuffer out = new StringBuffer();
      double messages = profile.getInt("messages");

      out.append("<h2>Top 10</h2><h3>Наиболее обсуждаемые темы этого месяца</h3>");
      Statement st = db.createStatement();

      ResultSet rs = st.executeQuery("select msgid, mess_order from top10");
      while (rs.next()) {
        Integer msgid = rs.getInt("msgid");
        Integer mess_order = rs.getInt("mess_order");
        ht.put(msgid, mess_order);
      }
      rs.close();

      rs = st.executeQuery("select topics.id as msgid, topics.title, lastmod, stat1 as c from topics where topics.postdate>(CURRENT_TIMESTAMP-'1 month 1 day'::interval) and not deleted and notop is null and groupid!=8404 and groupid!=4068 order by c desc, msgid limit 10");
      int order = 0;
      while (rs.next()) {
        order++;
        int c = rs.getInt("c");
        int msgid = rs.getInt("msgid");
        Timestamp lastmod = rs.getTimestamp("lastmod");
        if (lastmod == null) {
          lastmod = new Timestamp(0);
        }

        if ((ht.get(msgid) == null)
            || ((Integer) ht.get(msgid) > order)) {
          out.append("<img src=\"/").append(profile.getString("style")).append("/img/arrow.gif\" alt=\"[up]\" width=10 height=12> ");
        } else {
          out.append("* ");
        }

        out.append("<a href=\"jump-message.jsp?msgid=").append(msgid).append("&amp;lastmod=").append(lastmod.getTime()).append("\">").append(rs.getString("title")).append("</a>");
        int pages = (int) Math.ceil(c / messages);
        if (pages > 1) {
          out.append(" (стр.");
          for (int i = 1; i < pages; i++) {
            out.append(" <a href=\"jump-message.jsp?msgid=").append(msgid).append("&amp;lastmod=").append(lastmod.getTime()).append("&amp;page=").append(i).append("\">").append(i + 1).append("</a>");
          }
          out.append(')');
        }

        out.append(" (").append(c).append(")<br>");
      }

      rs.close();

      return out.toString();
    } finally {
      if (db != null) {
        ((SQLConfig) config).SQLclose();
      }
    }
  }

  public String getInfo() {
    return "Наиболее обсуждаемые темы этого месяца";
  }

  public String getVariantID(ProfileHashtable prof) throws UtilException {
    return "messages=" + prof.getInt("messages") + "&style=" + prof.getString("style");
  }

  public Date getExpire() {
    return new Date(new Date().getTime() + 5*60*1000);
  }
}
