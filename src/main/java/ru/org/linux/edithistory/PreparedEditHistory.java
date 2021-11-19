/*
 * Copyright 1998-2021 Linux.org.ru
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

package ru.org.linux.edithistory;

import ru.org.linux.markup.MessageTextService;
import ru.org.linux.markup.MarkupType;
import ru.org.linux.poll.Poll;
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.tag.TagRef;
import ru.org.linux.topic.PreparedImage;
import ru.org.linux.user.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;

public class PreparedEditHistory {
  private final boolean original;
  private final User editor;
  private final PreparedImage image;
  private final String message;
  private final boolean current;
  private final String title;
  private final List<TagRef> tags;
  private final String url;
  private final String linktext;
  private final Boolean minor;
  private final Date editdate;
  private final boolean imageDeleted;
  private final Poll poll;
  private final Integer restoreFrom;

  public PreparedEditHistory(
          MessageTextService lorCodeService,
          @Nonnull User editor,
          Date editdate,
          String message,
          String title,
          String url,
          String linktext,
          List<TagRef> tags,
          boolean current,
          boolean original,
          @Nullable Boolean minor,
          PreparedImage image,
          Boolean imageDeleted,
          MarkupType markup,
          Poll poll,
          Integer restoreFrom) {
    this.original = original;

    this.editor = editor;
    this.image = image;
    this.imageDeleted = imageDeleted;
    this.poll = poll;

    if (message!=null) {
      this.restoreFrom = restoreFrom;
      this.message = lorCodeService.renderCommentText(MessageText.apply(message, markup), false);
    } else {
      this.message = null;
      this.restoreFrom = null;
    }

    this.title = title;
    this.url = url;
    this.linktext = linktext;
    this.current = current;
    this.tags = tags;
    this.minor = minor;
    this.editdate = editdate;
  }

  public Date getEditDate() {
    return editdate;
  }

  @Nonnull
  public User getEditor() {
    return editor;
  }

  public String getMessage() {
    return message;
  }

  public boolean isCurrent() {
    return current;
  }

  public String getTitle() {
    return title;
  }

  public String getUrl() {
    return url;
  }

  public String getLinktext() {
    return linktext;
  }

  public boolean isOriginal() {
    return original;
  }

  public List<TagRef> getTags() {
    return tags;
  }

  public Boolean getMinor() {
    return minor;
  }

  public PreparedImage getImage() {
    return image;
  }

  public boolean isImageDeleted() {
    return imageDeleted;
  }

  public Poll getPoll() {
    return poll;
  }

  public Integer getRestoreFrom() {
    return restoreFrom;
  }
}
