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

public class MessageMenu {
  private final boolean editable;
  private final boolean resolvable;
  private final int memoriesId;

  public MessageMenu(Connection db, PreparedMessage message, User currentUser) throws SQLException {
    editable = currentUser!=null && message.isEditable(currentUser);

    if (currentUser!=null) {
      resolvable = (currentUser.canModerate() || (message.getAuthor().getId()==currentUser.getId())) &&
            message.getGroup().isResolvable();
      memoriesId = MemoriesListItem.getId(db, currentUser.getId(), message.getId());
    } else {
      resolvable = false;
      memoriesId = 0;
    }
  }

  public boolean isEditable() {
    return editable;
  }

  public boolean isResolvable() {
    return resolvable;
  }

  public int getMemoriesId() {
    return memoriesId;
  }
}