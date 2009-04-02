/*
 * Copyright 1998-2009 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.site.boxes;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import ru.org.linux.boxlet.Boxlet;
import ru.org.linux.site.LorDataSource;
import ru.org.linux.site.Poll;
import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.UtilException;

public final class poll extends Boxlet {
  public String getContentImpl(ProfileHashtable profile) throws SQLException, UtilException {
    Connection db = null;
    try {
      db = LorDataSource.getConnection();

      Poll poll = Poll.getCurrentPoll(db);

      StringBuffer out = new StringBuffer();

      out.append("<h2><a href=\"view-news.jsp?section=5\">Опрос</a></h2>");
      out.append("<h3>").append(poll.getTitle()).append("</h3>");

      Statement st = db.createStatement();
      ResultSet rs = st.executeQuery("SELECT id, label FROM votes WHERE vote=" + poll.getId() + " ORDER BY id");

      out.append("<form method=GET action=vote.jsp>");
      out.append("<input type=hidden name=voteid value=").append(poll.getId()).append('>');
      out.append("<input type=hidden name=msgid value=").append(poll.getTopicId()).append('>');
      while (rs.next()) {
        out.append("<input type=radio name=vote value=").append(rs.getInt("id")).append('>').append(rs.getString("label")).append("<br>");
      }
      rs.close();

      out.append("<input type=submit value=vote>");
      out.append("</form><br>");
      out.append("<a href=\"view-vote.jsp?vote=").append(poll.getId()).append("\">результаты</a>");

      rs = st.executeQuery("SELECT sum(votes) as s FROM votes WHERE vote=" + poll.getId());
      rs.next();
      out.append(" (").append(rs.getInt("s")).append(" голосов)");
      out.append("<br><a href=\"view-news.jsp?section=5\">итоги прошедших опросов...</a>");
      out.append("<br>[<a href=\"add-poll.jsp\">добавить опрос</a>]");
      return out.toString();
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  public String getInfo() {
    return "Опрос";
  }

  public String getVariantID(ProfileHashtable prof) throws UtilException {
    return "";
  }

  public Date getExpire() {
    return new Date(new Date().getTime() + 60*1000);
  }

}
