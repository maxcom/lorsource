/*
 * Copyright 1998-2010 Linux.org.ru
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

package ru.org.linux.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.org.linux.gallery.Screenshot;
import ru.org.linux.site.MemCachedSettings;
import ru.org.linux.spring.commons.CacheProvider;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ImageInfo;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NewsViewer {
  private static final Log logger = LogFactory.getLog("ru.org.linux");
  private boolean userFavs = false;

  public void setUserFavs(boolean userFavs) {
    this.userFavs = userFavs;
  }

  public enum CommitMode {
    COMMITED_ONLY,
    UNCOMMITED_ONLY,
    POSTMODERATED_ONLY,
    COMMITED_AND_POSTMODERATED,
    ALL
  }

  private final Set<Integer> sections = new HashSet<Integer>();
  private int group = 0;
  private boolean notalks = false;
  private boolean tech = false;
  private int userid = 0;
  private String datelimit = null;
  private String limit="";
  private int tag=0;

  private CommitMode commitMode = CommitMode.COMMITED_AND_POSTMODERATED;

  public static String showMediumImage(String htmlPath, Topic topic, boolean showMedium) {
    StringBuilder out = new StringBuilder();
    String url = topic.getUrl();

    try {
      String mediumName = Screenshot.getMediumName(url);

      if (!showMedium || !new File(htmlPath, mediumName).exists()) {
        mediumName = topic.getLinktext();
      }

      ImageInfo iconInfo = new ImageInfo(htmlPath + mediumName);

      out.append("<p>");
      out.append("<a href=\"/").append(url).append("\"><img src=\"/").append(mediumName).append("\" ALT=\"").append(topic.getTitle()).append("\" ").append(iconInfo.getCode()).append(" ></a>");
      out.append("</p>");
    } catch (BadImageException e) {
      logger.warn("Bad image", e);
      out.append("&gt;&gt;&gt; <a href=\"/").append(url).append("\">[BAD IMAGE!] Просмотр</a>");
    } catch (IOException e) {
      logger.warn("Bad image", e);
      out.append("&gt;&gt;&gt; <a href=\"/").append(url).append("\">[BAD IMAGE: IO Exception!] Просмотр</a>");
    }

    out.append("</p>");

    return out.toString();
  }

  public List<Topic> getMessagesCached(Connection db) throws SQLException {
    if (getCacheAge()==0) {
      return getMessages(db);
    }

    CacheProvider mcc = MemCachedSettings.getCache();

    String cacheId = getVariantID();

    List<Topic> res = (List<Topic>) mcc.getFromCache(cacheId);

    if (res == null) {
      res = getMessages(db);
      mcc.storeToCache(cacheId, res, getCacheAge());
    }

    return res;
  }

  public List<Topic> getMessages(Connection db) throws SQLException {
    Statement st = db.createStatement();

    StringBuilder where = new StringBuilder(
        "NOT deleted"
    );

    String sort = "ORDER BY COALESCE(commitdate, postdate) DESC";

    switch (commitMode) {
      case ALL:
        break;
      case COMMITED_AND_POSTMODERATED:
        where.append(" AND (topics.moderate OR NOT sections.moderate)");
        break;
      case COMMITED_ONLY:
        where.append(" AND sections.moderate AND commitdate is not null");
        sort = "ORDER BY commitdate DESC";
        break;
      case UNCOMMITED_ONLY:
        where.append(" AND (NOT topics.moderate) AND sections.moderate");
        sort = "ORDER BY postdate DESC";
        break;
      case POSTMODERATED_ONLY:
        where.append(" AND NOT sections.moderate");
        sort = "ORDER BY postdate DESC";
        break;
    }

    if (userFavs) {
      sort = "ORDER BY memories.id DESC";
    }

    if (!sections.isEmpty()) {
      where.append(" AND section in (");
      boolean first = true;
      for (int section : sections) {
        if (!first) {
          where.append(',');
        }
        where.append(section);
        first = false;
      }
      where.append(')');
    }

    if (group!=0) {
      where.append(" AND groupid=").append(group);
    }

    if (datelimit!=null) {
      where.append(" AND ").append(datelimit);
    }

    if (userid!=0) {
      if (userFavs) {
        where.append(" AND memories.userid=").append(userid);
      } else {
        where.append(" AND userid=").append(userid);
      }
    }

    if (notalks){
        where.append(" AND not topics.groupid=8404");
    }

    if (tech){
        where.append(" AND not topics.groupid=8404 AND not topics.groupid=4068 AND groups.section=2");
    }

    if (tag!=0) {
        where.append(" AND topics.moderate AND topics.id IN (SELECT msgid FROM tags WHERE tagid=").append(tag).append(')');
    }
    
    ResultSet res = st.executeQuery(
      "SELECT " +
          "postdate, topics.id as msgid, topics.userid, topics.title, " +
          "topics.groupid as guid, topics.url, topics.linktext, ua_id, " +
          "groups.title as gtitle, urlname, vote, havelink, section, topics.sticky, topics.postip, " +
          "postdate<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby, " +
          "commitdate, topics.stat1, postscore, topics.moderate, message, notop,bbcode, " +
          "topics.resolved, restrict_comments, minor " +
          "FROM topics " +
          "INNER JOIN groups ON (groups.id=topics.groupid) " +
          "INNER JOIN sections ON (sections.id=groups.section) " +
          "INNER JOIN msgbase ON (msgbase.id=topics.id) " +
          (userFavs?"INNER JOIN memories ON (memories.topic = topics.id) ":"")+
          "WHERE " + where+ ' ' +
          sort+ ' ' +limit
    );

    List<Topic> messages = new ArrayList<Topic>();

    while (res.next()) {
      Topic message = new Topic(res);
      messages.add(message);
    }

    res.close();

    return messages;
  }

  public void setCommitMode(CommitMode commitMode) {
    this.commitMode = commitMode;
  }

  public void setTag(int tag) {
    this.tag = tag;
  }

  public void addSection(int section) {
    sections.add(section);
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

  public String getVariantID()  {
    StringBuilder id = new StringBuilder("view-news?"+
        "&tg=" + tag);

    id.append("&cm=").append(commitMode);

    for (int section : sections) {
      id.append("&sec=").append(section);
    }

    if (group!=0) {
      id.append("&grp=").append(group);
    }

    if (datelimit!=null) {
      id.append("&dlmt=").append(URLEncoder.encode(datelimit));
    }

    if (userid!=0) {
      id.append("&u=").append(userid);
    }

    if (userFavs) {
      id.append("&f");
    }

    if (limit!=null && limit.length()>0) {
      id.append("&lmt=").append(URLEncoder.encode(limit));
    }

    if (notalks){
        id.append("&notalks=1");
    }
    if (tech){
        id.append("&tech=1");
    }
    return id.toString();
  }

  public int getCacheAge() {
    if (limit==null || limit.length()==0) {
      return 10*60*1000;
    }

    if (commitMode==CommitMode.COMMITED_ONLY) {
      return 0;
    }

    return 30*1000;
  }

  public static NewsViewer getMainpage() {
    NewsViewer nv = new NewsViewer();
    nv.addSection(1);
    nv.limit = "LIMIT 20";
    nv.datelimit = "commitdate>(CURRENT_TIMESTAMP-'1 month'::interval)";
    nv.commitMode = CommitMode.COMMITED_ONLY;
    return nv;
  }

  public void setUserid(int userid) {
    this.userid = userid;
  }

  public void setNotalks(boolean notalks) {
    this.notalks = notalks;
  }

  public void setTech(boolean tech) {
    this.tech = tech;
  }
}
