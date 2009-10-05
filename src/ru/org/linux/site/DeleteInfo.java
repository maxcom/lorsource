/*
 * Copyright 1998-2009 Linux.org.ru
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

import java.io.Serializable;
import java.sql.*;

public class DeleteInfo implements Serializable {
  private final String nick;
  private final int userid;
  private final String reason;
  private final Timestamp delDate;

  private DeleteInfo(String nick, int userid, String reason, Timestamp delDate) {
    this.nick = nick;
    this.reason = reason;
    this.userid = userid;
    this.delDate = delDate;
  }

  public static DeleteInfo getDeleteInfo(Connection db, int msgid) throws SQLException {
    PreparedStatement pst = null;
    try {
      pst = db.prepareStatement("SELECT nick,reason,users.id as userid, deldate FROM del_info,users WHERE msgid=? AND users.id=del_info.delby");

      pst.setInt(1, msgid);

      ResultSet rs = pst.executeQuery();

      if (!rs.next()) {
        return null;
      }

      return new DeleteInfo(rs.getString("nick"), rs.getInt("userid"), rs.getString("reason"), rs.getTimestamp("deldate"));
    } finally {
      if (pst!=null) {
        pst.close();
      }
    }
  }

  public String getNick() {
    return nick;
  }

  public int getUserid() {
    return userid;
  }

  public String getReason() {
    return reason;
  }

  public Timestamp getDelDate() {
    return delDate;
  }
}
