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

import java.sql.Connection;
import java.sql.SQLException;

import org.javabb.bbcode.BBCodeProcessor;

public class PreparedEditInfo {
  private final EditInfoDTO editInfo;
  private final User editor;
  private final String oldMessage;

  public PreparedEditInfo(Connection db, EditInfoDTO editInfo) throws UserNotFoundException, SQLException {
    this.editInfo = editInfo;

    editor = User.getUserCached(db, editInfo.getEditor());

    BBCodeProcessor proc = new BBCodeProcessor();
    oldMessage = proc.preparePostText(db, editInfo.getOldmessage());
  }

  public EditInfoDTO getEditInfo() {
    return editInfo;
  }

  public User getEditor() {
    return editor;
  }

  public String getOldMessage() {
    return oldMessage;
  }
}
