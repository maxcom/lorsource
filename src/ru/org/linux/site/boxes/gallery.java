package ru.org.linux.site.boxes;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import ru.org.linux.boxlet.Boxlet;
import ru.org.linux.site.config.PropertiesConfig;
import ru.org.linux.site.config.SQLConfig;
import ru.org.linux.util.*;

public final class gallery extends Boxlet {
  public String getContentImpl(ProfileHashtable profile) throws IOException, SQLException {
    Connection db = null;
    try {
      db = ((SQLConfig) config).getConnection("gallery");
      StringBuffer out = new StringBuffer();

      out.append("<h2><a href=\"view-news.jsp?section=3\">Галерея</a></h2> <h3>Последние скриншоты</h3>");
      Statement st = db.createStatement();
      ResultSet rs = st.executeQuery("SELECT topics.id as msgid, topics.stat1, topics.title, topics.url, topics.linktext, nick FROM topics, sections, groups, users WHERE groups.id=topics.groupid AND groups.section=sections.id AND users.id=topics.userid AND topics.moderate AND sections.id=3 AND NOT deleted ORDER BY commitdate DESC LIMIT 3");

      while (rs.next()) {
        String icon = rs.getString("linktext");
        String img = rs.getString("url");
        String htmlPath = ((PropertiesConfig) config).getProperties().getProperty("HTMLPathPrefix");
        String nick = rs.getString("nick");
        String title = StringUtil.makeTitle(rs.getString("title"));

        out.append("<a href=\"view-message.jsp?msgid=").append(rs.getInt("msgid")).append("\">");

        try {
          ImageInfo info = new ImageInfo(htmlPath + icon);

          out.append("<img border=1 src=\"/").append(icon).append("\" alt=\"Скриншот: ").append(title).append("\" ").append(info.getCode()).append('>');
        } catch (BadImageException e) {
          out.append("[bad image] <img border=1 src=\"").append(icon).append("\" alt=\"Скриншот: ").append(title).append("\" " + '>');
        }

        out.append("</a><br>");

        try {
          ImageInfo imginfo = new ImageInfo(htmlPath + img);

          out.append("<i>").append(imginfo.getWidth()).append('x').append(imginfo.getHeight()).append("</i>, \"").append(title).append("\" от <a href=\"whois.jsp?nick=").append(URLEncoder.encode(nick)).append("\">").append(nick).append("</a>");
        } catch (BadImageException e) {
          out.append("<i>Bad image!</i>, \"").append(title).append("\" от <a href=\"whois.jsp?nick=").append(URLEncoder.encode(nick)).append("\">").append(nick).append("</a>");
        }

        int c = rs.getInt("stat1");
        if (c > 0) {
          out.append(" (").append(c).append(')');
        }

        out.append("<p>");
      }
      rs.close();                          
      out.append("<a href=\"view-news.jsp?section=3\">другие скриншоты...</a>");
      return out.toString();
    } finally {
      if (db != null) {
        ((SQLConfig) config).SQLclose();
      }
    }
  }

  public String getInfo() {
    return "Последние добавления в галерею";
  }

  public String getVariantID(ProfileHashtable prof) throws UtilException {
    return "SearchMode=" + prof.getBoolean("SearchMode");
  }

  public Date getExpire() {
    return new Date(new Date().getTime() + 2 * 60 * 1000);
  }

}
