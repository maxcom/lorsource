/*
 * Copyright 1998-2013 Linux.org.ru
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

package ru.org.linux.user;

import java.sql.Timestamp;
import java.util.List;

public class UserStatistics {
  private final int ignoreCount;
  private final int commentCount;
  private final boolean exactCommentCount;

  private final Timestamp firstComment;
  private final Timestamp lastComment;
  private final Timestamp firstTopic;
  private final Timestamp lastTopic;

  private final List<UsersSectionStatEntry> topicsBySection;

  public UserStatistics(int ignoreCount, int commentCount,
                        boolean exactCommentCount, Timestamp firstComment, Timestamp lastComment,
                        Timestamp firstTopic, Timestamp lastTopic,
                        List<UsersSectionStatEntry> topicsBySection) {
    this.ignoreCount = ignoreCount;
    this.commentCount = commentCount;
    this.exactCommentCount = exactCommentCount;
    this.firstComment = firstComment;
    this.lastComment = lastComment;
    this.firstTopic = firstTopic;
    this.lastTopic = lastTopic;
    this.topicsBySection = topicsBySection;
  }

  public int getIgnoreCount() {
    return ignoreCount;
  }

  public int getCommentCount() {
    return commentCount;
  }

  public Timestamp getFirstComment() {
    return firstComment;
  }

  public Timestamp getLastComment() {
    return lastComment;
  }

  public Timestamp getFirstTopic() {
    return firstTopic;
  }

  public Timestamp getLastTopic() {
    return lastTopic;
  }

  public List<UsersSectionStatEntry> getTopicsBySection() {
    return topicsBySection;
  }

  public boolean isExactCommentCount() {
    return exactCommentCount;
  }
}
