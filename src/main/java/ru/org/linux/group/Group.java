/*
 * Copyright 1998-20166 Linux.org.ru
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

package ru.org.linux.group;

import ru.org.linux.section.Section;
import ru.org.linux.topic.TopicPermissionService;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Group implements Serializable {
  private static final long serialVersionUID = 6173447416543763434L;

  private final boolean moderate;
  private final boolean votepoll;
  private final boolean havelink;
  private final int section;
  private final String linktext;
  private String title;
  private final String urlName;
  private final String image;
  private final int restrictTopics;
  private final int restrictComments;
  private final int id;

  private final int stat3;

  private String info;
  private String longInfo;

  private final boolean resolvable;

  public Group(boolean moderate, boolean votepoll, boolean havelink, int section,
               String linktext, String urlName, String image, int restrictTopics, int restrictComments,
               int id, int stat3, boolean resolvable) {
    this.moderate = moderate;
    this.votepoll = votepoll;
    this.havelink = havelink;
    this.section = section;
    this.linktext = linktext;
    this.urlName = urlName;
    this.image = image;
    this.restrictTopics = restrictTopics;
    this.restrictComments = restrictComments;
    this.id = id;
    this.stat3 = stat3;
    this.resolvable = resolvable;
  }

  public static Group buildGroup(ResultSet rs) throws SQLException {
    int restrict_topics = rs.getInt("restrict_topics");
    if (rs.wasNull()) {
      restrict_topics = TopicPermissionService.POSTSCORE_UNRESTRICTED;
    }

    Group group = new Group(
      rs.getBoolean("moderate"),
            rs.getBoolean("vote"),
      rs.getBoolean("havelink"),
      rs.getInt("section"),
      rs.getString("linktext"),
      rs.getString("urlname"),
      rs.getString("image"),
      restrict_topics,
      rs.getInt("restrict_comments"),
      rs.getInt("id"),
      rs.getInt("stat3"),
      rs.getBoolean("resolvable")
    );

    group.setTitle(rs.getString("title"));
    group.setInfo(rs.getString("info"));
    group.setLongInfo(rs.getString("longinfo"));

    return group;
  }

  public boolean isPollPostAllowed() {
    return votepoll;
  }

  public int getSectionId() {
    return section;
  }

  public boolean isPremoderated() {
    return moderate;
  }

  public boolean isLinksAllowed() {
    return havelink;
  }

  public String getDefaultLinkText() {
    return linktext;
  }

  public String getTitle() {
    return title;
  }

  public String getImage() {
    return image;
  }

  public int getCommentsRestriction() {
    return restrictComments;
  }

  public int getTopicRestriction() {
    return restrictTopics;
  }

  public int getId() {
    return id;
  }

  public int getStat3() {
    return stat3;
  }

  public String getInfo() {
    return info;
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

