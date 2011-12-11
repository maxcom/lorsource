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

import com.google.common.collect.ImmutableList;
import ru.org.linux.group.Group;
import ru.org.linux.poll.PreparedPoll;
import ru.org.linux.section.Section;

import java.util.List;

public final class PreparedMessage {
  private final Message message;
  private final User author;
  private final DeleteInfo deleteInfo;
  private final User deleteUser;
  private final String processedMessage;
  private final PreparedPoll poll;
  private final User commiter;
  private final ImmutableList<String> tags;
  private final Group group;
  private final Section section;

  private final EditInfoDTO lastEditInfo;
  private final User lastEditor;
  private final int editCount;

  private final String userAgent;

  private static final int EDIT_PERIOD = 2 * 60 * 60 * 1000; // milliseconds

  public PreparedMessage(Message message, User author, DeleteInfo deleteInfo, User deleteUser, String processedMessage,
                          PreparedPoll poll, User commiter, List<String> tags, Group group, Section section,
                          EditInfoDTO lastEditInfo, User lastEditor, int editorCount, String userAgent) {
    this.message = message;
    this.author = author;
    this.deleteInfo = deleteInfo;
    this.deleteUser = deleteUser;
    this.processedMessage = processedMessage;
    this.poll = poll;
    this.commiter = commiter;
    if (tags!=null) {
      this.tags=ImmutableList.copyOf(tags);
    } else {
      this.tags=ImmutableList.of();
    }
    this.group = group;
    this.section = section;
    this.lastEditInfo = lastEditInfo;
    this.lastEditor = lastEditor;
    editCount = editorCount;
    this.userAgent = userAgent;
  }

  public Message getMessage() {
    return message;
  }

  public User getAuthor() {
    return author;
  }

  public DeleteInfo getDeleteInfo() {
    return deleteInfo;
  }

  public User getDeleteUser() {
    return deleteUser;
  }

  public String getProcessedMessage() {
    return processedMessage;
  }

  public PreparedPoll getPoll() {
    return poll;
  }

  public User getCommiter() {
    return commiter;
  }

  public EditInfoDTO getLastEditInfo() {
    return lastEditInfo;
  }

  public User getLastEditor() {
    return lastEditor;
  }

  public int getEditCount() {
    return editCount;
  }

  public int getId() {
    return message.getId();
  }

  public String getUserAgent() {
    return userAgent;
  }

  public ImmutableList<String> getTags() {
    return tags;
  }

  public Group getGroup() {
    return group;
  }

  public boolean isEditable(User by) {
    if (message.isDeleted()) {
      return false;
    }

    if (by.isAnonymous() || by.isBlocked()) {
      return false;
    }

    if (message.isExpired()) {
      return by.isModerator() && section.isPremoderated();
    }

    if (by.isModerator()) {
      if (author.isModerator()) {
        return true;
      }

      return section.isPremoderated();
    }

    if (!message.isLorcode()) {
      return false;
    }

    if (by.canCorrect() && section.isPremoderated()) {
      return true;
    }

    if (by.getId()==author.getId() && !message.isCommited()) {
      return message.isSticky() || section.isPremoderated() || (System.currentTimeMillis() - message.getPostdate().getTime()) < EDIT_PERIOD;
    }

    return false;
  }

  public Section getSection() {
    return section;
  }
}
