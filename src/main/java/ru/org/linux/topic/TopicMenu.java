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

package ru.org.linux.topic;

public class TopicMenu {
  private final boolean topicEditable;
  private final boolean tagsEditable;
  private final boolean resolvable;

  private final int memoriesId;
  private final int favsId;

  private final int memoriesCount;
  private final int favsCount;

  private final boolean commentsAllowed;
  private final boolean deletable;

  public TopicMenu(
          boolean topicEditable,
          boolean tagsEditable,
          boolean resolvable,
          int memoriesId,
          int favsId,
          int memoriesCount,
          int favsCount,
          boolean commentsAllowed,
          boolean deletable) {
    this.topicEditable = topicEditable;
    this.tagsEditable = tagsEditable;
    this.resolvable = resolvable;
    this.memoriesId = memoriesId;
    this.favsId = favsId;
    this.memoriesCount = memoriesCount;
    this.favsCount = favsCount;
    this.commentsAllowed = commentsAllowed;
    this.deletable = deletable;
  }

  public boolean isEditable() {
    return tagsEditable || topicEditable;
  }

  public boolean isTopicEditable() {
    return topicEditable;
  }

  public boolean isTagsEditable() {
    return tagsEditable;
  }

  public boolean isResolvable() {
    return resolvable;
  }

  public int getMemoriesId() {
    return memoriesId;
  }

  public boolean isCommentsAllowed() {
    return commentsAllowed;
  }

  public boolean isDeletable() {
    return deletable;
  }

  public int getFavsId() {
    return favsId;
  }

  public int getMemoriesCount() {
    return memoriesCount;
  }

  public int getFavsCount() {
    return favsCount;
  }
}