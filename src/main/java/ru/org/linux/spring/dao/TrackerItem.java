package ru.org.linux.spring.dao;

import ru.org.linux.site.Section;
import ru.org.linux.site.User;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Timestamp;

/**
 *
 */
public class TrackerItem {
  private final User author;
  private final int msgid;
  private final Timestamp lastmod;
  private final int stat1;
  private final int stat3;
  private final int stat4;
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

  public TrackerItem(User author, int msgid, Timestamp lastmod,
                     int stat1, int stat3, int stat4,
                     int groupId, String groupTitle, String title,
                     int cid, User lastCommentBy, boolean resolved,
                     int section, String groupUrlName,
                     Timestamp postdate, boolean uncommited, int pages) {
    this.author = author;
    this.msgid = msgid;
    this.lastmod = lastmod;
    this.stat1 = stat1;
    this.stat3 = stat3;
    this.stat4 = stat4;
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
  }

  public String getUrl() {
    if(section != 0) {
      if (pages > 1) {
        return getGroupUrl() + msgid + "/page" + Integer.toString(pages - 1) + "?lastmod=" + lastmod.getTime();
      } else {
        return getGroupUrl() + msgid + "?lastmod=" + lastmod.getTime();
      }
    } else {
      try {
        return String.format("/wiki/en/%s", URLEncoder.encode(title, "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e.getMessage());
      }
    }
  }

  public String getUrlReverse() {
    if(section != 0) {
      return getGroupUrl() + '/' + msgid + "?lastmod=" + lastmod.getTime();
    } else {
      try {
        return String.format("/wiki/en/%s", URLEncoder.encode(title, "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e.getMessage());
      }
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

  public User getAuthor() {
    return author;
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

  public int getStat3() {
    return stat3;
  }

  public int getStat4() {
    return stat4;
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
        return String.format("Комментарии к вики статье: %s", title.substring(9));
      } else {
        return String.format("Вики статья: %s", title);
      }
    }
  }

  public int getPages() {
    return pages;
  }

  public User getLastCommentBy() {
    return lastCommentBy;
  }

  public boolean isResolved() {
    return resolved;
  }

  public int getSection() {
    return section;
  }

  public String getGroupUrlName() {
    return groupUrlName;
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
}
