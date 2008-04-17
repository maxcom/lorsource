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
  public String getContentImpl(ProfileHashtable profile) throws IOException, SQLException, UtilException {
    Connection db = null;
    try {
      db = LorDataSource.getConnection();

      Map<String,Double> ht = new TreeMap<String,Double>();
      StringBuffer out = new StringBuffer();
      int tags = profile.getInt("tags");

      out.append("<h2>Облако Меток</h2><h3>Наиболее используемые метки</h3>");

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
        out.append(tag).append("\">").append(tag).append("</a>").append(" ");
      }
      out.append("<br>");
      
      return out.toString();
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  public String getInfo() {
    return "Наиболее используемые метки";
  }

  public String getVariantID(ProfileHashtable prof) throws UtilException {
    return "tag=" + prof.getInt("tags");
  }

  public Date getExpire() {
    return new Date(new Date().getTime() + 5*60*1000);
  }
}

