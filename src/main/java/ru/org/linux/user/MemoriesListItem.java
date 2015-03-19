/*
 * Copyright 1998-2015 Linux.org.ru
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

package ru.org.linux.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class MemoriesListItem {
  private final int id;
  private final int userid;
  private final Timestamp timestamp;
  private final int topic;
  private final boolean watch;

  public MemoriesListItem(ResultSet rs) throws SQLException {
    id = rs.getInt("id");
    userid = rs.getInt("userid");
    timestamp = rs.getTimestamp("add_date");
    topic = rs.getInt("topic");
    watch = rs.getBoolean("watch");
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

  public boolean isWatch() {
    return watch;
  }
}
