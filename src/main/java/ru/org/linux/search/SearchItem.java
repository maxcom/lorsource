/*
 * Copyright 1998-2012 Linux.org.ru
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

package ru.org.linux.search;

import org.apache.solr.common.SolrDocument;
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserNotFoundException;
import ru.org.linux.util.bbcode.LorCodeService;

import java.sql.Timestamp;
import java.util.Date;

public class SearchItem {
  private final int msgid;
  private final String title;
  private final String topicTitle;
  private final Timestamp postdate;
  private final int topic;
  private final User user;
  private final String message;

  public SearchItem(SolrDocument doc, UserDao userDao, MsgbaseDao msgbaseDao, LorCodeService lorCodeService, boolean secure) {
    msgid = Integer.valueOf(doc.getFieldValue("id").toString());
    title = (String) doc.getFieldValue("title");
    topicTitle = (String) doc.getFieldValue("topic_title");
    int userid = (Integer) doc.getFieldValue("user_id");
    Date postdate_dt = (Date) doc.getFieldValue("postdate");
    postdate = new Timestamp(postdate_dt.getTime());
    topic = (Integer) doc.getFieldValue("topic_id");

    MessageText messageText = msgbaseDao.getMessageText(msgid);

    String rawMessage = messageText.getText();

    if (messageText.isLorcode()) {
      message = lorCodeService.parseComment(rawMessage, secure);
    } else {
      message = rawMessage;
    }

    try {
      user = userDao.getUserCached(userid);
    } catch (UserNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public int getMsgid() {
    return msgid;
  }

  public String getTitle() {
    if (title!=null && !title.isEmpty()) {
      return title;
    } else {
      return topicTitle;
    }
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

  public String getUrl() {
    if (topic==0 || topic==msgid) {
      return "view-message.jsp?msgid="+msgid;
    } else {
      return "jump-message.jsp?msgid="+topic+"&amp;cid="+msgid;
    }
  }
}
