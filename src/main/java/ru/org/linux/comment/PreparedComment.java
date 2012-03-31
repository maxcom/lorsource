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

import ru.org.linux.marks.MessageMark;
import ru.org.linux.user.User;

import java.util.List;
import java.util.Set;
import java.util.SortedMap;

public class PreparedComment {
  private final Comment comment;
  private final User author;
  private final String processedMessage;
  private final User replyAuthor;
  
  private final SortedMap<MessageMark, Integer> marks;
  private final Set<MessageMark> currentUserMarks;

  public PreparedComment(Comment comment, User author, String processedMessage, User replyAuthor, SortedMap<MessageMark, Integer> marks, Set<MessageMark> currentUserMarks) {
    this.comment = comment;
    this.author = author;
    this.processedMessage = processedMessage;
    this.replyAuthor = replyAuthor;
    this.marks = marks;
    this.currentUserMarks = currentUserMarks;
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

  public SortedMap<MessageMark, Integer> getMarks() {
    return marks;
  }

  public Set<MessageMark> getCurrentUserMarks() {
    return currentUserMarks;
  }
}
