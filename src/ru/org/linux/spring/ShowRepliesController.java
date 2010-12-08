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

package ru.org.linux.spring;

import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ru.org.linux.site.*;
import ru.org.linux.util.StringUtil;

import org.javabb.bbcode.BBCodeProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ShowRepliesController {
  @Autowired
  private ReplyFeedView feedView;

  @RequestMapping("/show-replies.jsp")
  public ModelAndView showReplies(
    HttpServletRequest request,
    HttpServletResponse response,
    @RequestParam("nick") String nick,
    @RequestParam(value = "offset", defaultValue = "0") int offset
  ) throws Exception {
    User.checkNick(nick);

    Map<String, Object> params = new HashMap<String, Object>();
    params.put("nick", nick);
    Template tmpl = Template.getTemplate(request);

    boolean feedRequested = request.getParameterMap().containsKey("output");

    if (offset < 0) {
      offset = 0;
    }

    boolean firstPage = offset == 0;
    int topics = tmpl.getProf().getInt("topics");
    if (feedRequested) {
      topics = 50;
    }

    if (topics > 200) {
      topics = 200;
    }

    params.put("firstPage", firstPage);
    params.put("topics", topics);
    params.put("offset", offset);

    /* define timestamps for caching */
    long time = System.currentTimeMillis();
    int delay = firstPage ? 90 : 60 * 60;
    response.setDateHeader("Expires", time + 1000 * delay);

    List<MyTopicsListItem> list;

    Connection db = null;
    PreparedStatement pst = null;

    try {
      db = LorDataSource.getConnection();
      tmpl.initCurrentUser(db);

      User user = User.getUser(db, nick);

      list = new ArrayList<MyTopicsListItem>();

      boolean showPrivate = tmpl.isModeratorSession();

      User currentUser = tmpl.getCurrentUser();

      if (currentUser != null && currentUser.getId() == user.getId()) {
        showPrivate = true;
      }

      pst = db.prepareStatement(
        "SELECT event_date, " +
          " topics.title as subj, sections.name, groups.title as gtitle, " +
          " lastmod, topics.id as msgid, " +
          " comments.id AS cid, " +
          " comments.postdate AS cDate, " +
          " comments.userid AS cAuthor, " +
          " msgbase.message AS cMessage, bbcode, " +
          " urlname, sections.id as section, comments.deleted," +
          " type, user_events.message as ev_msg" +
          " FROM user_events INNER JOIN topics ON (topics.id = message_id)" +
          " INNER JOIN groups ON (groups.id = topics.groupid) " +
          " INNER JOIN sections ON (sections.id = groups.section) " +
          " LEFT JOIN comments ON (comments.id=comment_id) " +
          " LEFT JOIN msgbase ON (msgbase.id = comments.id)" +
          " WHERE user_events.userid = ? " +
          (showPrivate ? "" : " AND NOT private ") +
          " AND (comments.id is null or NOT comments.topic_deleted)" +
          " ORDER BY event_date DESC LIMIT " + topics +
          " OFFSET " + offset
      );

      pst.setInt(1, user.getId());
      ResultSet rs = pst.executeQuery();

      while (rs.next()) {
        list.add(new MyTopicsListItem(db, rs, feedRequested));
      }

      rs.close();

      if ("POST".equalsIgnoreCase(request.getMethod())) {
        user.resetUnreadEvents(db);
        tmpl.updateCurrentUser(db);
      }
    } finally {
      JdbcUtils.closeStatement(pst);
      JdbcUtils.closeConnection(db);
    }

    params.put("topicsList", list);
    params.put("hasMore", list.size()==topics);

    ModelAndView result = new ModelAndView("show-replies", params);

    if (feedRequested) {
      result.addObject("feed-type", "rss");
      if ("atom".equals(request.getParameter("output"))) {
        result.addObject("feed-type", "atom");
      }
      result.setView(feedView);
    }
    return result;
  }

  public enum EventType {
    REPLY, DEL, WATCH, OTHER
  }

  public static class MyTopicsListItem implements Serializable {
    private final int cid;
    private final User cAuthor;
    private final Timestamp cDate;
    private final String messageText;
    private final String groupTitle;
    private final String groupUrlName;
    private final String sectionTitle;
    private final int sectionId;
    private static final long serialVersionUID = -8433869244309809050L;
    private final String subj;
    private final Timestamp lastmod;
    private final int msgid;
    private final EventType type;
    private final String eventMessage;
    private final Timestamp eventDate;

    public MyTopicsListItem(Connection db, ResultSet rs, boolean readMessage) throws SQLException {
      subj = StringUtil.makeTitle(rs.getString("subj"));

      Timestamp lastmod = rs.getTimestamp("lastmod");
      if (lastmod == null) {
        this.lastmod = new Timestamp(0);
      } else {
        this.lastmod = lastmod;
      }

      eventDate = rs.getTimestamp("event_date");

      cid = rs.getInt("cid");
      if (!rs.wasNull()) {
        try {
          cAuthor = User.getUserCached(db, rs.getInt("cAuthor"));
        } catch (UserNotFoundException e) {
          throw new RuntimeException(e);
        }

        cDate = rs.getTimestamp("cDate");
      } else {
        cDate = null;
        cAuthor = null;
      }

      groupTitle = rs.getString("gtitle");
      groupUrlName = rs.getString("urlname");
      sectionTitle = rs.getString("name");
      sectionId = rs.getInt("section");
      msgid = rs.getInt("msgid");
      type = EventType.valueOf(rs.getString("type"));
      eventMessage = rs.getString("ev_msg");

      if (readMessage) {
        String text = rs.getString("cMessage");
        if (rs.getBoolean("bbcode")) {
          messageText = new BBCodeProcessor().preparePostText(db, text);
        } else {
          messageText = text;
        }
      } else {
        messageText = null;
      }
    }

    public int getCid() {
      return cid;
    }

    public User getCommentAuthor() {
      return cAuthor;
    }

    public Timestamp getCommentDate() {
      return cDate;
    }

    public String getMessageText() {
      return messageText;
    }

    public String getNick() {
      return cAuthor.getNick();
    }

    public String getGroupTitle() {
      return groupTitle;
    }

    public String getSectionTitle() {
      return sectionTitle;
    }

    public String getGroupUrl() {
      return Section.getSectionLink(sectionId) + groupUrlName + '/';
    }

    public String getSectionUrl() {
      return Section.getSectionLink(sectionId);
    }

    public String getSubj() {
      return subj;
    }

    public Timestamp getLastmod() {
      return lastmod;
    }

    public int getMsgid() {
      return msgid;
    }

    public EventType getType() {
      return type;
    }

    public String getEventMessage() {
      return eventMessage;
    }

    public Timestamp getEventDate() {
      return eventDate;
    }
  }
}
