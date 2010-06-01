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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

public class UserStatistics {
  private final int ignoreCount;
  private final int commentCount;

  public UserStatistics(Connection db, int id) throws SQLException {
    PreparedStatement ignoreStat = db.prepareStatement("SELECT count(*) as inum FROM ignore_list WHERE ignored=?");
    PreparedStatement commentStat = db.prepareStatement("SELECT count(*) as c FROM comments WHERE userid=?");

    ignoreStat.setInt(1, id);
    commentStat.setInt(1, id);

    ResultSet ignoreResult = ignoreStat.executeQuery();
    if (ignoreResult.next()) {
      ignoreCount = ignoreResult.getInt(1);
    } else {
      ignoreCount = 0;
    }
    ignoreResult.close();

    ResultSet commentResult = commentStat.executeQuery();
    if (commentResult.next()) {
      commentCount = commentResult.getInt(1);
    } else {
      commentCount = 0;
    }
  }

  public int getIgnoreCount() {
    return ignoreCount;
  }

  public int getCommentCount() {
    return commentCount;
  }
}
