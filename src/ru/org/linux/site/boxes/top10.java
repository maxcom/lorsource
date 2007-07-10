package ru.org.linux.site.boxes;

import java.io.IOException;
import java.sql.*;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import ru.org.linux.boxlet.Boxlet;
import ru.org.linux.site.config.SQLConfig;
import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.UtilException;

public final class top10 extends Boxlet {
  public String getContentImpl(ProfileHashtable profile) throws IOException, SQLException, UtilException {
    Connection db = null;
    try {
      db = ((SQLConfig) config).getConnection("top10");
      Map ht = new Hashtable();
      StringBuffer out = new StringBuffer();
      double messages = profile.getInt("messages");

      out.append("<h2>Top 10</h2><h3>Наиболее обсуждаемые темы этого месяца</h3>");
      Statement st = db.createStatement();

      ResultSet rs = st.executeQuery("select msgid, mess_order from top10");
      while (rs.next()) {
        Integer msgid = new Integer(rs.getInt("msgid"));
        Integer mess_order = new Integer(rs.getInt("mess_order"));
        ht.put(msgid, mess_order);
      }
      rs.close();

      rs = st.executeQuery("select topics.id as msgid, topics.title, lastmod, stat1 as c from topics where age('now', topics.postdate)<'1 month 1 day' and not deleted and notop is null order by c desc, msgid limit 10");
      int order = 0;
      while (rs.next()) {
        order++;
        int c = rs.getInt("c");
        int msgid = rs.getInt("msgid");
        Integer msg = new Integer(msgid);
        Timestamp lastmod = rs.getTimestamp("lastmod");
        if (lastmod == null) {
          lastmod = new Timestamp(0);
        }

        if ((ht.get(msg) == null)
            || (((Integer) ht.get(msg)).intValue() > order)) {
          out.append("<img src=\"/" + profile.getString("style") + "/img/arrow.gif\" alt=\"[up]\" width=10 height=12> ");
        } else {
          out.append("* ");
        }

        if (profile.getBoolean("SearchMode")) {
          out.append("<a href=\"view-message.jsp?msgid=" + msgid + "&amp;page=0\">" + rs.getString("title") + "</a> (" + c + ")<br>");
        } else {
          out.append("<a href=\"jump-message.jsp?msgid=" + msgid + "&amp;lastmod=" + lastmod.getTime() + "&amp;page=0\">" + rs.getString("title") + "</a>");
          int pages = (int) Math.ceil(c / messages);
          if (pages > 1) {
            out.append(" (стр.");
            for (int i = 0; i < pages; i++) {
              out.append(" <a href=\"jump-message.jsp?msgid=" + msgid + "&amp;lastmod=" + lastmod.getTime()+"&amp;page=" + i + "\">" + (i + 1) + "</a>");
            }
            out.append(')');
          }

          out.append(" (" + c + ")<br>");
        }
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

  public String getVariantID(ProfileHashtable prof, Properties request) throws UtilException {
    return "SearchMode=" + prof.getBoolean("SearchMode") + "&messages=" + prof.getInt("messages") + "&style=" + prof.getString("style");
  }

  public long getVersionID(ProfileHashtable profile, Properties request) {
    long time = new Date().getTime();

    return time - time % (5 * 60 * 1000); // 5 min
  }
}
