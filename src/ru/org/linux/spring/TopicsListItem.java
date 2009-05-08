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

package ru.org.linux.spring;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class TopicsListItem {
  private final String subj;
  private final Timestamp lastmod;
  private final String nick;
  private final int msgid;
  private final boolean deleted;
  private final int stat1, stat3, stat4;
  private final boolean sticky;

  // SELECT topics.title as subj, lastmod, nick, topics.id as msgid, deleted, topics.stat1, topics.stat3, topics.stat4, topics.sticky
  public TopicsListItem(ResultSet rs) throws SQLException {
    subj = rs.getString("subj");
    lastmod = rs.getTimestamp("lastmod");
    nick = rs.getString("nick");
    msgid = rs.getInt("msgid");
    deleted = rs.getBoolean("deleted");
    stat1 = rs.getInt("stat1");
    stat3 = rs.getInt("stat3");
    stat4 = rs.getInt("stat4");
    sticky = rs.getBoolean("sticky");
  }

  public String getSubj() {
    return subj;
  }

  public Timestamp getLastmod() {
    return lastmod;
  }

  public String getNick() {
    return nick;
  }

  public int getMsgid() {
    return msgid;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public int getStat1() {
    return stat1;
  }

  public int getStat3() {
    return stat3;
  }

  public int getStat4() {
    return stat4;
  }

  public boolean isSticky() {
    return sticky;
  }
}
