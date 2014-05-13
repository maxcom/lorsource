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

package ru.org.linux.comment;

import com.fasterxml.jackson.annotation.JsonInclude;
import ru.org.linux.site.PublicApi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;

@PublicApi
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReplyInfo {
  private final int id;
  private final String author;
  private final String title;
  private final Date postdate;
  private final boolean samePage;

  public ReplyInfo(
          int id,
          @Nonnull String author,
          @Nullable String title,
          @Nonnull Date postdate,
          boolean samePage
  ) {
    this.id = id;
    this.author = author;
    this.title = title;
    this.postdate = postdate;
    this.samePage = samePage;
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
}
