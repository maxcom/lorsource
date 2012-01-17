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

package ru.org.linux.topic;

import com.google.common.collect.ImmutableList;
import ru.org.linux.group.Group;
import ru.org.linux.poll.PreparedPoll;
import ru.org.linux.section.Section;
import ru.org.linux.site.DeleteInfo;
import ru.org.linux.user.User;

import java.util.List;

public final class PreparedTopic {
  private final Topic message;
  private final User author;
  private final DeleteInfo deleteInfo;
  private final User deleteUser;
  private final String processedMessage;
  private final PreparedPoll poll;
  private final User commiter;
  private final boolean lorcode;
  private final ImmutableList<String> tags;
  private final Group group;
  private final Section section;

  private final EditInfoDto lastEditInfo;
  private final User lastEditor;
  private final int editCount;

  private final String userAgent;

  public static final int EDIT_PERIOD = 2 * 60 * 60 * 1000; // milliseconds

  public PreparedTopic(Topic message, User author, DeleteInfo deleteInfo, User deleteUser, String processedMessage,
                       PreparedPoll poll, User commiter, List<String> tags, Group group, Section section,
                       EditInfoDto lastEditInfo, User lastEditor, int editorCount, String userAgent, boolean lorcode) {
    this.message = message;
    this.author = author;
    this.deleteInfo = deleteInfo;
    this.deleteUser = deleteUser;
    this.processedMessage = processedMessage;
    this.poll = poll;
    this.commiter = commiter;
    this.lorcode = lorcode;
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

  public Topic getMessage() {
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

  public EditInfoDto getLastEditInfo() {
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

  public Section getSection() {
    return section;
  }

  public boolean isLorcode() {
    return lorcode;
  }
}
