/*
 * Copyright 1998-2015 Linux.org.ru
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

package ru.org.linux.tracker;

import com.google.common.collect.ImmutableList;
import ru.org.linux.section.Section;
import ru.org.linux.user.User;
import ru.org.linux.util.URLUtil;

import java.sql.Timestamp;

/**
 *
 */
public class TrackerItem {
  private final User author;
  private final int msgid;
  private final Timestamp lastmod;
  private final int stat1;
  private final int groupId;
  private final String groupTitle;
  private final String title;
  private final int cid;
  private final User lastCommentBy;
  private final boolean resolved;
  private final int section;
  private final String groupUrlName;
  private final Timestamp postdate;
  private final boolean uncommited;
  private final int pages;
  private final ImmutableList<String> tags;

  public TrackerItem(User author, int msgid, Timestamp lastmod,
                     int stat1,
                     int groupId, String groupTitle, String title,
                     int cid, User lastCommentBy, boolean resolved,
                     int section, String groupUrlName,
                     Timestamp postdate, boolean uncommited, int pages, ImmutableList<String> tags) {
    this.author = author;
    this.msgid = msgid;
    this.lastmod = lastmod;
    this.stat1 = stat1;
    this.groupId = groupId;
    this.groupTitle = groupTitle;
    this.title = title;
    this.cid = cid;
    this.lastCommentBy = lastCommentBy;
    this.resolved =resolved;
    this.section = section;
    this.groupUrlName = groupUrlName;
    this.postdate = postdate;
    this.uncommited = uncommited;
    this.pages = pages;
    this.tags = tags;
  }

  public String getUrl() {
    if(section != 0) {
      if (pages > 1) {
        return getGroupUrl() + msgid + "/page" + Integer.toString(pages - 1) + "?lastmod=" + lastmod.getTime();
      } else {
        return getGroupUrl() + msgid + "?lastmod=" + lastmod.getTime();
      }
    } else {
      return String.format("/wiki/en/%s", URLUtil.encodeAndEscapeTopicName(title));
    }
  }

  public String getUrlReverse() {
    if(section != 0) {
      return getGroupUrl() + '/' + msgid + "?lastmod=" + lastmod.getTime();
    } else {
      return String.format("/wiki/en/%s", URLUtil.encodeAndEscapeTopicName(title));
    }
  }

  public String getGroupUrl() {
    if(section != 0) {
      return Section.getSectionLink(section) + groupUrlName + '/';
    } else {
      return "/wiki/";
    }
  }

  public boolean isWiki() {
    return section == 0;
  }

  public int getMsgid() {
    return msgid;
  }

  public Timestamp getLastmod() {
    return lastmod;
  }

  public int getStat1() {
    return stat1;
  }

  public int getGroupId() {
    return groupId;
  }

  public String getGroupTitle() {
    return groupTitle;
  }

  public String getTitle() {
    if(section != 0) {
      return title;
    } else {
      if(title.startsWith("Comments:")) {
        return title.substring(9); // откусываем Comments
      } else {
        return title;
      }
    }
  }

  public boolean isWikiArticle() {
    return isWiki() && !title.startsWith("Comments:");
  }

  public boolean isWikiComment() {
    return isWiki() && title.startsWith("Comments:");
  }

  public int getPages() {
    return pages;
  }

  public User getAuthor() {
    if (lastCommentBy!=null) {
      return lastCommentBy;
    } else {
      return author;
    }
  }

  public User getTopicAuthor() {
    return author;
  }

  public boolean isResolved() {
    return resolved;
  }

  public int getSection() {
    return section;
  }

  public Timestamp getPostdate() {
    return postdate;
  }

  public boolean isUncommited() {
    return uncommited;
  }

  public int getCid() {
    return cid;
  }

  public ImmutableList<String> getTags() {
    return tags;
  }
}
