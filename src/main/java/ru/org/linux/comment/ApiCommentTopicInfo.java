/*
 * Copyright 1998-2016 Linux.org.ru
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

import ru.org.linux.site.PublicApi;

@PublicApi
public class ApiCommentTopicInfo {
  private final int id;
  private final String link;

  private final boolean commentsAllowed;

  public ApiCommentTopicInfo(int id, String link, boolean commentsAllowed) {
    this.id = id;
    this.link = link;
    this.commentsAllowed = commentsAllowed;
  }

  public int getId() {
    return id;
  }

  public String getLink() {
    return link;
  }

  public boolean isCommentsAllowed() {
    return commentsAllowed;
  }
}
