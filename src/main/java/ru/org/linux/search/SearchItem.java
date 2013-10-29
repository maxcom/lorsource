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
import org.springframework.web.util.UriComponentsBuilder;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.util.StringUtil;

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
  private final String group;
  
  public SearchItem(SearchHit doc, UserDao userDao) {
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
    group = doc.getFields().get("group").getValue();

    if (doc.getHighlightFields().containsKey("message")) {
      message = doc.getHighlightFields().get("message").fragments()[0].string();
    } else {
      String fullMessage = doc.getFields().get("message").getValue();

      if (fullMessage.length()>SearchViewer.MESSAGE_FRAGMENT) {
        message = fullMessage.substring(0, SearchViewer.MESSAGE_FRAGMENT);
      } else {
        message = fullMessage;
      }
    }

    if(!"wiki".equals(section)) {
      virtualWiki = null;
    } else {
      // Wiki id like <virtual_wiki>-<topic_id>
      String[] msgIds = msgid.split("-");
      if(msgIds.length != 2) {
        throw new RuntimeException("Invalid wiki ID");
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
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/{section}/{group}/{msgid}");

        return builder.buildAndExpand(section, group, topic).toUriString();
      } else {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/{section}/{group}/{msgid}?cid={cid}");

        return builder.buildAndExpand(section, group, topic, msgid).toUriString();
      }
    }
  }
}
