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

package ru.org.linux.user;

import com.google.common.collect.ImmutableMap;

import java.sql.*;
import java.util.Map;

public class UserStatistics {
  private final int ignoreCount;
  private final int commentCount;

  private final Timestamp firstComment;
  private final Timestamp lastComment;
  private final Timestamp firstTopic;
  private final Timestamp lastTopic;

  private final Map<String, Integer> commentsBySection;

  public UserStatistics(int ignoreCount, int commentCount,
                        Timestamp firstComment, Timestamp lastComment,
                        Timestamp firstTopic, Timestamp lastTopic,
                        Map<String, Integer> commentsBySection) {
    this.ignoreCount = ignoreCount;
    this.commentCount = commentCount;
    this.firstComment = firstComment;
    this.lastComment = lastComment;
    this.firstTopic = firstTopic;
    this.lastTopic = lastTopic;
    this.commentsBySection = commentsBySection;
  }

  @Deprecated
  public UserStatistics(Connection db, int id) throws SQLException {
    PreparedStatement ignoreStat = db.prepareStatement("SELECT count(*) as inum FROM ignore_list JOIN users ON  ignore_list.userid = users.id WHERE ignored=? AND not blocked");
    PreparedStatement commentStat = db.prepareStatement("SELECT count(*) as c FROM comments WHERE userid=? AND not deleted");
    PreparedStatement topicDates = db.prepareStatement("SELECT min(postdate) as first,max(postdate) as last FROM topics WHERE topics.userid=?");
    PreparedStatement commentDates = db.prepareStatement("SELECT min(postdate) as first,max(postdate) as last FROM comments WHERE comments.userid=?");

    PreparedStatement commentsBySectionStat = db.prepareStatement(
            "SELECT sections.name as pname, count(*) as c " +
                    "FROM topics, groups, sections " +
                    "WHERE topics.userid=? " +
                    "AND groups.id=topics.groupid " +
                    "AND sections.id=groups.section " +
                    "AND not deleted " +
                    "GROUP BY sections.name"
    );

    ignoreStat.setInt(1, id);
    commentStat.setInt(1, id);
    topicDates.setInt(1, id);
    commentDates.setInt(1, id);
    commentsBySectionStat.setInt(1, id);

    ResultSet ignoreResult = ignoreStat.executeQuery();
    if (ignoreResult.next()) {
      ignoreCount = ignoreResult.getInt(1);
    } else {
      ignoreCount = 0;
    }
    ignoreResult.close();
    ignoreStat.close();

    ResultSet commentResult = commentStat.executeQuery();
    if (commentResult.next()) {
      commentCount = commentResult.getInt(1);
    } else {
      commentCount = 0;
    }
    commentResult.close();
    commentStat.close();

    ResultSet topicDatesResult = topicDates.executeQuery();
    if (topicDatesResult.next()) {
      firstTopic = topicDatesResult.getTimestamp("first");
      lastTopic = topicDatesResult.getTimestamp("last");
    } else {
      firstTopic = null;
      lastTopic = null;
    }
    topicDatesResult.close();
    topicDates.close();

    ResultSet commentDatesResult = commentDates.executeQuery();
    if (commentDatesResult.next()) {
      firstComment = commentDatesResult.getTimestamp("first");
      lastComment = commentDatesResult.getTimestamp("last");
    } else {
      firstComment = null;
      lastComment = null;
    }
    commentDatesResult.close();
    commentDates.close();

    ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
    ResultSet comments = commentsBySectionStat.executeQuery();
    while (comments.next()) {
      builder.put(comments.getString("pname"), comments.getInt("c"));
    }

    commentsBySection = builder.build();
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

  public Map<String, Integer> getCommentsBySection() {
    return commentsBySection;
  }
}
