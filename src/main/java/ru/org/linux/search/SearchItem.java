/*
 * Copyright 1998-2013 Linux.org.ru
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.bbcode.LorCodeService;

import java.sql.Timestamp;
import java.util.Date;

import static ru.org.linux.util.URLUtil.buildWikiURL;

public class SearchItem {
  private static final Logger logger = LoggerFactory.getLogger(SearchItem.class);
  private final String msgid;
  private final String title;
  private final String topicTitle;
  private final Timestamp postdate;
  private final int topic;
  private final User user;
  private final String message;
  private final String virtualWiki;
  private final String section;
  
  public SearchItem(SolrDocument doc, UserDao userDao, MsgbaseDao msgbaseDao, LorCodeService lorCodeService, boolean secure) {
    msgid = (String) doc.getFieldValue("id");
    title = (String) doc.getFieldValue("title");
    topicTitle = (String) doc.getFieldValue("topic_title");
    int userid = (Integer) doc.getFieldValue("user_id");
    Date postdate_dt = (Date) doc.getFieldValue("postdate");
    postdate = new Timestamp(postdate_dt.getTime());
    topic = (Integer) doc.getFieldValue("topic_id");
    section = (String) doc.getFieldValue("section");

    if(!"wiki".equals(section)) {
      virtualWiki = null;
      MessageText messageText = msgbaseDao.getMessageText(Integer.valueOf(msgid));
      String rawMessage = messageText.getText();
      if (messageText.isLorcode()) {
        message = lorCodeService.parseComment(rawMessage, secure, false);
      } else {
        message = rawMessage;
      }
    } else {
      // Wiki id like <virtual_wiki>-<topic_id>
      String[] msgIds = msgid.split("-");
      if(msgIds.length != 2) {
        throw new RuntimeException("Invalid wiki ID");
      }
      
      String content = msgbaseDao.getMessageTextFromWiki(Integer.valueOf(msgIds[1]));      
      String msg = StringUtil.escapeHtml(content.substring(0, Math.min(1300, content.length())));
      if(Math.min(1300, content.length()) == 1300) {
        message = msg + "...";
      } else {
        message = msg;
      }
      virtualWiki = msgIds[0];
    }

    user = userDao.getUserCached(userid);
  }

  public int getMsgid() {
    if("wiki".equals(section)) {
      return 0;
    } else {
      return Integer.valueOf(msgid);
    }
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
    if("wiki".equals(section)) {
      try {
        return buildWikiURL(virtualWiki, title);
      } catch (Exception e) {
        logger.warn("Fail build topic url for " + title + " in " + virtualWiki);
        return "#";
      }      
    } else {
      if (topic==0 || topic==Integer.valueOf(msgid)) {
        return "view-message.jsp?msgid="+msgid;
      } else {
        return "jump-message.jsp?msgid="+topic+"&amp;cid="+msgid;
      }
    }
  }
}
