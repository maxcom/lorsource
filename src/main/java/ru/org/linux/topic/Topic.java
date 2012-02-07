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

package ru.org.linux.topic;

import com.google.common.base.Strings;
import ru.org.linux.group.Group;
import ru.org.linux.section.Section;
import ru.org.linux.user.User;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.URLUtil;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class Topic implements Serializable {
  private final int msgid;
  private final int postscore;
  private final boolean sticky;
  private final String linktext;
  private final String url;
  private final String title;
  private final int userid;
  private final int guid;
  private final boolean deleted;
  private final boolean expired;
  private final int commitby;
  private final boolean havelink;
  private final Timestamp postdate;
  private final Timestamp commitDate;
  private final String groupUrl;
  private final Timestamp lastModified;
  private final int sectionid;
  private final int commentCount;
  private final boolean moderate;
  private final boolean notop;
  private final int userAgent;
  private final String postIP;
  private final boolean resolved;
  private final int groupCommentsRestriction;
  private final int sectionCommentsRestriction;
  private final boolean minor;

  private static final long serialVersionUID = 807240555706110851L;
  private static final String UTF8 = "UTF-8";

  public Topic(int msgId,
               int postScore,
               boolean sticky,
               String linkText,
               String url,
               String title,
               int userId,
               int groupId,
               boolean deleted,
               boolean expired,
               int commitBy,
               boolean haveLink,
               Timestamp postdate,
               Timestamp commitDate,
               String groupUrl,
               Timestamp lastModified,
               int sectionId,
               int commentCount,
               boolean moderate,
               boolean noTop,
               int userAgent,
               String postIP,
               boolean resolved,
               int groupCommentsRestriction,
               int sectionCommentsRestriction,
               boolean minor
  ) {
    this.msgid = msgId;
    this.postscore = postScore;

    this.sticky= sticky;
    this.linktext = linkText;
    this.url = url;
    this.title = title;
    this.userid = userId;
    this.guid = groupId;
    this.deleted = deleted;
    this.expired = expired;
    this.commitby = commitBy;
    this.havelink = haveLink;
    this.postdate = postdate;
    this.commitDate = commitDate;
    this.groupUrl = groupUrl;
    this.lastModified = lastModified;
    this.sectionid = sectionId;
    this.commentCount = commentCount;
    this.moderate = moderate;
    this.notop = noTop;
    this.userAgent = userAgent;
    this.postIP = postIP;
    this.resolved = resolved;
    this.groupCommentsRestriction = groupCommentsRestriction;
    this.sectionCommentsRestriction = sectionCommentsRestriction;
    this.minor = minor;
  }

  public Topic(ResultSet rs) throws SQLException {
    this(
      rs.getInt("msgid"),
      (rs.getObject("postscore") == null)
        ? TopicPermissionService.POSTSCORE_UNRESTRICTED
        : rs.getInt("postscore"),
      rs.getBoolean("sticky"),
      rs.getString("linktext"),
      rs.getString("url"),
      StringUtil.makeTitle(rs.getString("title")),
      rs.getInt("userid"),
      rs.getInt("guid"),
      rs.getBoolean("deleted"),
      !rs.getBoolean("sticky") && rs.getBoolean("expired"),
      rs.getInt("commitby"),
      rs.getBoolean("havelink"),
      rs.getTimestamp("postdate"),
      rs.getTimestamp("commitdate"),
      rs.getString("urlname"),
      rs.getTimestamp("lastmod"),
      rs.getInt("section"),
      rs.getInt("stat1"),
      rs.getBoolean("moderate"),
      rs.getBoolean("notop"),
      rs.getInt("ua_id"),
      rs.getString("postip"),
      rs.getBoolean("resolved"),
      rs.getInt("restrict_comments"),
      Section.getCommentPostscore(rs.getInt("section")),
      rs.getBoolean("minor")
    );
  }

  public Topic(AddTopicRequest form, User user, String postIP) {
    userAgent = 0;
    this.postIP = postIP;

    guid = form.getGroup().getId();

    Group group = form.getGroup();

    groupCommentsRestriction = group.getCommentsRestriction();

    if (form.getLinktext()!=null) {
      linktext = StringUtil.escapeHtml(form.getLinktext());
    } else {
      linktext = null;
    }

    // url check
    if (!Strings.isNullOrEmpty(form.getUrl())) {
      url = URLUtil.fixURL(form.getUrl());
    } else {
      url = null;
    }

    // Setting Message fields
    if (form.getTitle()!=null) {
      title = StringUtil.escapeHtml(form.getTitle());
    } else {
      title = null;
    }

    havelink = form.getUrl() != null && form.getLinktext() != null && !form.getUrl().isEmpty() && !form.getLinktext().isEmpty() && !group.isImagePostAllowed();
    sectionid = group.getSectionId();
    // Defaults
    msgid = 0;
    postscore = 0;
    sticky = false;
    deleted = false;
    expired = false;
    commitby = 0;
    postdate = new Timestamp(System.currentTimeMillis());
    commitDate = null;
    groupUrl = "";
    lastModified = new Timestamp(System.currentTimeMillis());
    commentCount = 0;
    moderate = false;
    notop = false;
    userid = user.getId();
    resolved = false;
    minor = false;

    sectionCommentsRestriction = Section.getCommentPostscore(sectionid);
  }

  public Topic(Group group, Topic original, EditTopicRequest form) {
    userAgent = original.userAgent;
    postIP = original.postIP;
    guid = original.guid;

    groupCommentsRestriction = group.getCommentsRestriction();

    if (form.getLinktext() != null && original.havelink) {
      linktext = form.getLinktext();
    } else {
      linktext = original.linktext;
    }

    if (form.getUrl() != null && original.havelink) {
      url = URLUtil.fixURL(form.getUrl());
    } else {
      url = original.url;
    }

    if (form.getTitle() != null) {
      title = StringUtil.escapeHtml(form.getTitle());
    } else {
      title = original.title;
    }

    resolved = original.resolved;

    havelink = original.havelink;

    sectionid = group.getSectionId();

    msgid = original.msgid;
    postscore = original.getPostScore();
    sticky = original.sticky;
    deleted = original.deleted;
    expired = original.expired;
    commitby = original.commitby;
    postdate = original.postdate;
    commitDate = original.commitDate;
    groupUrl = original.groupUrl;
    lastModified = new Timestamp(System.currentTimeMillis());
    commentCount = original.commentCount;
    moderate = original.moderate;
    notop = original.notop;
    userid = original.userid;

    if (form.getMinor()!=null && sectionid==Section.SECTION_NEWS) {
      minor = form.getMinor();
    } else {
      minor = original.minor;
    }

    sectionCommentsRestriction = Section.getCommentPostscore(sectionid);
  }

  public boolean isExpired() {
    return expired;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public String getTitle() {
    return title;
  }

  public Timestamp getLastModified() {
    if (lastModified == null) {
      return new Timestamp(0);
    }

    return lastModified;
  }

  public int getGroupId() {
    return guid;
  }

  public int getSectionId() {
    return sectionid;
  }

  public int getCommentCount() {
    return commentCount;
  }

  private int getCommentCountRestriction() {
    int commentCountPS = TopicPermissionService.POSTSCORE_UNRESTRICTED;

    if (!sticky) {
      if (commentCount > 3000) {
        commentCountPS = 200;
      } else if (commentCount > 2000) {
        commentCountPS = 100;
      } else if (commentCount > 1000) {
        commentCountPS = 50;
      }
    }

    return commentCountPS;
  }

  public int getPostScore() {
    int effective = Math.max(postscore, groupCommentsRestriction);

    effective = Math.max(effective, sectionCommentsRestriction);

    effective = Math.max(effective, getCommentCountRestriction());

    return effective;
  }

  public String getPostScoreInfo() {
    return TopicPermissionService.getPostScoreInfo(getPostScore());
  }

  public int getMessageId() {
    return msgid;
  }

  public boolean isCommited() {
    return moderate;
  }

  public int getPageCount(int messages) {
    return (int) Math.ceil(commentCount / ((double) messages));
  }

  public static int getPageCount(int commentCount, int messages) {
    return (int) Math.ceil(commentCount / ((double) messages));
  }

  public boolean isSticky() {
    return sticky;
  }

  public String getUrl() {
    return url;
  }

  public String getLinktext() {
    return linktext;
  }

  public int getUid() {
    return userid;
  }

  public boolean isNotop() {
    return notop;
  }

  public String getLinkLastmod() {
    if (expired) {
      return getLink();
    } else {
      return getLink() + "?lastmod=" + getLastModified().getTime();
    }
  }

  public boolean isHaveLink() {
    return havelink;
  }

  public int getId() {
    return msgid;
  }

  public int getUserAgent() {
    return userAgent;
  }

  public Timestamp getPostdate() {
    return postdate;
  }

  public String getPostIP() {
    return postIP;
  }

  public int getCommitby() {
    return commitby;
  }

  public Timestamp getCommitDate() {
    return commitDate;
  }

  public boolean isResolved() {
    return resolved;
  }

  public String getLink() {
    try {
      return Section.getSectionLink(sectionid) + URLEncoder.encode(groupUrl, UTF8) + '/' + msgid;
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public String getLinkPage(int page) {
    if (page == 0) {
      return getLink();
    }

    try {
      return Section.getSectionLink(sectionid) + URLEncoder.encode(groupUrl, UTF8) + '/' + msgid + "/page" + page;
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean isMinor() {
    return minor;
  }

  public String getGroupUrl() {
    return groupUrl;
  }
}
