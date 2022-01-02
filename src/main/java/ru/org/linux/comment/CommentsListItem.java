/*
 * Copyright 1998-2021 Linux.org.ru
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
package ru.org.linux.comment;

import javax.annotation.Nullable;
import java.sql.Timestamp;

/**
 * DTO-класс, описывающий данные удалённого комментария
 */
public class CommentsListItem {
  private final String ptitle;
  private final String gtitle;
  private final int msgid;
  private final String title;

  @Nullable
  private final String reason;

  @Nullable
  private final Timestamp delDate;

  private final int bonus;
  private final int cid;
  private final boolean deleted;
  private final Timestamp postdate;

  public CommentsListItem(String ptitle, String gtitle, int msgid, String title, @Nullable String reason,
                          @Nullable Timestamp delDate, int bonus, int cid, boolean deleted, Timestamp postdate) {
    this.ptitle = ptitle;
    this.gtitle = gtitle;
    this.msgid = msgid;
    this.title = title;
    this.reason = reason;
    this.delDate = delDate;
    this.bonus = bonus;
    this.cid = cid;
    this.deleted = deleted;
    this.postdate = postdate;
  }

  public String getPtitle() {
    return ptitle;
  }

  public String getGtitle() {
    return gtitle;
  }

  public int getMsgid() {
    return msgid;
  }

  public String getTitle() {
    return title;
  }

  @Nullable
  public String getReason() {
    return reason;
  }

  @Nullable
  public Timestamp getDelDate() {
    return delDate;
  }

  public int getBonus() {
    return bonus;
  }

  public int getCommentId() {
    return cid;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public Timestamp getPostdate() {
    return postdate;
  }
}
