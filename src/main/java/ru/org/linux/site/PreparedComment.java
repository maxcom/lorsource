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

package ru.org.linux.site;

import ru.org.linux.dto.UserDto;

public class PreparedComment {
  private final Comment comment;
  private final UserDto author;
  private final String processedMessage;
  private final UserDto replyAuthor;

  public PreparedComment(Comment comment, UserDto author, String processedMessage, UserDto replyAuthor) {
    this.comment = comment;
    this.author = author;
    this.processedMessage = processedMessage;
    this.replyAuthor = replyAuthor;
  }

  public Comment getComment() {
    return comment;
  }

  public UserDto getAuthor() {
    return author;
  }

  public String getProcessedMessage() {
    return processedMessage;
  }

  public UserDto getReplyAuthor() {
    return replyAuthor;
  }
}
