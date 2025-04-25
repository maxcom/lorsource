/*
 * Copyright 1998-2025 Linux.org.ru
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
import ru.org.linux.user.User;

import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.util.List;

public class PreparedTopicsListItem {
  private final User topicAuthor; // topic author
  private final int topicId; // topic id
  private final int stat1; // comment count
  private final int groupId;
  private final String groupTitle;
  private final String title;
  private final int lastCommentId; // or 0

  @Nullable
  private final User lastCommentBy;

  private final boolean resolved;
  private final int section;
  private final String groupUrlName;
  private final Timestamp postdate; // date of last comment or topic postdate if none
  private final boolean uncommited; // awaits for approve
  private final int pages; // number of pages
  private final List<String> tags;
  private final boolean deleted;
  private final boolean sticky;
  private final int topicPostscore;

  public PreparedTopicsListItem(User topicAuthor, int topicId, int stat1, int groupId, String groupTitle, String title,
                                int lastCommentId, User lastCommentBy, boolean resolved, int section, String groupUrlName,
                                Timestamp postdate, boolean uncommited, int pages, List<String> tags, boolean deleted,
                                boolean sticky, int topicPostscore) {
    this.topicAuthor = topicAuthor;
    this.topicId = topicId;
    this.stat1 = stat1;
    this.groupId = groupId;
    this.groupTitle = groupTitle;
    this.title = title;
    this.lastCommentId = lastCommentId;
    this.lastCommentBy = lastCommentBy;
    this.resolved = resolved;
    this.section = section;
    this.groupUrlName = groupUrlName;
    this.postdate = postdate;
    this.uncommited = uncommited;
    this.pages = pages;
    this.tags = tags;
    this.deleted = deleted;
    this.sticky = sticky;
    this.topicPostscore = topicPostscore;
  }

  public String getLastPageUrl() {
    if (pages > 1) {
      return getGroupUrl() + topicId + "/page" + (pages - 1) + "?lastmod=" + lastCommentId;
    } else {
      return getGroupUrl() + topicId + "?lastmod=" + lastCommentId;
    }
  }

  public String getFirstPageUrl() {
    if (pages<=1) {
      return getGroupUrl() + topicId + "?lastmod=" + lastCommentId;
    } else {
      return getCanonicalUrl();
    }
  }

  public String getCanonicalUrl() {
    return getGroupUrl() + topicId;
  }

  public String getGroupUrl() {
    return Section.getSectionLink(section) + groupUrlName + '/';
  }

  public int getTopicId() {
    return topicId;
  }

  public int getCommentCount() {
    if (topicPostscore != TopicPermissionService.POSTSCORE_HIDE_COMMENTS()) {
      return stat1;
    } else {
      return 0;
    }
  }

  public int getGroupId() {
    return groupId;
  }

  public String getGroupTitle() {
    return groupTitle;
  }

  public String getTitle() {
    return title;
  }

  public int getPages() {
    return pages;
  }

  public User getAuthor() {
    if (lastCommentBy!=null) {
      return lastCommentBy;
    } else {
      return topicAuthor;
    }
  }

  public User getTopicAuthor() {
    return topicAuthor;
  }

  public boolean isResolved() {
    return resolved;
  }

  public int getSection() {
    return section;
  }

  public Timestamp getPostdate() {
    return postdate;
  }

  public boolean isUncommited() {
    return uncommited;
  }

  public List<String> getTags() {
    return tags;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public boolean isSticky() {
    return sticky;
  }

  public boolean isCommentsClosed() {
    return topicPostscore >= TopicPermissionService.POSTSCORE_MODERATORS_ONLY();
  }
}
