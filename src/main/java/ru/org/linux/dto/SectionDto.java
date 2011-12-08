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

package ru.org.linux.dto;

import ru.org.linux.exception.SectionNotFoundException;

import java.io.Serializable;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class SectionDto implements Serializable {
  private static final long serialVersionUID = -2259350244006777910L;

  private final String name;
  private final boolean imagepost;
  private final boolean moderate;
  private final int id;
  private final boolean votepoll;

  public static final int SCROLL_NOSCROLL = 0;
  public static final int SCROLL_SECTION = 1;
  public static final int SCROLL_GROUP = 2;

  public static final int SECTION_NEWS = 1;
  public static final int SECTION_FORUM = 2;
  public static final int SECTION_GALLERY = 3;
  public static final int SECTION_POLLS = 5;

  private static final Map<String, Integer> sections = new HashMap<String, Integer>();

  static {
    sections.put("news", SECTION_NEWS);
    sections.put("forum", SECTION_FORUM);
    sections.put("gallery", SECTION_GALLERY);
    sections.put("polls", SECTION_POLLS);
  }

  public SectionDto(ResultSet rs) throws SQLException {
    name = rs.getString("name");
    imagepost = rs.getBoolean("imagepost");
    votepoll = rs.getBoolean("vote");
    moderate = rs.getBoolean("moderate");
    id = rs.getInt("id");
  }

  public String getName() {
    return name;
  }

  public boolean isImagepost() {
    return imagepost;
  }

  public boolean isVotePoll() {
    return votepoll;
  }

  public static int getScrollMode(int sectionid) {
    switch (sectionid) {
      case SECTION_NEWS: /* news*/
      case SECTION_GALLERY: /* screenshots */
      case SECTION_POLLS: /* poll */
        return SCROLL_SECTION;
      case SECTION_FORUM: /* forum */
        return SCROLL_GROUP;
      default:
        return SCROLL_NOSCROLL;
    }
  }

  public int getId() {
    return id;
  }

  public boolean isPremoderated() {
    return moderate;
  }

  public String getAddText() {
    if (id == 4) {
      return "Добавить ссылку";
    } else {
      return "Добавить сообщение";
    }
  }

  public boolean isForum() {
    return id == SECTION_FORUM;
  }

  public String getTitle() {
    return name;
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
      if (rs != null) {
        rs.close();
      }

      if (st != null) {
        st.close();
      }
    }
  }

  public static int getCommentPostscore(int id) {
    //TODO move this to database
    if (id == SECTION_NEWS || id == SECTION_FORUM) {
      return MessageDto.POSTSCORE_UNRESTRICTED;
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
    return getArchiveLink(id) + year + '/' + month + '/';
  }

  public String getArchiveLink() {
    return getArchiveLink(id);
  }

  public static String getArchiveLink(int id) {
    if (id == SECTION_FORUM) {
      return null;
    }

    return getSectionLink(id) + "archive/";
  }

  public static int getSection(String name) throws SectionNotFoundException {
    Integer v = sections.get(name);

    if (v == null) {
      throw new SectionNotFoundException();
    }

    return v;
  }
}
