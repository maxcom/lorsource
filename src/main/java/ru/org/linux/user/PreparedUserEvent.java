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

package ru.org.linux.user;

public class PreparedUserEvent {
  private final UserEvent event;
  private final String messageText;
  private final User commentAuthor;

  public PreparedUserEvent(UserEvent event, String messageText, User commentAuthor) {
    this.event = event;
    this.messageText = messageText;
    this.commentAuthor = commentAuthor;
  }

  public UserEvent getEvent() {
    return event;
  }

  public String getMessageText() {
    return messageText;
  }

  public User getCommentAuthor() {
    return commentAuthor;
  }
}
