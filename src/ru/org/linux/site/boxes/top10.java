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

import java.io.IOException;
import java.sql.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ru.org.linux.boxlet.Boxlet;
import ru.org.linux.site.LorDataSource;
import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.UtilException;

public final class top10 extends Boxlet {
  public String getContentImpl(ProfileHashtable profile) throws IOException, SQLException, UtilException {
    Connection db = null;
    try {
      db = LorDataSource.getConnection();

      Map<Integer,Integer> ht = new HashMap<Integer,Integer>();
      StringBuffer out = new StringBuffer();
      double messages = profile.getInt("messages");

      out.append("<h2>Top 10</h2><h3>Наиболее обсуждаемые темы этого месяца</h3>");
      Statement st = db.createStatement();

      ResultSet rs = st.executeQuery("select msgid, mess_order from top10");
      while (rs.next()) {
        ht.put(rs.getInt("msgid"), rs.getInt("mess_order"));
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
            || (ht.get(msgid) > order)) {
          out.append("<img src=\"/").append(profile.getString("style")).append("/img/arrow.gif\" alt=\"[up]\" width=10 height=12> ");
        } else {
          out.append("* ");
        }

        out.append("<a href=\"view-message.jsp?msgid=").append(msgid).append("&amp;lastmod=").append(lastmod.getTime()).append("\">").append(rs.getString("title")).append("</a>");
        int pages = (int) Math.ceil(c / messages);
        if (pages > 1) {
          out.append(" (стр.");
          out.append(" <a href=\"view-message.jsp?msgid=").append(msgid).append("&amp;lastmod=").append(lastmod.getTime()).append("&amp;page=").append(pages-1).append("\">").append(pages).append("</a>");
          out.append(')');
        }

        out.append(" (").append(c).append(")<br>");
      }

      rs.close();

      return out.toString();
    } finally {
      if (db != null) {
        db.close();
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
