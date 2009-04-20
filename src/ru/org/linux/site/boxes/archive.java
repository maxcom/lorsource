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
import ru.org.linux.util.BadDateException;
import ru.org.linux.util.DateUtil;
import ru.org.linux.util.ProfileHashtable;

public final class archive extends Boxlet {
  @Override
  public String getContentImpl(ProfileHashtable profile) throws SQLException, BadDateException {
    Connection db = null;

    try {
      db = LorDataSource.getConnection();
      StringBuilder out = new StringBuilder();

      out.append("<h2><a href=\"view-news-archive.jsp?section=1\">Архив Новостей</a></h2>");
      out.append("<div class=\"boxlet_content\">");
      Statement st = db.createStatement();
      ResultSet rs = st.executeQuery("select year, month, c from monthly_stats where section=1 order by year desc, month desc limit 13");

      while (rs.next()) {
        int year = rs.getInt("year");
        int month = rs.getInt("month");
        out.append("<a href=\"view-news.jsp?year=").append(year).append("&amp;month=").append(month).append("&amp;section=1\">").append(year).append(' ').append(DateUtil.getMonth(month)).append("</a> (").append(rs.getInt("c")).append(")<br>");
      }
      rs.close();

      out.append("<br>&gt;&gt;&gt; <a href=\"view-news-archive.jsp?section=1\"> Предыдущие месяцы</a> (с октября 1998)");
      out.append("</div>");

      return out.toString();
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  @Override
  public String getInfo() {
    return "Архив новостей по месяцам";
  }

  @Override
  public Date getExpire() {
    return new Date(new Date().getTime() + 5*60*1000);
  }
}
