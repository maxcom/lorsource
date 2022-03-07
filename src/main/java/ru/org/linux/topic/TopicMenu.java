/*
 * Copyright 1998-2022 Linux.org.ru
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

import ru.org.linux.user.Userpic;

import javax.annotation.Nullable;

public class TopicMenu {
  private final boolean topicEditable;
  private final boolean tagsEditable;
  private final boolean resolvable;

  private final boolean commentsAllowed;
  private final boolean showComments;
  private final boolean deletable;

  private final boolean undeletable;
  private final boolean commitable;
  @Nullable
  private final Userpic userpic;

  public TopicMenu(
          boolean topicEditable,
          boolean tagsEditable,
          boolean resolvable,
          boolean commentsAllowed,
          boolean deletable,
          boolean undeletable,
          boolean commitable,
          @Nullable Userpic userpic,
          boolean showComments) {
    this.topicEditable = topicEditable;
    this.tagsEditable = tagsEditable;
    this.resolvable = resolvable;
    this.commentsAllowed = commentsAllowed;
    this.deletable = deletable;
    this.undeletable = undeletable;
    this.commitable = commitable;
    this.userpic = userpic;
    this.showComments = showComments;
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

  public boolean isCommentsAllowed() {
    return commentsAllowed;
  }

  public boolean isDeletable() {
    return deletable;
  }

  public boolean isUndeletable() {
    return undeletable;
  }

  @Nullable
  public Userpic getUserpic() {
    return userpic;
  }

  public boolean isCommitable() {
    return commitable;
  }

  public boolean isShowComments() {
    return showComments;
  }
}
