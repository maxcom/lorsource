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

package ru.org.linux.topic;

import com.google.common.collect.ImmutableSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class TopicListDto {
  public enum DateLimitType {
    NONE,
    BETWEEN,
    MONTH_AGO
  }

  private TopicListDao.CommitMode commitMode = TopicListDao.CommitMode.COMMITED_AND_POSTMODERATED;

  private ImmutableSet<Integer> sections = ImmutableSet.of();
  private int userId = 0;
  private boolean userFavs = false;
  private boolean userWatches = false;
  private int group = 0;
  private int tag = 0;
  private Integer limit = null;
  private Integer offset = null;
  private DateLimitType dateLimitType = DateLimitType.NONE;
  private Date fromDate;
  private Date toDate;
  private boolean notalks = false;
  private boolean tech = false;

  private boolean showDraft = false;

  public ImmutableSet<Integer> getSections() {
    return sections;
  }

  public void setSection(Integer... sections) {
    this.sections = ImmutableSet.copyOf(sections);
  }

  public TopicListDao.CommitMode getCommitMode() {
    return commitMode;
  }

  public void setCommitMode(TopicListDao.CommitMode commitMode) {
    this.commitMode = commitMode;
  }

  public int getGroup() {
    return group;
  }

  public void setGroup(int group) {
    this.group = group;
  }

  public int getTag() {
    return tag;
  }

  public void setTag(int tag) {
    this.tag = tag;
  }

  public Integer getLimit() {
    return limit;
  }

  public void setLimit(Integer limit) {
    this.limit = limit;
  }

  public Integer getOffset() {
    return offset;
  }

  public void setOffset(Integer offset) {
    this.offset = offset;
  }

  public DateLimitType getDateLimitType() {
    return dateLimitType;
  }

  public void setDateLimitType(DateLimitType dateLimitType) {
    this.dateLimitType = dateLimitType;
  }

  public Date getFromDate() {
    return fromDate;
  }

  public void setFromDate(Date fromDate) {
    this.fromDate = fromDate;
  }

  public Date getToDate() {
    return toDate;
  }

  public void setToDate(Date toDate) {
    this.toDate = toDate;
  }

  public int getUserId() {
    return userId;
  }

  public void setUserId(int userId) {
    this.userId = userId;
  }

  public boolean isUserFavs() {
    return userFavs;
  }

  public void setUserFavs(boolean userFavs) {
    this.userFavs = userFavs;
  }

  public boolean isUserWatches() {
    return userWatches;
  }

  public void setUserWatches(boolean userWatches) {
    this.userWatches = userWatches;
  }

  public boolean isNotalks() {
    return notalks;
  }

  public void setNotalks(boolean notalks) {
    this.notalks = notalks;
  }

  public boolean isTech() {
    return tech;
  }

  public void setTech(boolean tech) {
    this.tech = tech;
  }

  public boolean isShowDraft() {
    return showDraft;
  }

  public void setShowDraft(boolean showDraft) {
    this.showDraft = showDraft;
  }

  public String toString() {
    return new StringBuilder()
      .append(TopicListDto.class.toString())
      .append('[')
      .append("commitMode=").append(commitMode)
      .append("; userId=").append(userId)
      .append("; sections=").append(sections.toString())
      .append("; userFavs=").append(userFavs)
      .append("; group=").append(group)
      .append("; tag=").append(tag)
      .append("; limit=").append(limit)
      .append("; offset=").append(offset)
      .append("; dateLimitType=").append(dateLimitType)
      .append("; fromDate=").append((fromDate != null) ? fromDate.toString() : "")
      .append("; toDate=").append((toDate != null) ? toDate.toString() : "")
      .append("; notalks=").append(notalks)
      .append("; tech=").append(tech)
      .append(']')
      .toString();
  }

  public static class DeletedTopic {
    private final String nick;
    private final int id;
    private final int groupId;
    private final String ptitle;
    private final String gtitle;
    private final String title;
    private final String reason;

    public DeletedTopic(ResultSet rs) throws SQLException {
      nick = rs.getString("nick");
      id = rs.getInt("msgid");
      groupId = rs.getInt("guid");
      ptitle = rs.getString("ptitle");
      gtitle = rs.getString("gtitle");
      title = rs.getString("subj");
      reason = rs.getString("reason");
    }

    public String getNick() {
      return nick;
    }

    public int getId() {
      return id;
    }

    public int getGroupId() {
      return groupId;
    }

    public String getPtitle() {
      return ptitle;
    }

    public String getGtitle() {
      return gtitle;
    }

    public String getTitle() {
      return title;
    }

    public String getReason() {
      return reason;
    }
  }
}
