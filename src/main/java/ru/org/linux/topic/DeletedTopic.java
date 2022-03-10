/*
 * Copyright 1998-2022 Linux.org.ru
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
package ru.org.linux.topic;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class DeletedTopic {
  private final String nick;
  private final int id;
  private final String title;
  private final String reason;
  private final Timestamp postdate;
  private final Timestamp delDate;

  public DeletedTopic(ResultSet rs) throws SQLException {
    nick = rs.getString("nick");
    id = rs.getInt("msgid");
    title = rs.getString("subj");
    reason = rs.getString("reason");
    postdate = rs.getTimestamp("postdate");
    delDate = rs.getTimestamp("delDate");
  }

  public String getNick() {
    return nick;
  }

  public int getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getReason() {
    return reason;
  }

  public Timestamp getPostdate() {
    return postdate;
  }

  public Timestamp getDelDate() {
    return delDate;
  }
}
