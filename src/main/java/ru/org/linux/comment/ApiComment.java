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

package ru.org.linux.comment;

import java.util.Date;

public class ApiComment {
  private final int id;

  private boolean deleted = false;
  private String deleterNick = null;
  private String deleteReason = null;

  private Reply reply;

  public ApiComment(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public String getDeleterNick() {
    return deleterNick;
  }

  public void setDeleterNick(String deleterNick) {
    this.deleterNick = deleterNick;
  }

  public String getDeleteReason() {
    return deleteReason;
  }

  public void setDeleteReason(String deleteReason) {
    this.deleteReason = deleteReason;
  }

  public Reply getReply() {
    return reply;
  }

  public void setReply(Reply reply) {
    this.reply = reply;
  }

  public static class Reply {
    private final int id;
    private final boolean samePage;
    private final ApiUserRef author;
    private final Date postdate;

    public Reply(int id, boolean samePage, ApiUserRef author, Date postdate) {
      this.id = id;
      this.samePage = samePage;
      this.author = author;
      this.postdate = new Date(postdate.getTime());
    }

    public int getId() {
      return id;
    }

    public boolean isSamePage() {
      return samePage;
    }

    public ApiUserRef getAuthor() {
      return author;
    }

    public Date getPostdate() {
      return postdate;
    }
  }
}
