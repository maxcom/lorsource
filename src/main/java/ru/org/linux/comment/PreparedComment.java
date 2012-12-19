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

import ru.org.linux.site.ApiDeleteInfo;
import ru.org.linux.user.ApiUserRef;
import ru.org.linux.user.Remark;
import ru.org.linux.user.Userpic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;

public class PreparedComment {
  private final int id;

  private final ApiUserRef author;
  private final String processedMessage;

  @Nullable
  private final ReplyInfo reply;

  private final boolean deletable;
  private final boolean editable;
  private final Remark remark;
  private final boolean deleted;
  private final Date postdate;

  @Nullable
  private final Userpic userpic;

  @Nullable
  private final ApiDeleteInfo deleteInfo;

  @Nullable
  private final EditSummary editSummary;

  @Nonnull
  private final String title;

  @Nullable
  private final String postIP;

  @Nullable
  private final String userAgent;

  public PreparedComment(Comment comment,
                         ApiUserRef author,
                         String processedMessage,
                         @Nullable ReplyInfo reply,
                         boolean deletable,
                         boolean editable,
                         Remark remark,
                         @Nullable Userpic userpic,
                         @Nullable ApiDeleteInfo deleteInfo,
                         @Nullable EditSummary editSummary,
                         @Nullable String postIP,
                         @Nullable String userAgent) {
    this.deleteInfo = deleteInfo;
    this.editSummary = editSummary;
    this.postIP = postIP;
    this.userAgent = userAgent;
    this.id = comment.getId();
    this.author = author;
    this.processedMessage = processedMessage;
    this.reply = reply;
    this.deletable = deletable;
    this.editable = editable;
    this.remark = remark;
    this.userpic = userpic;

    title = comment.getTitle();
    deleted = comment.isDeleted();
    postdate = comment.getPostdate();
  }

  public ApiUserRef getAuthor() {
    return author;
  }

  public String getProcessedMessage() {
    return processedMessage;
  }

  @Nullable
  public ReplyInfo getReply() {
    return reply;
  }

  public boolean isDeletable() {
    return deletable;
  }

  public boolean isEditable() {
    return editable;
  }

  public Remark getRemark() {
    return remark;
  }

  @Nullable
  public Userpic getUserpic() {
    return userpic;
  }

  public int getId() {
    return id;
  }

  @Nullable
  public ApiDeleteInfo getDeleteInfo() {
    return deleteInfo;
  }

  @Nullable
  public EditSummary getEditSummary() {
    return editSummary;
  }

  @Nonnull
  public String getTitle() {
    return title;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public Date getPostdate() {
    return postdate;
  }

  @Nullable
  public String getPostIP() {
    return postIP;
  }

  @Nullable
  public String getUserAgent() {
    return userAgent;
  }
}
