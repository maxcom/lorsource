package ru.org.linux.site.boxes;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Properties;

import ru.org.linux.boxlet.Boxlet;
import ru.org.linux.site.config.PropertiesConfig;
import ru.org.linux.site.config.SQLConfig;
import ru.org.linux.site.Poll;
import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.UtilException;

public final class poll extends Boxlet {
  public String getContentImpl(ProfileHashtable profile) throws SQLException, UtilException {
    Connection db = null;
    try {
      db = ((SQLConfig) config).getConnection("poll");

      Poll poll = Poll.getCurrentPoll(db);

      StringBuffer out = new StringBuffer();

      out.append("<h2><a href=\"votes.jsp\">Опрос</a></h2>");
      out.append("<h3>" + poll.getTitle() + "</h3>");

      Statement st = db.createStatement();
      ResultSet rs = st.executeQuery("SELECT id, label FROM votes WHERE vote=" + poll.getId() + " ORDER BY id");

      out.append("<form method=GET action=vote.jsp>");
      out.append("<input type=hidden name=voteid value=" + poll.getId() + '>');
      while (rs.next()) {
        out.append("<input type=radio name=vote value=" + rs.getInt("id") + '>' + rs.getString("label") + "<br>");
      }
      rs.close();

      out.append("<input type=submit value=vote>");
      out.append("</form><br>");
      out.append("<a href=\"view-vote.jsp?vote=" + poll.getId() + "\">результаты</a>");

      rs = st.executeQuery("SELECT sum(votes) as s FROM votes WHERE vote=" + poll.getId());
      rs.next();
      out.append(" (" + rs.getInt("s") + " голосов)");
      out.append("<br><a href=\"votes.jsp\">итоги прошедших опросов...</a>");
      out.append("<br>[<a href=\"add-poll.jsp\">добавить опрос</a>]");
      return out.toString();
    } finally {
      if (db != null) {
        ((SQLConfig) config).SQLclose();
      }
    }
  }

  public String getInfo() {
    return "Опрос";
  }

  public String getVariantID(ProfileHashtable prof, Properties request) throws UtilException {
    return "SearchMode=" + prof.getBooleanProperty("SearchMode");
  }

  public long getVersionID(ProfileHashtable profile, Properties request) {
    long time = new Date().getTime();

    return time - time % (1 * 60 * 1000); // 1 min
  }

}
