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

/**
 * Доменный класс для работы с секциями.
 */
public class Section implements Serializable {
  private static final long serialVersionUID = -2259350244006777910L;

  private int id;
  private String title;
  private boolean imagepost;
  private boolean premoderated;
  private boolean votePoll;
  private String name;
  private String link;
  private String feedLink;
  private int minCommentScore;

  private SectionScrollModeEnum scrollMode;

  public Section() {
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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public String getFeedLink() {
    return feedLink;
  }

  public void setFeedLink(String feedLink) {
    this.feedLink = feedLink;
  }

  public int getMinCommentScore() {
    return minCommentScore;
  }

  public void setMinCommentScore(int minCommentScore) {
    this.minCommentScore = minCommentScore;
  }

  public static Section fromSectionDto(SectionDto sectionDto) {
    Section section = new Section();

    section.setId(sectionDto.getId());
    section.setTitle(sectionDto.getTitle());
    section.setImagepost(sectionDto.isImagePost());
    section.setPremoderated(sectionDto.isPremoderated());
    section.setVotePoll(sectionDto.isVotePoll());
    section.setScrollMode(SectionScrollModeEnum.valueOf(sectionDto.getScrollMode()));
    section.setName(sectionDto.getName());
    section.setLink(sectionDto.getLink());
    section.setFeedLink(sectionDto.getFeedLink());
    section.setMinCommentScore(sectionDto.getMinCommentScore());
    return section;
  }

  public boolean isForum() {
    return "forum".equals(name);
  }

  public String getArchiveLink() {
    return isForum()
      ? null
      : getLink() + "archive/";
  }

  public String getArchiveLink(int year, int month) {
    String archiveLink = getArchiveLink();
    if (archiveLink == null) {
      //todo: действия при отсутствующем линке
    }
    return archiveLink + year + '/' + month + '/';
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


  @Deprecated
  public String getAddText() {
    // используется в section.jsp:40
    // TODO: это должно быть в контроллере
    if (id == 4) {
      return "Добавить ссылку";
    } else {
      return "Добавить сообщение";
    }
  }

  @Deprecated
  public static int getCommentPostscore(int id) {
    //TODO move this to database
    if (id == 1 || id == 2) {
      return Topic.POSTSCORE_UNRESTRICTED;
    } else {
      return 50;
    }
  }

  @Deprecated
  public static String getSectionLink(int section) {
    return getSectionLinkInternal(section);
  }

  @Deprecated
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

  @Deprecated
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


  @Deprecated
  public static String getArchiveLink(int id) {
    if (id == SECTION_FORUM) {
      return null;
    }

    return getSectionLink(id) + "archive/";
  }

  @Deprecated
  public static int getSection(String name) throws SectionNotFoundException {
    Integer v = sections.get(name);

    if (v == null) {
      throw new SectionNotFoundException();
    }

    return v;
  }

  @Deprecated
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
}
