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

import java.sql.*;

import org.springframework.jdbc.support.JdbcUtils;

public class BanInfo {
  private final Timestamp date;
  private final String reason;
  private final User moderator;

  public BanInfo(Timestamp date, String reason, User moderator) {
    this.date = date;
    this.reason = reason;
    this.moderator = moderator;
  }

  public static BanInfo getBanInfo(Connection db, User user) throws SQLException {
    Statement st = null;

    try {
      st = db.createStatement();
      ResultSet rs = st.executeQuery("SELECT * FROM ban_info WHERE userid="+user.getId());

      if (!rs.next()) {
        return null;
      } else {
        try {
          Timestamp date = rs.getTimestamp("bandate");
          String reason = rs.getString("reason");
          User moderator = User.getUserCached(db, rs.getInt("ban_by"));

          return new BanInfo(date, reason, moderator);
        } catch (UserNotFoundException e) {
          throw new RuntimeException(e);
        }
      }
    } finally {
      JdbcUtils.closeStatement(st);
    }
  }

  public Timestamp getDate() {
    return date;
  }

  public String getReason() {
    return reason;
  }

  public User getModerator() {
    return moderator;
  }
}
