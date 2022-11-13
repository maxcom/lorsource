/*
 * Copyright 1998-2022 Linux.org.ru
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

import com.google.common.base.Strings;
import org.apache.commons.text.StringEscapeUtils;
import ru.org.linux.site.ApiDeleteInfo;
import ru.org.linux.user.User;
import ru.org.linux.user.Userpic;

import javax.annotation.Nullable;
import java.util.Date;

public class PreparedComment {
  private final int id;

  private final User author;
  private final String processedMessage;

  @Nullable
  private final ReplyInfo reply;

  private final boolean deletable;
  private final boolean undeletable;
  private final boolean editable;
  private final String remark;
  private final boolean deleted;
  private final Date postdate;

  @Nullable
  private final Userpic userpic;

  @Nullable
  private final ApiDeleteInfo deleteInfo;

  @Nullable
  private final EditSummary editSummary;

  @Nullable
  private final String title;

  @Nullable
  private final String postIP;

  @Nullable
  private final String userAgent;

  private final int userAgentId;

  private final int answerCount;
  private final String answerLink;
  private final boolean answerSamepage;
  private final boolean authorReadonly;

  public PreparedComment(Comment comment,
                         User author,
                         String processedMessage,
                         @Nullable ReplyInfo reply,
                         boolean deletable,
                         boolean editable,
                         String remark,
                         @Nullable Userpic userpic,
                         @Nullable ApiDeleteInfo deleteInfo,
                         @Nullable EditSummary editSummary,
                         @Nullable String postIP,
                         @Nullable String userAgent,
                         int userAgentId,
                         boolean undeletable,
                         int answerCount,
                         @Nullable String answerLink,
                         boolean answerSamepage,
                         boolean authorReadonly) {
    this.deleteInfo = deleteInfo;
    this.editSummary = editSummary;
    this.postIP = postIP;
    this.userAgent = userAgent;
    this.userAgentId = userAgentId;
    this.id = comment.getId();
    this.author = author;
    this.processedMessage = processedMessage;
    this.reply = reply;
    this.deletable = deletable;
    this.undeletable = undeletable;
    this.editable = editable;
    this.remark = remark;
    this.userpic = userpic;
    this.answerCount = answerCount;
    this.answerLink = answerLink;
    this.answerSamepage = answerSamepage;
    this.authorReadonly = authorReadonly;

    String encodedTitle = Strings.emptyToNull(comment.getTitle().trim());

    if (encodedTitle!=null) {
      title = StringEscapeUtils.unescapeHtml4(encodedTitle);
    } else {
      title = null;
    }

    deleted = comment.isDeleted();
    postdate = comment.getPostdate();
  }

  public User getAuthor() {
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

  public boolean isUndeletable() {
    return undeletable;
  }

  public boolean isEditable() {
    return editable;
  }

  public String getRemark() {
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

  @Nullable
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

  public int getAnswerCount() {
    return answerCount;
  }

  public String getAnswerLink() {
    return answerLink;
  }

  public boolean isAnswerSamepage() {
    return answerSamepage;
  }

  public int getUserAgentId() {
    return userAgentId;
  }

  public boolean isAuthorReadonly() {
    return authorReadonly;
  }
}
