/*
 * Copyright 1998-2022 Linux.org.ru
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

package ru.org.linux.user;

import com.sun.syndication.feed.synd.*;
import org.apache.commons.text.StringEscapeUtils;
import ru.org.linux.spring.AbstractRomeView;
import ru.org.linux.spring.SiteConfig;
import ru.org.linux.util.StringUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class UserEventFeedView extends AbstractRomeView {
  private final SiteConfig siteConfig;

  public UserEventFeedView(SiteConfig siteConfig) {
    this.siteConfig = siteConfig;
  }

  @Override
  protected void createFeed(SyndFeed feed, Map model) {
    @SuppressWarnings("unchecked")
    List<PreparedUserEvent> list = (List<PreparedUserEvent>) model.get("topicsList");
    String s = "Уведомления пользователя " + model.get("nick");
    feed.setTitle(s);
    feed.setLink(siteConfig.getSecureUrl());
    feed.setUri(siteConfig.getSecureUrl());
    feed.setAuthor("");
    feed.setDescription(s);

    Date lastModified;
    if (!list.isEmpty()) {
      lastModified = list.get(0).getEvent().getEventDate();
    } else {
      lastModified = new Date();
    }
    feed.setPublishedDate(lastModified);

    List<SyndEntry> entries = new ArrayList<>();
    feed.setEntries(entries);
    for (PreparedUserEvent preparedUserEvent : list) {
      UserEvent item = preparedUserEvent.getEvent();
      
      SyndEntry feedEntry = new SyndEntryImpl();
      feedEntry.setPublishedDate(item.getEventDate());
      feedEntry.setTitle(StringEscapeUtils.unescapeHtml4(item.getSubj()));

      String link;

      if (item.getCid()!=0) {
        feedEntry.setAuthor(preparedUserEvent.getAuthor().getNick());

        link = String.format(
          "%s/jump-message.jsp?msgid=%s&cid=%s",
          siteConfig.getSecureUrlWithoutSlash(),
          item.getTopicId(),
          item.getCid()
        );
      } else {
        link = String.format(
          "%s/view-message.jsp?msgid=%s",
           siteConfig.getSecureUrlWithoutSlash(),
           item.getTopicId()
        );
      }

      feedEntry.setLink(link);
      feedEntry.setUri(link);

      if (preparedUserEvent.getMessageText() != null){
        SyndContent message = new SyndContentImpl();
        message.setValue(StringUtil.removeInvalidXmlChars(preparedUserEvent.getMessageText()));
        message.setType("text/html");
        feedEntry.setDescription(message);
      }
      entries.add(feedEntry);
    }
  }
}
