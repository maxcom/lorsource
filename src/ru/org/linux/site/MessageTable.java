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

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;

import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ImageInfo;
import ru.org.linux.util.StringUtil;

public class MessageTable {
  public static final int RSS_MIN = 10;
  public static final int RSS_MAX = 30;
  public static final int RSS_DEFAULT = 20;

  private MessageTable() {
  }

  public static String showComments(Connection db, User user, int offset, int limit) throws SQLException {
    DateFormat dateFormat = DateFormats.createDefault();

    StringBuilder out = new StringBuilder();

    PreparedStatement pst=null;

    try {
      pst = db.prepareStatement(
        "SELECT sections.name as ptitle, groups.title as gtitle, topics.title, " +
          "topics.id as topicid, comments.id as msgid, comments.postdate " +
          "FROM sections, groups, topics, comments " +
          "WHERE sections.id=groups.section AND groups.id=topics.groupid " +
          "AND comments.topic=topics.id " +
          "AND comments.userid=? AND NOT comments.deleted ORDER BY postdate DESC LIMIT " + limit + " OFFSET " + offset
      );
      
      pst.setInt(1, user.getId());
      ResultSet rs = pst.executeQuery();

      while (rs.next()) {
        out.append("<tr><td>").append(rs.getString("ptitle")).append("</td>");
        out.append("<td>").append(rs.getString("gtitle")).append("</td>");
        out.append("<td><a href=\"jump-message.jsp?msgid=").append(rs.getInt("topicid")).append("&amp;cid=").append(rs.getInt("msgid")).append("\" rev=contents>").append(StringUtil.makeTitle(rs.getString("title"))).append("</a></td>");
        out.append("<td>").append(dateFormat.format(rs.getTimestamp("postdate"))).append("</td></tr>");
      }

      rs.close();
    } finally {
      if (pst != null) {
        pst.close();
      }
    }

    return out.toString();
  }

  public static String getTopicRss(Connection db, String htmlPath, String fullUrl, Message topic) throws SQLException, PollNotFoundException, IOException, BadImageException {
    StringBuilder buf = new StringBuilder();

    if (topic.getSection().isImagepost()) {
      ImageInfo iconInfo = new ImageInfo(htmlPath + topic.getLinktext());
      ImageInfo info = new ImageInfo(htmlPath + topic.getUrl());

      buf.append(topic.getProcessedMessage(db));
      buf.append("<p><img src=\"" + fullUrl + topic.getLinktext() + "\" ALT=\"" + topic.getTitle() + "\" " + iconInfo.getCode() + " >");
      buf.append("<p><i>" + info.getWidth() + 'x' + info.getHeight() + ", " + info.getSizeString() + "</i>");
    } else if (topic.isVotePoll()) {
      Poll poll = Poll.getPollByTopic(db, topic.getId());
      buf.append(poll.renderPoll(db, fullUrl));
    } else {
      buf.append(topic.getProcessedMessage(db));
    }

    return buf.toString();
  }
}
