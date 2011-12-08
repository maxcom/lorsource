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

package ru.org.linux.spring;

import org.springframework.jdbc.support.rowset.SqlRowSet;
import ru.org.linux.dao.UserDao;
import ru.org.linux.dto.UserDto;
import ru.org.linux.site.Message;
import ru.org.linux.site.UserNotFoundException;
import ru.org.linux.util.StringUtil;

import java.io.Serializable;
import java.sql.Timestamp;

public class TopicsListItem implements Serializable {
  private final String subj;
  private final Timestamp lastmod;
  private final int msgid;
  private final boolean deleted;
  private final int stat1;
  private final int stat3;
  private final int stat4;
  private final boolean sticky;
  private final int pages;
  private final UserDto author;
  private final boolean resolved;

  private static final long serialVersionUID = 5344250574674257995L;

  // SELECT topics.title as subj, sections.name, lastmod, topics.id as msgid, topics.deleted, topics.stat1, topics.stat3, topics.stat4, topics.sticky, userid
  public TopicsListItem(UserDao userDao, SqlRowSet rs, int messagesInPage) {
    subj = StringUtil.makeTitle(rs.getString("subj"));

    Timestamp lastmod = rs.getTimestamp("lastmod");
    if (lastmod==null) {
      this.lastmod = new Timestamp(0);
    } else {
      this.lastmod = lastmod;
    }

    try {
      author = userDao.getUserCached(rs.getInt("userid"));
    } catch (UserNotFoundException e) {
      throw new RuntimeException(e);
    }

    msgid = rs.getInt("msgid");
    deleted = rs.getBoolean("deleted");
    stat1 = rs.getInt("stat1");
    stat3 = rs.getInt("stat3");
    stat4 = rs.getInt("stat4");
    sticky = rs.getBoolean("sticky");
    resolved = rs.getBoolean("resolved");

    pages = Message.getPageCount(stat1, messagesInPage);
  }

  public String getSubj() {
    return subj;
  }

  public Timestamp getLastmod() {
    return lastmod;
  }

  public UserDto getAuthor() {
    return author;
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

  public int getPages() {
    return pages;
  }

  public boolean isResolved(){
    return resolved;
  }
}
