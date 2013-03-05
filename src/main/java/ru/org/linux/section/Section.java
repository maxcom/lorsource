/*
 * Copyright 1998-2012 Linux.org.ru
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

import com.google.common.collect.ImmutableBiMap;
import ru.org.linux.topic.TopicPermissionService;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Section implements Serializable {
  private static final long serialVersionUID = -2259350244006777911L;

  private final String name;
  private final boolean imagepost;
  private final boolean moderate;
  private final int id;
  private final boolean votepoll;
  private final int topicsRestriction;
  private final boolean imageAllowed;
  
  private SectionScrollModeEnum scrollMode;

  public static final int SECTION_FORUM = 2;
  public static final int SECTION_GALLERY = 3;
  public static final int SECTION_NEWS = 1;
  public static final int SECTION_POLLS = 5;

  private static final ImmutableBiMap<String, Integer> sections
          = ImmutableBiMap.of(
          "news", SECTION_NEWS,
          "forum", SECTION_FORUM,
          "gallery", SECTION_GALLERY,
          "polls", SECTION_POLLS
  );

  public Section(ResultSet rs) throws SQLException {
    name = rs.getString("name");
    imagepost = rs.getBoolean("imagepost");
    votepoll = rs.getBoolean("vote");
    moderate = rs.getBoolean("moderate");
    id = rs.getInt("id");
    imageAllowed = rs.getBoolean("imageallowed");

    int restrictTopicsValue = rs.getInt("restrict_topics");
    if (!rs.wasNull()) {
      topicsRestriction = restrictTopicsValue;
    } else {
      topicsRestriction = TopicPermissionService.POSTSCORE_UNRESTRICTED;
    }

    scrollMode = SectionScrollModeEnum.valueOf(rs.getString("scroll_mode"));
  }

  public Section(String name, boolean imagepost, boolean moderate, int id, boolean votepoll, String scrollModeStr, int topicsRestriction) {
    this.name = name;
    this.imagepost = imagepost;
    this.moderate = moderate;
    this.id = id;
    this.votepoll = votepoll;
    scrollMode = SectionScrollModeEnum.valueOf(scrollModeStr);
    this.topicsRestriction = topicsRestriction;
    imageAllowed = false;
  }

  public String getName() {
    return name;
  }

  public boolean isImagepost() {
    return imagepost;
  }

  public boolean isPollPostAllowed() {
    return votepoll;
  }

  public SectionScrollModeEnum getScrollMode() {
    return scrollMode;
  }

  public int getId() {
    return id;
  }

  public boolean isPremoderated() {
    return moderate;
  }

  public String getTitle() {
    return name;
  }

  public static int getCommentPostscore(int id) {
    //TODO move this to database
    if (id==SECTION_NEWS || id==SECTION_FORUM) {
      return TopicPermissionService.POSTSCORE_UNRESTRICTED;
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
    return '/' + getUrlName(section) + '/';
  }

  public static String getNewsViewerLink(int section) throws SectionNotFoundException {
    String urlName = getUrlName(section);

    switch (section) {
      case SECTION_FORUM:
        return "/forum/lenta/";
      default:
        return '/' +urlName+ '/';
    }
  }

  private static String getUrlName(int section) {
    String name = sections.inverse().get(section);

    if (name!=null) {
      return name;
    } else {
      throw new SectionNotFoundException(section);
    }
  }

  public String getUrlName() {
    return getUrlName(id);
  }

  public String getArchiveLink(int year, int month) {
    return getArchiveLink()+year+ '/' +month+ '/';
  }

  public String getArchiveLink() {
    if (id==SECTION_FORUM) {
      return null;
    }
    
    return getSectionLink(id)+"archive/";
  }

  public int getTopicsRestriction() {
    return topicsRestriction;
  }

  public boolean isImageAllowed() {
    return imageAllowed;
  }
}
