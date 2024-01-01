/*
 * Copyright 1998-2024 Linux.org.ru
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

import java.util.Date;

public class TopicListDto {
  public enum CommitMode {
    COMMITED_ONLY(" AND sections.moderate AND commitdate is not null "),
    UNCOMMITED_ONLY(" AND (NOT topics.moderate) AND sections.moderate "),
    POSTMODERATED_ONLY(" AND NOT sections.moderate"),
    COMMITED_AND_POSTMODERATED(" AND (topics.moderate OR NOT sections.moderate) "),
    ALL(" ");

    private final String queryPiece;

    CommitMode(String queryPiece) {
      this.queryPiece = queryPiece;
    }

    public String getQueryPiece() {
      return queryPiece;
    }
  }
  public enum DateLimitType {
    NONE,
    BETWEEN,
    FROM_DATE
  }

  public enum MiniNewsMode {
    ALL,
    MAJOR,
    MINOR
  }

  private CommitMode commitMode = CommitMode.COMMITED_AND_POSTMODERATED;

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
  private boolean includeAnonymous = true;

  private MiniNewsMode miniNewsMode = MiniNewsMode.ALL;

  public ImmutableSet<Integer> getSections() {
    return sections;
  }

  public void setSection(Integer... sections) {
    this.sections = ImmutableSet.copyOf(sections);
  }

  public CommitMode getCommitMode() {
    return commitMode;
  }

  public void setCommitMode(CommitMode commitMode) {
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

  public MiniNewsMode getMiniNewsMode() {
    return miniNewsMode;
  }

  public void setMiniNewsMode(MiniNewsMode miniNewsMode) {
    this.miniNewsMode = miniNewsMode;
  }

  public boolean isIncludeAnonymous() {
    return includeAnonymous;
  }

  public void setIncludeAnonymous(boolean includeAnonymous) {
    this.includeAnonymous = includeAnonymous;
  }
}
