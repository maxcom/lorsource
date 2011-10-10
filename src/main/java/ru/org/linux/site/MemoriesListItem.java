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

public class MemoriesListItem {
  private final int id;
  private final int userid;
  private final Timestamp timestamp;
  private final int topic;

  public MemoriesListItem(Connection db, int id) throws SQLException, RecordNotFoundException {
    Statement st = db.createStatement();

    ResultSet rs = st.executeQuery("SELECT * FROM memories WHERE id="+id);

    if (!rs.next()) {
      throw new RecordNotFoundException();
    }

    this.id = id;
    userid = rs.getInt("userid");
    timestamp = rs.getTimestamp("add_date");
    topic = rs.getInt("topic");
  }

  private MemoriesListItem(ResultSet rs) throws SQLException {
    id = rs.getInt("id");
    userid = rs.getInt("userid");
    timestamp = rs.getTimestamp("add_date");
    topic = rs.getInt("topic");
  }

  public static MemoriesListItem getMemoriesListItem(Connection db, int id) throws SQLException {
    Statement st = db.createStatement();

    ResultSet rs = st.executeQuery("SELECT * FROM memories WHERE id="+id);

    if (!rs.next()) {
      return null;
    } else {
      return new MemoriesListItem(rs);
    }
  }

  public int getId() {
    return id;
  }

  public int getUserid() {
    return userid;
  }

  public Timestamp getTimestamp() {
    return timestamp;
  }

  public int getTopic() {
    return topic;
  }

  @Deprecated
  public static int getId(Connection db, int userid, int topic) throws SQLException {
    Statement st = db.createStatement();

    ResultSet rs = st.executeQuery("SELECT id FROM memories WHERE userid="+userid+" AND topic="+topic);

    if (!rs.next()) {
      return 0;
    } else {
      return rs.getInt("id");
    }
  }
}
