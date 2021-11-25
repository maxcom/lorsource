/*
 * Copyright 1998-2021 Linux.org.ru
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

package ru.org.linux.group;

import com.google.common.collect.ImmutableList;
import ru.org.linux.section.Section;
import ru.org.linux.user.User;

import javax.annotation.Nullable;
import java.sql.Timestamp;

/**
 *
 */
public class TopicsListItem {
  private final User author; // topic author
  private final int msgid; // topic id
  private final Timestamp lastmod; // topic lastmod
  private final int stat1; // comment count
  private final int groupId;
  private final String groupTitle;
  private final String title;
  private final int cid; // tracker only!

  @Nullable
  private final User lastCommentBy; // tracker only!

  private final boolean resolved;
  private final int section;
  private final String groupUrlName;
  private final Timestamp postdate; // date of last comment or topic postdate if none ( = lastmod for group)
  private final boolean uncommited; // awaits for approve
  private final int pages; // number of pages
  private final ImmutableList<String> tags;
  private final boolean deleted;
  private final boolean sticky;

  public TopicsListItem(User author, int msgid, Timestamp lastmod,
                        int stat1,
                        int groupId, String groupTitle, String title,
                        int cid, User lastCommentBy, boolean resolved,
                        int section, String groupUrlName,
                        Timestamp postdate, boolean uncommited, int pages, ImmutableList<String> tags, boolean deleted,
                        boolean sticky) {
    this.author = author;
    this.msgid = msgid;
    this.lastmod = lastmod;
    this.stat1 = stat1;
    this.groupId = groupId;
    this.groupTitle = groupTitle;
    this.title = title;
    this.cid = cid;
    this.lastCommentBy = lastCommentBy;
    this.resolved = resolved;
    this.section = section;
    this.groupUrlName = groupUrlName;
    this.postdate = postdate;
    this.uncommited = uncommited;
    this.pages = pages;
    this.tags = tags;
    this.deleted = deleted;
    this.sticky = sticky;
  }

  public String getLastPageUrl() {
    if (pages > 1) {
      return getGroupUrl() + msgid + "/page" + (pages - 1) + "?lastmod=" + lastmod.getTime();
    } else {
      return getGroupUrl() + msgid + "?lastmod=" + lastmod.getTime();
    }
  }

  public String getCommentUrl() {
    if (cid!=0) {
      return getGroupUrl() + msgid + "?cid=" + cid;
    } else {
      return getLastPageUrl();
    }
  }

  public String getFirstPageUrl() {
    if (pages<=1) {
      return getGroupUrl() + msgid + "?lastmod=" + lastmod.getTime();
    } else {
      return getCanonicalUrl();
    }
  }

  public String getCanonicalUrl() {
    return getGroupUrl() + msgid;
  }

  public String getGroupUrl() {
    return Section.getSectionLink(section) + groupUrlName + '/';
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
    return title;
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

  public boolean isDeleted() {
    return deleted;
  }

  public boolean isSticky() {
    return sticky;
  }
}
