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

import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.bbcode.LorCodeService;

import java.sql.Timestamp;

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
  
  public SearchItem(SearchHit doc, UserDao userDao, MsgbaseDao msgbaseDao, LorCodeService lorCodeService, boolean secure) {
    msgid = doc.getId();

    if (doc.getHighlightFields().containsKey("title")) {
      title = doc.getHighlightFields().get("title").fragments()[0].string();
    } else if (doc.getFields().containsKey("title")) {
      title = StringUtil.escapeHtml(doc.getFields().get("title").<String>getValue());
    } else {
      title = null;
    }

    if (doc.getHighlightFields().containsKey("topic_title")) {
      topicTitle = doc.getHighlightFields().get("topic_title").fragments()[0].string();
    } else {
      topicTitle = StringUtil.escapeHtml(doc.getFields().get("topic_title").<String>getValue());
    }

    String author = doc.getFields().get("author").getValue();
    postdate = new Timestamp(ISODateTimeFormat.dateTime().parseDateTime(doc.getFields().get("postdate").<String>getValue()).getMillis());
    topic = doc.getFields().get("topic_id").getValue();
    section = doc.getFields().get("section").getValue();

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

    user = userDao.getUser(author);
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
