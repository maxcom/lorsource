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

import ru.org.linux.spring.dao.UserDao;
import ru.org.linux.util.bbcode.LorCodeService;

import java.util.List;

public class PreparedEditInfo {
  private final EditInfoDTO editInfo;
  private final boolean original;
  private final User editor;
  private final String message;
  private final boolean current;
  private final String title;
  private final List<String> tags;
  private final String url;
  private final String linktext;
  private final boolean showLink;

  public PreparedEditInfo(
    LorCodeService lorCodeService,
    boolean secure,
    UserDao userDao,
    EditInfoDTO editInfo,
    String message,
    String title,
    String url,
    String linktext,
    boolean showLink,
    List<String> tags,
    boolean current,
    boolean original
  ) throws UserNotFoundException {
    this.editInfo = editInfo;
    this.original = original;

    editor = userDao.getUserCached(editInfo.getEditor());

    if (message!=null) {
      this.message = lorCodeService.parseComment(message, secure);
    } else {
      this.message = null;
    }

    this.title = title;
    this.url = url;
    this.linktext = linktext;
    this.showLink = showLink;

    this.current = current;

    this.tags = tags;
  }

  public EditInfoDTO getEditInfo() {
    return editInfo;
  }

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

  public List<String> getTags() {
    return tags;
  }

  public boolean isShowLink() {
    return showLink;
  }
}
