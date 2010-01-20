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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class IgnoreList {
  private final int userId;
  
  private Map<Integer, String> ignoreList;

  public static Map<Integer, String> getIgnoreList(Connection db, String nick) throws SQLException {
    PreparedStatement pst = db.prepareStatement("SELECT a.ignored,b.nick FROM ignore_list a, users b WHERE a.userid=(SELECT id FROM users WHERE nick=?) AND b.id=a.ignored");
    pst.clearParameters();
    pst.setString(1, nick);
    ResultSet rs = pst.executeQuery();
    Map<Integer, String> cignored = new HashMap<Integer, String>();

    while(rs.next()) {
      cignored.put(rs.getInt("ignored"),rs.getString("nick"));
    }

    return cignored;
  }

  public IgnoreList(Connection db, int userId) throws SQLException {
    if (userId<1) {
      throw new SQLException("Incorrect user ID");
    }
    this.userId = userId;
    ignoreList = new HashMap<Integer, String>();
    PreparedStatement pst = db.prepareStatement("SELECT a.id,a.nick FROM users a, ignore_list b WHERE b.userid=? AND a.id=b.ignored ORDER BY a.nick ASC");
    pst.clearParameters();
    pst.setInt(1, userId); 
    ResultSet rst = pst.executeQuery();
    while (rst.next()) {
      ignoreList.put(rst.getInt("id"),rst.getString("nick"));
    }
  }

  public Map<Integer, String> getIgnoreList() {
    return ignoreList;
  }

  public boolean containsUser(User user) {
    return ignoreList.containsKey(user.getId());
  }

  public void addUser(Connection db, User user) throws SQLException,  AccessViolationException {
    int id = user.getId();
    if (user.canModerate()) {
      throw new AccessViolationException("Нельзя игнорировать модератора");
    } else {
      if (!ignoreList.containsKey(id)) {
        PreparedStatement addPst = db.prepareStatement("INSERT INTO ignore_list (userid,ignored) VALUES(?,?)");
        addPst.clearParameters();
        addPst.setInt(1, userId);
        addPst.setInt(2, id);
        addPst.executeUpdate();
        ignoreList.put(id, user.getNick());
      }
    }
  }

  public boolean removeNick(Connection db, int uid) throws SQLException {
    if (!ignoreList.containsKey(uid)) {
      return false;
    }
    PreparedStatement pst = db.prepareStatement("DELETE FROM ignore_list WHERE userid=? AND ignored=?"); 
    pst.clearParameters();
    pst.setInt(1,userId);
    pst.setInt(2,uid);
    boolean r = pst.executeUpdate() == 1;

    if (r) {
      ignoreList.remove(uid);
    }

    return r;
  }
}
