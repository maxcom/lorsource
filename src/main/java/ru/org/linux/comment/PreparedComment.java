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

import ru.org.linux.user.ApiUserRef;
import ru.org.linux.user.Remark;
import ru.org.linux.user.Userpic;

import javax.annotation.Nullable;

public class PreparedComment {
  private final Comment comment;
  private final ApiUserRef author;
  private final String processedMessage;

  @Nullable
  private final ReplyInfo reply;

  private final boolean deletable;
  private final boolean editable;
  private final Remark remark;

  @Nullable
  private final Userpic userpic;

  public PreparedComment(Comment comment,
                         ApiUserRef author,
                         String processedMessage,
                         @Nullable ReplyInfo reply,
                         boolean deletable,
                         boolean editable,
                         Remark remark,
                         @Nullable Userpic userpic
  ) {
    this.comment = comment;
    this.author = author;
    this.processedMessage = processedMessage;
    this.reply = reply;
    this.deletable = deletable;
    this.editable = editable;
    this.remark = remark;
    this.userpic = userpic;
  }

  public Comment getComment() {
    return comment;
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
}
