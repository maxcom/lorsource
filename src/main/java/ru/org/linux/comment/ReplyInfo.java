/*
 * Copyright 1998-2024 Linux.org.ru
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;

public class ReplyInfo {
  private final int id;
  private final String author;
  private final String title;
  private final Date postdate;
  private final boolean samePage;
  private final boolean deleted;

  public ReplyInfo(int id, boolean deleted) {
    this.id = id;
    this.deleted = deleted;
    // Для удаленных сообщений не имеет значения,
    // так как эта информация не должна отображаться.
    this.author = null;
    this.title = null;
    this.postdate = new Date(0);
    this.samePage = false;
  }

  public ReplyInfo(
          int id,
          @Nonnull String author,
          @Nullable String title,
          @Nonnull Date postdate,
          boolean samePage,
          boolean deleted
  ) {
    this.id = id;
    this.author = author;
    this.title = title;
    this.postdate = postdate;
    this.samePage = samePage;
    this.deleted = deleted;
  }

  public int getId() {
    return id;
  }

  public String getAuthor() {
    return author;
  }

  public String getTitle() {
    return title;
  }

  public Date getPostdate() {
    return postdate;
  }

  public boolean isSamePage() {
    return samePage;
  }

  public boolean isDeleted() {
    return deleted;
  }
}
