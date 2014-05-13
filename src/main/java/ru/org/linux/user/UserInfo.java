/*
 * Copyright 1998-2014 Linux.org.ru
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

public class UserInfo {
  private final String url;
  private final String town;
  private final Timestamp lastLogin;
  private final Timestamp registrationDate;

  public UserInfo(ResultSet resultSet) throws SQLException {
    url = resultSet.getString("url");
    town = resultSet.getString("town");
    lastLogin = resultSet.getTimestamp("lastlogin");
    registrationDate = resultSet.getTimestamp("regdate");
  }

  public String getUrl() {
    return url;
  }

  public String getTown() {
    return town;
  }

  public Timestamp getLastLogin() {
    return lastLogin;
  }

  public Timestamp getRegistrationDate() {
    return registrationDate;
  }
}
