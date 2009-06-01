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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;

/**
 * User: rsvato
 * Date: Jun 1, 2009
 * Time: 3:21:53 PM
 */
public class ReplyFeedView extends AbstractRomeView {
  protected void createFeed(SyndFeed feed, Map model) {
    @SuppressWarnings("unchecked")
    List<ShowRepliesController.MyTopicsListItem> list = (List<ShowRepliesController.MyTopicsListItem>) model.get("topicsList");
    final String s = "Последние 30 ответов на комментарии пользователя " + String.valueOf(model.get("nick"));
    feed.setTitle(s);
    feed.setLink("http://www.linux.org.ru");
    feed.setDescription(s);
    Date lastModified = new Date();
    if (!list.isEmpty()) {
      final Timestamp timestamp = list.get(0).getLastmod();
      lastModified = new Date(timestamp.getTime());
    }
    feed.setPublishedDate(lastModified);
    List<SyndEntry> entries = new ArrayList<SyndEntry>();
    feed.setEntries(entries);
    for (ShowRepliesController.MyTopicsListItem item : list) {
      SyndEntry feedEntry = new SyndEntryImpl();
      feedEntry.setPublishedDate(new Date(item.getLastmod().getTime()));
      feedEntry.setTitle(item.getSubj());
      feedEntry.setLink(String.format("http://www.linux.org.ru/jump-message.jsp?msgid=%s&cid=%s",
        String.valueOf(item.getMsgid()), String.valueOf(item.getCid())));
      entries.add(feedEntry);
    }
  }
}
