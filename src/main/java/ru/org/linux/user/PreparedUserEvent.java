/*
 * Copyright 1998-2014 Linux.org.ru
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

import ru.org.linux.group.Group;

public class PreparedUserEvent {
  private final UserEvent event;
  private final String messageText;
  private final User commentAuthor;
  private final int bonus;
  private final Group group;

  public PreparedUserEvent(UserEvent event, String messageText, User commentAuthor, int bonus, Group group) {
    this.event = event;
    this.messageText = messageText;
    this.commentAuthor = commentAuthor;
    this.bonus = bonus;
    this.group = group;
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

  public int getBonus() {
    return bonus;
  }

  public Group getGroup() {
    return group;
  }
}
