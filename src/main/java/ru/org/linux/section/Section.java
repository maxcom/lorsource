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

package ru.org.linux.section;

import ru.org.linux.topic.Topic;

import java.io.Serializable;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class Section implements Serializable {
  private static final long serialVersionUID = -2259350244006777910L;

  private int id;
  private String title;
  private boolean imagepost;
  private boolean premoderated;
  private boolean votePoll;

  private SectionScrollModeEnum scrollMode;

  public Section(){
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public boolean isImagepost() {
    return imagepost;
  }

  public void setImagepost(boolean imagepost) {
    this.imagepost = imagepost;
  }

  public boolean isPremoderated() {
    return premoderated;
  }

  public void setPremoderated(boolean premoderated) {
    this.premoderated = premoderated;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public boolean isVotePoll() {
    return votePoll;
  }

  public void setVotePoll(boolean votePoll) {
    this.votePoll = votePoll;
  }

  public SectionScrollModeEnum getScrollMode() {
    return scrollMode;
  }

  public void setScrollMode(SectionScrollModeEnum scrollMode) {
    this.scrollMode = scrollMode;
  }

  public static Section fromSectionDto(SectionDto sectionDto) {
    Section section = new Section();

    section.setId(sectionDto.getId());
    section.setTitle(sectionDto.getTitle());
    section.setImagepost(sectionDto.isImagePost());
    section.setPremoderated(sectionDto.isPremoderated());
    section.setVotePoll(sectionDto.isVotePoll());
    section.setScrollMode(SectionScrollModeEnum.valueOf(sectionDto.getScrollMode()));
    return section;
  }

  // **************************************************************

  public static final int SECTION_FORUM = 2;
  public static final int SECTION_GALLERY = 3;
  public static final int SECTION_NEWS = 1;
  public static final int SECTION_POLLS = 5;

  private static final Map<String, Integer> sections = new HashMap<String, Integer>();
  static {
    sections.put("news", SECTION_NEWS);
    sections.put("forum", SECTION_FORUM);
    sections.put("gallery", SECTION_GALLERY);
    sections.put("polls", SECTION_POLLS);
  }


  public String getAddText() {
    if (id==4) {
      return "Добавить ссылку";
    } else {
      return "Добавить сообщение";
    }
  }

  public boolean isForum() {
    return id==2;
  }

  public static int getCommentPostscore(int id) {
    //TODO move this to database
    if (id==1 || id==2) {
      return Topic.POSTSCORE_UNRESTRICTED;
    } else {
      return 50;
    }
  }
  public String getSectionLink() {
    return getSectionLinkInternal(id);
  }

  @Deprecated
  public static String getSectionLink(int section) {
    return getSectionLinkInternal(section);
  }

  private static String getSectionLinkInternal(int section) {
    switch (section) {
      case SECTION_FORUM:
        return "/forum/";
      case SECTION_GALLERY:
        return "/gallery/";
      case SECTION_NEWS:
        return "/news/";
      case SECTION_POLLS:
        return "/polls/";
      default:
        throw new RuntimeException("unknown section: " + section);
    }
  }

  public static String getNewsViewerLink(int section) throws SectionNotFoundException {
    switch (section) {
      case SECTION_FORUM:
        return "/forum/lenta/";
      case SECTION_GALLERY:
        return "/gallery/";
      case SECTION_NEWS:
        return "/news/";
      case SECTION_POLLS:
        return "/polls/";
      default:
        throw new SectionNotFoundException(section);
    }
  }

  public String getArchiveLink(int year, int month) {
    return getArchiveLink(id)+year+ '/' +month+ '/';
  }

  public String getArchiveLink() {
    return getArchiveLink(id);
  }

  public static String getArchiveLink(int id) {
    if (id==SECTION_FORUM) {
      return null;
    }

    return getSectionLink(id)+"archive/";
  }

  public static int getSection(String name) throws SectionNotFoundException {
    Integer v = sections.get(name);

    if (v==null) {
      throw new SectionNotFoundException();
    }

    return v;
  }

  public Timestamp getLastCommitdate(Connection db) throws SQLException {
    Statement st = null;
    ResultSet rs = null;

    try {
      st = db.createStatement();

      rs = st.executeQuery("select max(commitdate) from topics,groups where section=" + id + " and topics.groupid=groups.id");

      if (!rs.next()) {
        return null;
      } else {
        return rs.getTimestamp("max");
      }
    } finally {
      if (rs!=null) {
        rs.close();
      }

      if (st!=null) {
        st.close();
      }
    }
  }

  @Deprecated
  public String getName() {
    return title;
  }

}
