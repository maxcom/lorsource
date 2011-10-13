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

import ru.org.linux.spring.dao.TagDao;
import ru.org.linux.util.bbcode.ParserUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
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

  public PreparedEditInfo(
    Connection db,
    EditInfoDTO editInfo,
    String message,
    String title,
    String url,
    String linktext,
    List<String> tags,
    boolean current,
    boolean original
  ) throws UserNotFoundException {
    this.editInfo = editInfo;
    this.original = original;

    editor = User.getUserCached(db, editInfo.getEditor());

    if (message!=null) {
      this.message = ParserUtil.bb2xhtml(message, true, true, "", db);
    } else {
      this.message = null;
    }

    this.title = title;
    this.url = url;
    this.linktext = linktext;

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

  @Deprecated
  public static List<PreparedEditInfo> build(Connection db, Message message) throws SQLException, UserNotFoundException, UserErrorException {
    List<EditInfoDTO> editInfoDTOs = message.loadEditInfo(db);
    List<PreparedEditInfo> editInfos = new ArrayList<PreparedEditInfo>(editInfoDTOs.size());

    String currentMessage = message.getMessage();
    String currentTitle = message.getTitle();
    String currentUrl = message.getUrl();
    String currentLinktext = message.getLinktext();
    List<String> currentTags = TagDao.getMessageTags(db, message.getMessageId());

    for (int i = 0; i<editInfoDTOs.size(); i++) {
      EditInfoDTO dto = editInfoDTOs.get(i);

      editInfos.add(
        new PreparedEditInfo(
          db,
          dto,
          dto.getOldmessage()!=null ? currentMessage : null,
          dto.getOldtitle()!=null ? currentTitle : null,
          dto.getOldurl()!=null ? currentUrl : null,
          dto.getOldlinktext()!=null ? currentLinktext : null,
          dto.getOldtags()!=null ? currentTags : null,
          i==0,
          false
        )
      );

      if (dto.getOldmessage() !=null) {
        currentMessage = dto.getOldmessage();
      }

      if (dto.getOldtitle() != null) {
        currentTitle = dto.getOldtitle();
      }

      if (dto.getOldurl() != null) {
        currentUrl = dto.getOldurl();
      }

      if (dto.getOldlinktext() != null) {
        currentLinktext = dto.getOldlinktext();
      }

      if (dto.getOldtags()!=null) {
        currentTags = TagDao.parseTags(dto.getOldtags());
      }
    }

    if (!editInfoDTOs.isEmpty()) {
      EditInfoDTO current = EditInfoDTO.createFromMessage(db, message);

      editInfos.add(new PreparedEditInfo(db, current, currentMessage, currentTitle, currentUrl, currentLinktext, currentTags, false, true));
    }

    return editInfos;
  }
}
