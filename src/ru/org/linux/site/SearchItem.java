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

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import ru.org.linux.util.StringUtil;

import org.javabb.bbcode.BBCodeProcessor;

public class SearchItem implements Serializable {
//  "msgs.id, msgs.title, msgs.postdate, topic, msgs.userid, rank(idxFTI, q) as rank, message, bbcode"
  private final int msgid;
  private final String title;
  private final Timestamp postdate;
  private final int topic;
  private final User user;
  private final String message;
  private final boolean bbcode;

  private static final long serialVersionUID = -8100510220616995405L;

  SearchItem(Connection db, ResultSet rs) throws SQLException {
    msgid = rs.getInt("id");
    title = StringUtil.makeTitle(rs.getString("title"));
    postdate = rs.getTimestamp("postdate");
    topic = rs.getInt("topic");
    try {
      user = User.getUserCached(db, rs.getInt("userid"));
    } catch (UserNotFoundException e) {
      throw new RuntimeException(e);
    }
    String rawMessage = rs.getString("message");
    bbcode = rs.getBoolean("bbcode");

    if (bbcode) {
      BBCodeProcessor proc = new BBCodeProcessor();
      message = proc.preparePostText(db, rawMessage);
    } else {
      message = rawMessage;
    }
  }

  public int getMsgid() {
    return msgid;
  }

  public String getTitle() {
    return title;
  }

  public Timestamp getPostdate() {
    return postdate;
  }

  public int getTopic() {
    return topic;
  }

  public User getUser() {
    return user;
  }

  public String getMessage() {
    return message;
  }

  public boolean isBbcode() {
    return bbcode;
  }

  public String getUrl() {
    if (topic==0 || topic==msgid) {
      return "view-message.jsp?msgid="+msgid;
    } else {
      return "jump-message.jsp?msgid="+topic+"&amp;cid="+msgid;
    }
  }
}
