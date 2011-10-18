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
import java.sql.Statement;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class PollVariant {
  private final int id;
  private String label;
  private final int votes;

  public PollVariant(int id, String label, int votes) {
    this.id = id;
    this.label = label;
    this.votes = votes;
  }

  public int getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  public void updateLabel(Connection db, String label) throws SQLException {
    if (this.label.equals(label)) {
      return;
    }

    PreparedStatement pst = db.prepareStatement("UPDATE votes SET label=? WHERE id=?");
    pst.setString(1, label);
    pst.setInt(2, id);
    pst.executeUpdate();

    this.label = label;
  }

  public int getVotes() {
    return votes;
  }

  public void remove(Connection db) throws SQLException {
    Statement st = db.createStatement();
    st.executeUpdate("DELETE FROM votes WHERE id="+id);
  }

  public static SortedMap<Integer, String> toMap(List<PollVariant> list) {
    SortedMap<Integer, String> map = new TreeMap<Integer, String>();

    for (PollVariant v : list) {
      map.put(v.getId(), v.getLabel());
    }

    return map;
  }
}
