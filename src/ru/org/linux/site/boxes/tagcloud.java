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
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import ru.org.linux.boxlet.Boxlet;
import ru.org.linux.site.LorDataSource;
import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.UtilException;

public final class tagcloud extends Boxlet {
  @Override
  public String getContentImpl(ProfileHashtable profile) throws IOException, SQLException {
    Connection db = null;
    try {
      db = LorDataSource.getConnection();

      Map<String,Double> ht = new TreeMap<String,Double>();
      StringBuilder out = new StringBuilder();
      int tags = profile.getInt("tags");

      out.append("<h2>Облако Меток</h2><h3>Наиболее используемые метки</h3>");
      out.append("<div align=\"center\">");

      PreparedStatement st = db.prepareStatement("select value,counter from tags_values where counter>0 order by counter desc limit ?");
      st.setInt(1,tags);

      ResultSet rs = st.executeQuery();
      double maxc = 1;
      double minc = -1;
      while (rs.next()) {
        String tag = rs.getString("value");
        double cnt = Math.log(rs.getInt("counter"));

        if (cnt>maxc) {
          maxc = cnt;
        }

        if (minc<0 || cnt<minc) {
          minc = cnt;
        }

        ht.put(tag, cnt);
      }
      rs.close();

      if (minc<0) {
        minc = 0;
      }

      for (String tag : ht.keySet()) {
        double cnt = ht.get(tag);

        long weight = Math.round(10*(cnt-minc)/(maxc-minc));

        out.append("<a class=\"cloud").append(URLEncoder.encode(Long.toString(weight), "UTF-8")).append("\" href=\"view-news.jsp?section=1&amp;tag=");
        out.append(tag).append("\">").append(tag).append("</a>").append(' ');
      }

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
    return "Наиболее используемые метки";
  }

  @Override
  public String getVariantID(ProfileHashtable prof) {
    return "tag=" + prof.getInt("tags");
  }

  @Override
  public Date getExpire() {
    return new Date(new Date().getTime() + 5*60*1000);
  }
}

