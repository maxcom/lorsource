/*
 * Copyright 1998-2010 Linux.org.ru
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

package ru.org.linux.site;

import com.google.common.base.Strings;
import org.springframework.validation.Errors;
import ru.org.linux.dto.GroupDto;
import ru.org.linux.dto.SectionDto;
import ru.org.linux.dto.UserDto;
import ru.org.linux.spring.AddMessageRequest;
import ru.org.linux.spring.EditMessageRequest;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.URLUtil;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

public class Message implements Serializable {
  private final int msgid;
  private final int postscore;
  private final boolean votepoll;
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
  private final String groupTitle;
  private final String groupUrl;
  private final Timestamp lastModified;
  private final int sectionid;
  private final int commentCount;
  private final boolean moderate;
  private final String message;
  private final boolean notop;
  private final int userAgent;
  private final String postIP;
  private final boolean lorcode;
  private final boolean resolved;
  private final int groupCommentsRestriction;
  private final int sectionCommentsRestriction;
  private final boolean minor;

  private static final long serialVersionUID = 807240555706110851L;
  public static final int POSTSCORE_MOD_AUTHOR = 9999;
  public static final int POSTSCORE_UNRESTRICTED = -9999;
  public static final int POSTSCORE_MODERATORS_ONLY = 10000;
  public static final int POSTSCORE_REGISTERED_ONLY = -50;
  private static final String UTF8 = "UTF-8";

  public Message(ResultSet rs) throws SQLException {
    msgid = rs.getInt("msgid");

    int ps = rs.getInt("postscore");

    if (rs.wasNull()) {
      postscore = POSTSCORE_UNRESTRICTED;
    } else {
      postscore = ps;
    }

    votepoll = rs.getBoolean("vote");
    sticky = rs.getBoolean("sticky");
    linktext = rs.getString("linktext");
    url = rs.getString("url");
    userid = rs.getInt("userid");
    title = StringUtil.makeTitle(rs.getString("title"));
    guid = rs.getInt("guid");
    deleted = rs.getBoolean("deleted");
    expired = !sticky && rs.getBoolean("expired");
    havelink = rs.getBoolean("havelink");
    postdate = rs.getTimestamp("postdate");
    commitDate = rs.getTimestamp("commitdate");
    commitby = rs.getInt("commitby");
    groupTitle = rs.getString("gtitle");
    groupUrl = rs.getString("urlname");
    lastModified = rs.getTimestamp("lastmod");
    sectionid = rs.getInt("section");
    commentCount = rs.getInt("stat1");
    moderate = rs.getBoolean("moderate");
    message = rs.getString("message");
    notop = rs.getBoolean("notop");
    userAgent = rs.getInt("ua_id");
    postIP = rs.getString("postip");
    lorcode = rs.getBoolean("bbcode");
    resolved = rs.getBoolean("resolved");
    groupCommentsRestriction = rs.getInt("restrict_comments");
    minor = rs.getBoolean("minor");
    sectionCommentsRestriction = SectionDto.getCommentPostscore(sectionid);
  }

  public Message(AddMessageRequest form, UserDto user, String message, String postIP) {
    userAgent = 0;
    this.postIP = postIP;

    guid = form.getGroup().getId();

    GroupDto groupDto = form.getGroup();

    groupCommentsRestriction = groupDto.getCommentsRestriction();

    if (form.getLinktext() != null) {
      linktext = StringUtil.escapeHtml(form.getLinktext());
    } else {
      linktext = null;
    }

    // url check
    if (!groupDto.isImagePostAllowed() && !Strings.isNullOrEmpty(form.getUrl())) {
      url = URLUtil.fixURL(form.getUrl());
    } else {
      url = null;
    }

    // Setting Message fields
    if (form.getTitle() != null) {
      title = StringUtil.escapeHtml(form.getTitle());
    } else {
      title = null;
    }

    havelink = form.getUrl() != null && form.getLinktext() != null && !form.getUrl().isEmpty() && !form.getLinktext().isEmpty() && !groupDto.isImagePostAllowed();
    sectionid = groupDto.getSectionId();
    // Defaults
    msgid = 0;
    postscore = 0;
    votepoll = false;
    sticky = false;
    deleted = false;
    expired = false;
    commitby = 0;
    postdate = new Timestamp(System.currentTimeMillis());
    commitDate = null;
    groupTitle = "";
    groupUrl = "";
    lastModified = new Timestamp(System.currentTimeMillis());
    commentCount = 0;
    moderate = false;
    notop = false;
    userid = user.getId();
    lorcode = true;
    resolved = false;
    minor = false;

    this.message = message;
    sectionCommentsRestriction = SectionDto.getCommentPostscore(sectionid);
  }

  public Message(GroupDto groupDto, Message original, EditMessageRequest form) {
    userAgent = original.userAgent;
    postIP = original.postIP;
    guid = original.guid;

    groupCommentsRestriction = groupDto.getCommentsRestriction();

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

    sectionid = groupDto.getSectionId();

    msgid = original.msgid;
    postscore = original.getPostScore();
    votepoll = original.votepoll;
    sticky = original.sticky;
    deleted = original.deleted;
    expired = original.expired;
    commitby = original.commitby;
    postdate = original.postdate;
    commitDate = original.commitDate;
    groupTitle = original.groupTitle;
    groupUrl = original.groupUrl;
    lastModified = new Timestamp(System.currentTimeMillis());
    commentCount = original.commentCount;
    moderate = original.moderate;
    notop = original.notop;
    userid = original.userid;
    lorcode = original.lorcode;

    if (form.getMinor() != null && sectionid == SectionDto.SECTION_NEWS) {
      minor = form.getMinor();
    } else {
      minor = original.minor;
    }

    if (form.getMsg() != null) {
      message = form.getMsg();
    } else {
      message = original.message;
    }

    sectionCommentsRestriction = SectionDto.getCommentPostscore(sectionid);
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

  public String getGroupTitle() {
    return groupTitle;
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
    int commentCountPS = POSTSCORE_UNRESTRICTED;

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
    return getPostScoreInfo(getPostScore());
  }

  public static String getPostScoreInfo(int postscore) {
    switch (postscore) {
      case POSTSCORE_UNRESTRICTED:
        return "";
      case 100:
        return "<b>Ограничение на отправку комментариев</b>: " + UserDto.getStars(100, 100);
      case 200:
        return "<b>Ограничение на отправку комментариев</b>: " + UserDto.getStars(200, 200);
      case 300:
        return "<b>Ограничение на отправку комментариев</b>: " + UserDto.getStars(300, 300);
      case 400:
        return "<b>Ограничение на отправку комментариев</b>: " + UserDto.getStars(400, 400);
      case 500:
        return "<b>Ограничение на отправку комментариев</b>: " + UserDto.getStars(500, 500);
      case POSTSCORE_MOD_AUTHOR:
        return "<b>Ограничение на отправку комментариев</b>: только для модераторов и автора";
      case POSTSCORE_MODERATORS_ONLY:
        return "<b>Ограничение на отправку комментариев</b>: только для модераторов";
      case POSTSCORE_REGISTERED_ONLY:
        return "<b>Ограничение на отправку комментариев</b>: только для зарегистрированных пользователей";
      default:
        return "<b>Ограничение на отправку комментариев</b>: только для зарегистрированных пользователей, score>=" + postscore;
    }
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

  public boolean isVotePoll() {
    return votepoll;
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

  public boolean isCommentsAllowed(UserDto user) {
    if (user != null && user.isBlocked()) {
      return false;
    }

    if (deleted || expired) {
      return false;
    }

    int score = getPostScore();

    if (score == POSTSCORE_UNRESTRICTED) {
      return true;
    }

    if (user == null || user.isAnonymous()) {
      return false;
    }

    if (user.isModerator()) {
      return true;
    }

    if (score == POSTSCORE_REGISTERED_ONLY) {
      return true;
    }

    if (score == POSTSCORE_MODERATORS_ONLY) {
      return false;
    }

    boolean isAuthor = user.getId() == userid;

    if (score == POSTSCORE_MOD_AUTHOR) {
      return isAuthor;
    }

    if (isAuthor) {
      return true;
    } else {
      return user.getScore() >= score;
    }
  }

  public void checkCommentsAllowed(UserDto user, Errors errors) {
    if (deleted) {
      errors.reject(null, "Нельзя добавлять комментарии к удаленному сообщению");
      return;
    }

    if (expired) {
      errors.reject(null, "Сообщение уже устарело");
      return;
    }

    if (!isCommentsAllowed(user)) {
      errors.reject(null, "Вы не можете добавлять комментарии в эту тему");
    }
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

  public String getMessage() {
    return message;
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

  public boolean isLorcode() {
    return lorcode;
  }

  public boolean isResolved() {
    return resolved;
  }

  public String getLink() {
    try {
      return SectionDto.getSectionLink(sectionid) + URLEncoder.encode(groupUrl, UTF8) + '/' + msgid;
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public String getLinkPage(int page) {
    if (page == 0) {
      return getLink();
    }

    try {
      return SectionDto.getSectionLink(sectionid) + URLEncoder.encode(groupUrl, UTF8) + '/' + msgid + "/page" + page;
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean isMinor() {
    return minor;
  }

  /**
   * Проверка может ли пользователь удалить топик
   *
   * @param user пользователь удаляющий сообщение
   * @return признак возможности удаления
   */
  public boolean isDeletableByUser(UserDto user) {
    Calendar calendar = Calendar.getInstance();

    calendar.setTime(new Date());
    calendar.add(Calendar.HOUR_OF_DAY, -1);
    Timestamp hourDeltaTime = new Timestamp(calendar.getTimeInMillis());

    return (postdate.compareTo(hourDeltaTime) >= 0 && userid == user.getId());
  }

  /**
   * Проверка, может ли модератор удалить топик
   *
   * @param user       пользователь удаляющий сообщение
   * @param sectionDto местоположение топика
   * @return признак возможности удаления
   */
  public boolean isDeletableByModerator(UserDto user, SectionDto sectionDto) {
    // TODO убрать от сюда аргумент функции section
    if(!user.isModerator()) {
      return false;
    }
    Calendar calendar = Calendar.getInstance();

    calendar.setTime(new Date());
    calendar.add(Calendar.MONTH, -1);
    Timestamp monthDeltaTime = new Timestamp(calendar.getTimeInMillis());

    boolean ret = false;

    // Если раздел премодерируемый и топик не подтвержден удалять можно
    if (sectionDto.isPremoderated() && !moderate) {
      ret = true;
    }

    // Если раздел премодерируемый, топик подтвержден и прошло меньше месяца с подтверждения удалять можно
    if (sectionDto.isPremoderated() && moderate && postdate.compareTo(monthDeltaTime) >= 0) {
      ret = true;
    }

    // Если раздел не премодерируем, удалять можно
    if (!sectionDto.isPremoderated()) {
      ret = true;
    }

    return ret;
  }
}
