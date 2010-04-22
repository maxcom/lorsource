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

package ru.org.linux.site;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URLEncoder;
import java.sql.*;
import java.util.*;
import java.util.Date;

import com.danga.MemCached.MemCachedClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ImageInfo;
import ru.org.linux.util.UtilException;

public class NewsViewer {
  private static final Log logger = LogFactory.getLog("ru.org.linux");

  public enum CommitMode {
    COMMITED_ONLY,
    UNCOMMITED_ONLY,
    COMMITED_AND_POSTMODERATED,
    ALL
  }

  private Set<Integer> sections = new HashSet<Integer>();
  private int group = 0;
  private boolean notalks = false;
  private boolean tech = false;
  private int userid = 0;
  private String datelimit = null;
  private String limit="";
  private String tag="";

  private CommitMode commitMode = CommitMode.COMMITED_AND_POSTMODERATED;

  public static void showMediumImage(String htmlPath, Writer out, String url, String subj, String linktext, boolean showMedium) throws IOException {
    try {
      String mediumName = ScreenshotProcessor.getMediumName(url);

      if (!showMedium || !new File(htmlPath, mediumName).exists()) {
        mediumName = linktext;
      }

      ImageInfo iconInfo = new ImageInfo(htmlPath + mediumName);

      out.append("<p>");
      out.append("<a href=\"/").append(url).append("\"><img src=\"/").append(mediumName).append("\" ALT=\"").append(subj).append("\" ").append(iconInfo.getCode()).append(" ></a>");
      out.append("</p>");
    } catch (BadImageException e) {
      logger.warn("Bad image", e);
      out.append("&gt;&gt;&gt; <a href=\"/").append(url).append("\">[BAD IMAGE!] Просмотр</a>");
    } catch (IOException e) {
      logger.warn("Bad image", e);
      out.append("&gt;&gt;&gt; <a href=\"/").append(url).append("\">[BAD IMAGE: IO Exception!] Просмотр</a>");
    }

    out.append("</p>");
  }

  public List<Message> getMessagesCached(Connection db) throws UtilException, SQLException, UserErrorException {
    if (getCacheAge()==0) {
      return getMessages(db);
    }

    MemCachedClient mcc = MemCachedSettings.getClient();

    String cacheId = MemCachedSettings.getId(getVariantID());

    List<Message> res = (List<Message>) mcc.get(cacheId);

    if (res == null) {
      res = getMessages(db);
      mcc.add(cacheId, res, new Date(System.currentTimeMillis()+getCacheAge()));
    }

    return res;
  }

  public List<Message> getMessages(Connection db) throws SQLException,  UserErrorException {
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
        where.append(" AND topics.moderate AND sections.moderate AND commitdate is not null");
        sort = "ORDER BY commitdate DESC";
        break;
      case UNCOMMITED_ONLY:
        where.append(" AND (NOT topics.moderate) AND sections.moderate");
        sort = "ORDER BY postdate DESC";
        break;
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
      where.append(")");
    }

    if (group!=0) {
      where.append(" AND groupid=").append(group);
    }

    if (datelimit!=null) {
      where.append(" AND ").append(datelimit);
    }

    if (userid!=0) {
      where.append(" AND userid=").append(userid);
    }

    if (notalks){
        where.append(" AND not topics.groupid=8404");
    }

    if (tech){
        where.append(" AND not topics.groupid=8404 AND not topics.groupid=4068 AND groups.section=2");
    }

    if (tag!=null && !"".equals(tag)) {
      PreparedStatement pst = db.prepareStatement("SELECT id FROM tags_values WHERE value=? AND counter>0");
      pst.setString(1,tag);
      ResultSet rs = pst.executeQuery();
      if (rs.next()) {
        int tagid=rs.getInt("id");
        if (tagid>0) {
          where.append(" AND topics.moderate AND topics.id IN (SELECT msgid FROM tags WHERE tagid=").append(tagid).append(')');
        }
      } else {
        throw new UserErrorException("Tag not found");
      }
      rs.close();
      pst.close();
    }
    
    ResultSet res = st.executeQuery(
      "SELECT " +
          "postdate, topics.id as msgid, userid, topics.title, " +
          "topics.groupid as guid, topics.url, topics.linktext, user_agents.name as useragent, " +
          "groups.title as gtitle, urlname, vote, havelink, section, topics.sticky, topics.postip, " +
          "postdate<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby, " +
          "commitdate, topics.stat1, postscore, topics.moderate, message, notop,bbcode, " +
          "topics.resolved " +
          "FROM topics " +
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
            sort+" "+limit
    );

    List<Message> messages = new ArrayList<Message>();

    while (res.next()) {
      Message message = new Message(db, res);
      messages.add(message);
    }

    res.close();

    return messages;
  }

  public void setCommitMode(CommitMode commitMode) {
    this.commitMode = commitMode;
  }

  public void setTag(String tag) {
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
        "&tg=" + URLEncoder.encode(tag));

    id.append("&cm="+commitMode);

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

  public long getCacheAge() {
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
    nv.setCommitMode(CommitMode.COMMITED_ONLY);
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
