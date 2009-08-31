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

package ru.org.linux.site;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.servlet.jsp.JspWriter;
import com.danga.MemCached.MemCachedClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ImageInfo;
import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.UtilException;

public class NewsViewer {
  private static final Log logger = LogFactory.getLog("ru.org.linux");

  private boolean viewAll = false;
  private int section = 0;
  private int group = 0;
  private String datelimit = null;
  private String limit="";
  private String tag="";

  public static void showMediumImage(String htmlPath, JspWriter out, String url, String subj, String linktext) throws IOException {
    try {
      out.append("<p>");
      String mediumName = ScreenshotProcessor.getMediumName(url);

      if (!new File(htmlPath, mediumName).exists()) {
        mediumName = linktext;
      }

      ImageInfo iconInfo = new ImageInfo(htmlPath + mediumName);
      ImageInfo info = new ImageInfo(htmlPath + url);

      out.append("<a href=\"/").append(url).append("\"><img src=\"/").append(mediumName).append("\" ALT=\"").append(subj).append("\" ").append(iconInfo.getCode()).append(" ></a>");
      out.append("</p><p>");


      out.append("&gt;&gt;&gt; <a href=\"/").append(url).append("\">Просмотр</a>");
      out.append(" (<i>").append(Integer.toString(info.getWidth())).append('x').append(Integer.toString(info.getHeight())).append(", ").append(info.getSizeString()).append("</i>)");
    } catch (BadImageException e) {
      logger.warn("Bad image", e);
      out.append("&gt;&gt;&gt; <a href=\"/").append(url).append("\">[BAD IMAGE!] Просмотр</a>");
    } catch (IOException e) {
      logger.warn("Bad image", e);
      out.append("&gt;&gt;&gt; <a href=\"/").append(url).append("\">[BAD IMAGE: IO Exception!] Просмотр</a>");
    }

    out.append("</p>");
  }

  public List<Message> getMessagesCached(Connection db, Template tmpl) throws UtilException, SQLException, UserErrorException {
    MemCachedClient mcc = MemCachedSettings.getClient();

    String cacheId = MemCachedSettings.getId(getVariantID(tmpl.getProf()));

    List<Message> res = (List<Message>) mcc.get(cacheId);

    if (res == null) {
      res = getMessages(db);
      mcc.add(cacheId, res, getExpire());
    }

    return res;
  }

  public List<Message> getMessages(Connection db) throws SQLException, UtilException, UserErrorException {
    Statement st = db.createStatement();

    StringBuilder where = new StringBuilder(
        "sections.id=groups.section AND topics.id=msgbase.id AND topics.userid=users.id " +
            "AND topics.groupid=groups.id AND NOT deleted"
    );

    if (!viewAll) {
      where.append(" AND topics.moderate AND sections.moderate");
    } else {
      where.append(" AND (NOT topics.moderate) AND sections.moderate");    
    }

    if (section!=0) {
      where.append(" AND section=").append(section);
    }

    if (group!=0) {
      where.append(" AND groupid=").append(group);
    }

    if (datelimit!=null) {
      where.append(" AND ").append(datelimit);
    }

    if (tag!=null && !"".equals(tag)) {
      PreparedStatement pst = db.prepareStatement("SELECT id FROM tags_values WHERE value=? AND counter>0");
      pst.setString(1,tag);
      ResultSet rs = pst.executeQuery();
      if (rs.next()) {
        int tagid=rs.getInt("id");
        if (tagid>0) {
          where.append(" AND topics.id IN (SELECT msgid FROM tags WHERE tagid=").append(tagid).append(')');
        }
      } else {
        throw new UserErrorException("Tag not found");
      }
      rs.close();
      pst.close();
    }
    
    ResultSet res = st.executeQuery(
      "SELECT " +
          "postdate, topics.id as msgid, users.id as userid, topics.title, " +
          "topics.groupid as guid, topics.url, topics.linktext, user_agents.name as useragent, " +
          "groups.title as gtitle, vote, havelink, section, topics.sticky, topics.postip, " +
          "postdate<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby, " +
          "commitdate, topics.stat1, postscore, topics.moderate, message, notop,bbcode " +
          "FROM topics " +
          "INNER JOIN users ON (users.id=topics.userid) " +
          "INNER JOIN groups ON (groups.id=topics.groupid) " +
          "INNER JOIN sections ON (sections.id=groups.section) " +
          "INNER JOIN msgbase ON (msgbase.id=topics.id) " +
          "LEFT JOIN user_agents ON (user_agents.id=topics.ua_id) " +

//        "SELECT topics.title as subj, topics.lastmod, topics.stat1, postdate, nick, image, " +
//            "groups.title as gtitle, topics.id as msgid, groups.id as guid, " +
//            "topics.url, topics.linktext, imagepost, vote, sections.name as pname, " +
//            "postdate<(CURRENT_TIMESTAMP-expire) as expired, message, bbcode, " +
//            "sections.id as section, NOT topics.sticky AS ssticky, sections.moderate " +
//            "FROM topics,groups,users,sections,msgbase " +
            "WHERE " + where+ ' ' +
            "ORDER BY sticky,commitdate DESC, msgid DESC "+limit
    );

    List<Message> messages = new ArrayList<Message>();

    while (res.next()) {
      Message message = new Message(db, res);
      messages.add(message);
    }

    res.close();

    return messages;
  }

  public void setViewAll(boolean viewAll) {
    this.viewAll = viewAll;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public void setSection(int section) {
    this.section = section;
  }

  public void setGroup(int group) {
    this.group = group;
  }

  public void setDatelimit(String datelimit) {
    this.datelimit = datelimit;
  }

  public void setLimit(String limit) {
    this.limit = limit;
  }

  public String getVariantID(ProfileHashtable prof) throws UtilException {
    StringBuilder id = new StringBuilder("view-news?"+
        "&tg=" + URLEncoder.encode(tag));

    if (viewAll) {
      id.append("&v-all=true");
    }

    if (section!=0) {
      id.append("&sec=").append(section);
    }

    if (group!=0) {
      id.append("&grp=").append(group);
    }

    if (datelimit!=null) {
      id.append("&dlmt=").append(URLEncoder.encode(datelimit));
    }

    if (limit!=null && limit.length()>0) {
      id.append("&lmt=").append(URLEncoder.encode(limit));
    }

    return id.toString();
  }

  public Date getExpire() {
    if (limit==null || limit.length()==0) {
      return new Date(new Date().getTime() + 10*60*1000);
    }

    return new Date(new Date().getTime() + 60*1000);
  }

  public static NewsViewer getMainpage(Properties config, ProfileHashtable profile) {
    NewsViewer nv = new NewsViewer();
    nv.section = 1;
    nv.limit = "LIMIT 20";
    nv.datelimit = "commitdate>(CURRENT_TIMESTAMP-'1 month'::interval)";
    return nv;
  }
}
