/*
 * Copyright 1998-2013 Linux.org.ru
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

public class Remark {
  private final int id;
  private final int userId;
  private final int refUserId;
  private final String text;

  public Remark(ResultSet resultSet) throws SQLException {
    id = resultSet.getInt("id");
    userId = resultSet.getInt("user_id");
    refUserId = resultSet.getInt("ref_user_id");
    text = resultSet.getString("remark_text");
  }

  public int getId() {
    return id;
  }

  public int getRefUserId() {
    return refUserId;
  }

  public int getUserId() {
    return userId;
  }

  public String getText() {
    return text;
  }
}
