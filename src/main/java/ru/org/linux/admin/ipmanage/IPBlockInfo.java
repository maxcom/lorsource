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

package ru.org.linux.admin.ipmanage;

import ru.org.linux.user.AccessViolationException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

public class IPBlockInfo {
  private final String reason;
  private final Timestamp banDate;
  private final Timestamp originalDate;
  private final int moderator;

  public IPBlockInfo(ResultSet rs) throws SQLException {
    reason = rs.getString("reason");
    banDate = rs.getTimestamp("ban_date");
    originalDate = rs.getTimestamp("date");
    moderator = rs.getInt("mod_id");
  }

  public boolean isBlocked() {
    return banDate == null || banDate.after(new Date());
  }

  @Deprecated
  public void checkBlock() throws AccessViolationException {
    if (isBlocked()) {
      throw new AccessViolationException("Постинг заблокирован: " + reason);
    }
  }

  public Timestamp getOriginalDate() {
    return originalDate;
  }

  public Timestamp getBanDate() {
    return banDate;
  }

  public String getReason() {
    return reason;
  }

  public int getModerator() {
    return moderator;
  }
}
