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

public class UserInfo {
  private String url;
  private String town;
  private Timestamp lastLogin;
  private Timestamp registrationDate;

  public UserInfo(Connection db, int id) throws SQLException, UserNotFoundException {
    PreparedStatement userInfo = db.prepareStatement("SELECT url, town, lastlogin, regdate FROM users WHERE id=?");

    userInfo.setInt(1, id);

    ResultSet rs = userInfo.executeQuery();

    if (!rs.next()) {
      throw new UserNotFoundException(id);
    }

    url = rs.getString("url");
    town = rs.getString("town");
    lastLogin = rs.getTimestamp("lastlogin");
    registrationDate = rs.getTimestamp("regdate");

    rs.close();
    userInfo.close();
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
