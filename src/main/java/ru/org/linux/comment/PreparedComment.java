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

import ru.org.linux.user.User;
import ru.org.linux.user.Remark;

public class PreparedComment {
  private final Comment comment;
  private final User author;
  private final String processedMessage;
  private final User replyAuthor;
  private final boolean haveAnswers;
  private final Comment reply;
  private final int replyPage;
  private final boolean deletable;
  private final boolean editable;
  private final Remark remark;
  private final boolean samePage;

  public PreparedComment(Comment comment, User author, String processedMessage, User replyAuthor, boolean haveAnswers,
                         Comment reply, int replyPage,
                         boolean deletable, boolean editable, Remark remark, boolean samePage) {
    this.comment = comment;
    this.author = author;
    this.processedMessage = processedMessage;
    this.replyAuthor = replyAuthor;
    this.haveAnswers = haveAnswers;
    this.reply = reply;
    this.replyPage = replyPage;
    this.deletable = deletable;
    this.editable = editable;
    this.remark = remark;
    this.samePage = samePage;
  }

  public Comment getComment() {
    return comment;
  }

  public User getAuthor() {
    return author;
  }

  public String getProcessedMessage() {
    return processedMessage;
  }

  public User getReplyAuthor() {
    return replyAuthor;
  }

  public boolean isHaveAnswers() {
    return haveAnswers;
  }

  public Comment getReply() {
    return reply;
  }

  public int getReplyPage() {
    return replyPage;
  }

  public String getReplyTitle() {
    if(reply != null) {
      String replyTitle = reply.getTitle();
      if (replyTitle.trim().isEmpty()) {
        return  "комментарий";
      }
      return replyTitle;
    } else {
      return "";
    }
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

  public boolean isSamePage() {
    return samePage;
  }
}
