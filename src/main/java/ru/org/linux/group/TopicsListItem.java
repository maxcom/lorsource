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

import java.sql.Timestamp;
import java.util.Optional;

public class TopicsListItem {
  private final int topicAuthor;
  private final int topicId;
  private final int commentCount;
  private final int groupId;
  private final String groupTitle;
  private final String title;
  private final Optional<Integer> lastCommentId;
  private final Optional<Integer> lastCommentBy;
  private final boolean resolved;
  private final int section;
  private final String groupUrlName;
  private final Timestamp postdate;
  private final boolean uncommited;
  private final boolean deleted;
  private final boolean sticky;
  private final int topicPostscore;

  public TopicsListItem(int topicAuthor, int topicId, int commentCount, int groupId, String groupTitle, String title,
                        Optional<Integer> lastCommentId, Optional<Integer> lastCommentBy, boolean resolved,
                        int section, String groupUrlName, Timestamp postdate, boolean uncommited, boolean deleted,
                        boolean sticky, int topicPostscore) {
    this.topicAuthor = topicAuthor;
    this.topicId = topicId;
    this.commentCount = commentCount;
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
    this.deleted = deleted;
    this.sticky = sticky;
    this.topicPostscore = topicPostscore;
  }

  public int getTopicId() {
    return topicId;
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

  public int getTopicAuthor() {
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

  public Optional<Integer> getLastCommentId() {
    return lastCommentId;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public boolean isSticky() {
    return sticky;
  }

  public int getCommentCount() {
    return commentCount;
  }

  public Optional<Integer> getLastCommentBy() {
    return lastCommentBy;
  }

  public String getGroupUrlName() {
    return groupUrlName;
  }

  public int getTopicPostscore() {
    return topicPostscore;
  }
}
