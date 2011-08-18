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

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import ru.org.linux.spring.dao.GroupDao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Group {
  private boolean moderate;
  private boolean imagepost;
  private boolean votepoll;
  private boolean havelink;
  private int section;
  private String linktext;
  private String sectionName;
  private String title;
  private String urlName;
  private String image;
  private int restrictTopics;
  private int restrictComments;
  private int id;

  private int stat1;
  private int stat2;
  private int stat3;

  private String info;
  private String longInfo;

  private boolean resolvable;

  @Deprecated
  public static Group getGroup(Connection db, int id) throws SQLException, BadGroupException {
    SingleConnectionDataSource scds = new SingleConnectionDataSource(db, true);
    JdbcTemplate jdbcTemplate = new JdbcTemplate(scds);

    return GroupDao.getGroup(jdbcTemplate, id);
  }

  public Group(Connection db, int section, String urlname) throws SQLException, BadGroupException {
    ResultSet rs = null;
    PreparedStatement pst = null;
    try {
      pst = db.prepareStatement("SELECT sections.moderate, sections.preformat, imagepost, vote, section, havelink, linktext, sections.name as sname, title, urlname, image, restrict_topics, restrict_comments,stat1,stat2,stat3,groups.id, groups.info, groups.longinfo, groups.resolvable FROM groups, sections WHERE groups.urlname=? AND groups.section=sections.id AND groups.section=?");

      pst.setString(1, urlname);
      pst.setInt(2, section);
      
      rs = pst.executeQuery();

      if (!rs.next()) {
        throw new BadGroupException("Группа " + id + " не существует");
      }

      init(rs);
    } finally {
      if (pst != null) {
        pst.close();
      }

      if (rs != null) {
        rs.close();
      }
    }
  }

  public Group(ResultSet rs) throws SQLException {
    init(rs);
  }

  public static List<Group> getGroups(Connection db, Section section) throws SQLException {
    Statement st = db.createStatement();

    ResultSet rs = st.executeQuery("SELECT sections.moderate, sections.preformat, imagepost, vote, section, havelink, linktext, sections.name as sname, title, urlname, image, restrict_topics, restrict_comments, stat1,stat2,stat3,groups.id,groups.info,groups.longinfo,groups.resolvable FROM groups, sections WHERE sections.id=" + section.getId() + " AND groups.section=sections.id ORDER BY id");

    List<Group> list = new ArrayList<Group>();

    while(rs.next()) {
      Group group = new Group(rs);

      list.add(group);
    }

    return list;
  }

  private void init(ResultSet rs) throws SQLException {
    id = rs.getInt("id");
    moderate = rs.getBoolean("moderate");
    imagepost = rs.getBoolean("imagepost");
    votepoll = rs.getBoolean("vote");
    section = rs.getInt("section");
    havelink = rs.getBoolean("havelink");
    linktext = rs.getString("linktext");
    sectionName = rs.getString("sname");
    title = rs.getString("title");
    urlName = rs.getString("urlname");
    image = rs.getString("image");
    restrictTopics = rs.getInt("restrict_topics");
    restrictComments = rs.getInt("restrict_comments");

    stat1 = rs.getInt("stat1");
    stat2 = rs.getInt("stat2");
    stat3 = rs.getInt("stat3");


    info = rs.getString("info");
    longInfo = rs.getString("longinfo");
    resolvable = rs.getBoolean("resolvable");
  }

  public boolean isImagePostAllowed() {
    return imagepost;
  }

  public boolean isPollPostAllowed() {
    return votepoll;
  }

  public int getSectionId() {
    return section;
  }

  public boolean isModerated() {
    return moderate;
  }

  public boolean isLinksAllowed() {
    return havelink;
  }

  public String getDefaultLinkText() {
    return linktext;
  }

  public String getSectionName() {
    return sectionName;
  }

  public String getTitle() {
    return title;
  }

  public String getImage() {
    return image;
  }

  public boolean isTopicsRestricted() {
    return restrictTopics != 0;
  }

  public int getTopicsRestriction() {
    return restrictTopics;
  }

  public boolean isTopicPostingAllowed(User currentUser) {
    if (!isTopicsRestricted()) {
      return true;
    }

    if (currentUser==null) {
      return false;
    }

    if (currentUser.isBlocked()) {
      return false;
    }

    if (restrictTopics==-1) {
      return currentUser.canModerate();
    } else {
      return currentUser.getScore() >= restrictTopics;
    }
  }

  public boolean isCommentsRestricted() {
    return restrictComments != 0;
  }

  public int getCommentsRestriction() {
    return restrictComments;
  }

  public boolean isCommentPostingAllowed(User currentUser) {
    if (!isCommentsRestricted()) {
      return true;
    }

    if (restrictComments==-1) {
      return currentUser.canModerate();
    }

    return currentUser.getScore() >= restrictComments;
  }

  public int getId() {
    return id;
  }

  public int getStat1() {
    return stat1;
  }

  public int getStat2() {
    return stat2;
  }

  public int getStat3() {
    return stat3;
  }

  public String getInfo() {
    return info;
  }

  public int calcTopicsCount(Connection db, boolean showDeleted) throws SQLException {
    Statement st = null;

    try {
      st = db.createStatement();

      String query = "SELECT count(topics.id) " +
        "FROM topics WHERE " +
        (moderate?"moderate AND ":"") +
        "groupid=" + id;

      if (!showDeleted) {
        query+=" AND NOT topics.deleted";
      }

      ResultSet rs = st.executeQuery(query);

      if (rs.next()) {
        return rs.getInt("count");
      } else {
        return 0;
      }
    } finally {
      if (st != null) {
        st.close();
      }
    }
  }

  public String getLongInfo() {
    return longInfo;
  }

  public void setInfo(String info) {
    this.info = info;
  }

  public void setLongInfo(String longInfo) {
    this.longInfo = longInfo;
  }

  public boolean isResolvable() {
    return resolvable;
  }

  public String getSectionLink() {
    return Section.getSectionLink(section);
  }

  public String getUrl() {
    return getSectionLink()+urlName+ '/';
  }

  public String getUrlName() {
    return urlName;
  }

  public String getArchiveLink(int year, int month) {
    return getUrl() +year+ '/' +month+ '/';
  }

  public void setTitle(String title) {
    this.title = title;
  }
}

