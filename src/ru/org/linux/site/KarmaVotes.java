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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class KarmaVotes {
  private KarmaVotes() {
  }

  public static Set<Integer> getKarmaVotes(Connection db, int userid) throws SQLException {
    Statement st = db.createStatement();

    ResultSet rs = st.executeQuery("SELECT userid FROM karma_voted WHERE voter = "+userid);

    ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();

    while (rs.next()) {
      builder.add(rs.getInt(1));
    }

    return builder.build();
  }
}
