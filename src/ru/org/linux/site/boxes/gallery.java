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
import java.io.StringWriter;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Vector;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.VelocityContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.org.linux.boxlet.Boxlet;
import ru.org.linux.site.LorDataSource;
import ru.org.linux.site.config.PropertiesConfig;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ImageInfo;
import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.StringUtil;

public final class gallery extends Boxlet {

  private static final Log log = LogFactory.getLog(gallery.class);

  public String getContent(ProfileHashtable profileHashtable) throws IOException, SQLException {
    Connection db = null;
    StringWriter writer = new StringWriter();
    try {
      db = LorDataSource.getConnection();

      Statement st = db.createStatement();
      ResultSet rs = st.executeQuery("SELECT topics.id as msgid, topics.stat1, topics.title, topics.url," +
        " topics.linktext, nick FROM topics, sections, groups, users WHERE groups.id=topics.groupid" +
        " AND groups.section=sections.id AND users.id=topics.userid AND topics.moderate AND sections.id=3 AND NOT deleted " +
        " ORDER BY commitdate DESC LIMIT 3");

      Vector<GalleryItem> items = new Vector<GalleryItem>();
      while (rs.next()) {
        GalleryItem item = createGalleryItem(rs);
        items.add(item);
      }

      try {
        VelocityEngine ve = new VelocityEngine();
        ve.init();
        VelocityContext vc = new VelocityContext();
        vc.put("items", items);
        //todo: template loader
        ve.mergeTemplate("gallery.vm", "UTF-8", vc, writer);
      } catch (Exception e) {
        log.error(e);
      }
    } finally {
      if (db != null) {
        db.close();
      }
    }
    return writer.toString();
  }

  private GalleryItem createGalleryItem(ResultSet rs) throws SQLException, IOException {
    GalleryItem item = new GalleryItem();
    item.setMsgid(rs.getInt("msgid"));
    item.setStat(rs.getInt("stat1"));
    item.setTitle(rs.getString("title"));
    item.setUrl("url");
    item.setIcon("linktext");
    item.setNick("nick");

    String htmlPath = ((PropertiesConfig) config).getProperties().getProperty("HTMLPathPrefix");

    try {
      item.setInfo(new ImageInfo(htmlPath + item.getIcon()));
      item.setImginfo(new ImageInfo(htmlPath + item.getUrl()));
    } catch (BadImageException e) {
      log.error(e);
    }

    return item;
  }

  public String getContentImpl(ProfileHashtable profile) throws IOException, SQLException {
    Connection db = null;
    try {
      db = LorDataSource.getConnection();

      StringBuilder out = new StringBuilder();

      out.append("<h2><a href=\"view-news.jsp?section=3\">Галерея</a></h2>");
      out.append("<div class=\"boxlet_content\">");
      out.append(" <h3>Последние скриншоты</h3>");
      Statement st = db.createStatement();
      ResultSet rs = st.executeQuery("SELECT topics.id as msgid, topics.stat1, topics.title, topics.url, topics.linktext, nick FROM topics, sections, groups, users WHERE groups.id=topics.groupid AND groups.section=sections.id AND users.id=topics.userid AND topics.moderate AND sections.id=3 AND NOT deleted ORDER BY commitdate DESC LIMIT 3");

      while (rs.next()) {
        String icon = rs.getString("linktext");
        String img = rs.getString("url");
        String htmlPath = ((PropertiesConfig) config).getProperties().getProperty("HTMLPathPrefix");
        String nick = rs.getString("nick");
        String title = StringUtil.makeTitle(rs.getString("title"));

        out.append("<div align=\"center\"><a href=\"view-message.jsp?msgid=").append(rs.getInt("msgid")).append("\">");

        try {
          ImageInfo info = new ImageInfo(htmlPath + icon);

          out.append("<img border=1 src=\"/").append(icon).append("\" alt=\"Скриншот: ").append(title).append("\" ").append(info.getCode()).append('>');
        } catch (BadImageException e) {
          out.append("[bad image] <img border=1 src=\"").append(icon).append("\" alt=\"Скриншот: ").append(title).append("\" " + '>');
        }

        out.append("</a></div><br>");

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
    return "Последние добавления в галерею";
  }

  @Override
  public String getVariantID(ProfileHashtable prof) {
    return "";
  }

  @Override
  public Date getExpire() {
    return new Date(new Date().getTime() + 2 * 60 * 1000);
  }

}
