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
import ru.org.linux.dto.*;

import java.util.List;

public final class PreparedMessage {
  private final MessageDto message;
  private final UserDto author;
  private final DeleteInfoDto deleteInfo;
  private final UserDto deleteUser;
  private final String processedMessage;
  private final PreparedPoll poll;
  private final UserDto commiter;
  private final ImmutableList<String> tags;
  private final GroupDto groupDto;
  private final SectionDto sectionDto;

  private final EditInfoDTO lastEditInfo;
  private final UserDto lastEditor;
  private final int editCount;

  private final String userAgent;

  private static final int EDIT_PERIOD = 2 * 60 * 60 * 1000; // milliseconds

  public PreparedMessage(MessageDto messageDto, UserDto author, DeleteInfoDto deleteInfoDto, UserDto deleteUser, String processedMessage,
                         PreparedPoll poll, UserDto commiter, List<String> tags, GroupDto groupDto, SectionDto sectionDto,
                         EditInfoDTO lastEditInfo, UserDto lastEditor, int editorCount, String userAgent) {
    this.message = messageDto;
    this.author = author;
    this.deleteInfo = deleteInfoDto;
    this.deleteUser = deleteUser;
    this.processedMessage = processedMessage;
    this.poll = poll;
    this.commiter = commiter;
    if (tags != null) {
      this.tags = ImmutableList.copyOf(tags);
    } else {
      this.tags = ImmutableList.of();
    }
    this.groupDto = groupDto;
    this.sectionDto = sectionDto;
    this.lastEditInfo = lastEditInfo;
    this.lastEditor = lastEditor;
    editCount = editorCount;
    this.userAgent = userAgent;
  }

  public MessageDto getMessage() {
    return message;
  }

  public UserDto getAuthor() {
    return author;
  }

  public DeleteInfoDto getDeleteInfo() {
    return deleteInfo;
  }

  public UserDto getDeleteUser() {
    return deleteUser;
  }

  public String getProcessedMessage() {
    return processedMessage;
  }

  public PreparedPoll getPoll() {
    return poll;
  }

  public UserDto getCommiter() {
    return commiter;
  }

  public EditInfoDTO getLastEditInfo() {
    return lastEditInfo;
  }

  public UserDto getLastEditor() {
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

  public GroupDto getGroupDto() {
    return groupDto;
  }

  public boolean isEditable(UserDto by) {
    if (message.isDeleted()) {
      return false;
    }

    if (by.isAnonymous() || by.isBlocked()) {
      return false;
    }

    if (message.isExpired()) {
      return by.isModerator() && sectionDto.isPremoderated();
    }

    if (by.isModerator()) {
      if (author.isModerator()) {
        return true;
      }

      return sectionDto.isPremoderated();
    }

    if (!message.isLorcode()) {
      return false;
    }

    if (by.canCorrect() && sectionDto.isPremoderated()) {
      return true;
    }

    if (by.getId() == author.getId() && !message.isCommited()) {
      return message.isSticky() || sectionDto.isPremoderated() || (System.currentTimeMillis() - message.getPostdate().getTime()) < EDIT_PERIOD;
    }

    return false;
  }

  public SectionDto getSectionDto() {
    return sectionDto;
  }
}
