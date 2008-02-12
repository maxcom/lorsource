package ru.org.linux.site.boxes;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ru.org.linux.boxlet.Boxlet;
import ru.org.linux.site.config.SQLConfig;
import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.UtilException;

public final class tagcloud extends Boxlet {
  public String getContentImpl(ProfileHashtable profile) throws IOException, SQLException, UtilException {
    Connection db = null;
    try {
      db = ((SQLConfig) config).getConnection();
      Map<String,Integer> ht = new HashMap<String,Integer>();
      StringBuffer out = new StringBuffer();
      int tags = profile.getInt("tags");

      out.append("<h2>Облако Меток</h2><h3>Наиболее используемые метки</h3>");

      PreparedStatement st = db.prepareStatement("select value,counter from tags_values where counter>0 order by counter desc limit ?");
      st.setInt(1,tags);

      ResultSet rs = st.executeQuery();
      int maxc = 0;
      int minc = 0;
      while (rs.next()) {
        String tag = rs.getString("value");
        Integer cnt = rs.getInt("counter");
        if (cnt>maxc) {
          maxc = cnt;
        }
        if (cnt<minc || minc==0) {
          minc = cnt;
        }
        ht.put(tag, cnt);
      }
      rs.close();

      int scale = maxc-minc;
      if (scale>10) {
        scale /= 10;
      }
      if (scale<1) {
        scale = 1;
      }
      for (String tag : ht.keySet()) {
        int cnt = ht.get(tag);
        out.append("<a class=\"cloud").append(Math.round((1 + cnt - minc) / scale)).append("\" href=\"index.jsp?tag=");
        out.append(tag).append("\">").append(tag).append("</a>").append(" ");
      }
      out.append("<br>");
      
      return out.toString();
    } finally {
      if (db != null) {
        ((SQLConfig) config).SQLclose();
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

