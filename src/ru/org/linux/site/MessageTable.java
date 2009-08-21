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

package ru.org.linux.site;

import java.io.IOException;
import java.sql.*;
import java.text.DateFormat;
import java.util.Date;

import org.javabb.bbcode.BBCodeProcessor;

import ru.org.linux.util.BadImageException;
import ru.org.linux.util.HTMLFormatter;
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
	  if (limit<1 || offset<0) {
    	pst = db.prepareStatement(
          "SELECT sections.name as ptitle, groups.title as gtitle, topics.title, " +
              "topics.id as topicid, comments.id as msgid, comments.postdate " +
              "FROM sections, groups, topics, comments, users " +
              "WHERE sections.id=groups.section AND groups.id=topics.groupid " +
              "AND comments.topic=topics.id AND comments.userid=users.id " +
              "AND users.nick=? AND NOT comments.deleted ORDER BY postdate DESC LIMIT 50"
		);
	  } else {
    	pst = db.prepareStatement(
          "SELECT sections.name as ptitle, groups.title as gtitle, topics.title, " +
              "topics.id as topicid, comments.id as msgid, comments.postdate " +
              "FROM sections, groups, topics, comments, users " +
              "WHERE sections.id=groups.section AND groups.id=topics.groupid " +
              "AND comments.topic=topics.id AND comments.userid=users.id " +
              "AND users.nick=? AND NOT comments.deleted ORDER BY postdate DESC LIMIT "+limit+" OFFSET "+offset
		);
	  }
      pst.setString(1, user.getNick());
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

  public static String getSectionRss(Connection db, int sectionid, int groupid, String htmlPath, String fullUrl) throws SQLException, ScriptErrorException {
    StringBuilder out = new StringBuilder();
    DateFormat rfc822 = DateFormats.createRFC822();

    Section section = new Section(db, sectionid);
    Group group = null;
    if (groupid!=0) {
      group = new Group(db, groupid);
      if (group.getSectionId()!=sectionid) {
        throw new BadGroupException("группа #"+groupid+" не принадлежит разделу #"+sectionid);
      }
    }

    out.append("<title>Linux.org.ru: ").append(section.getName());
    if (group!=null) {
      out.append(" - ").append(group.getTitle());
    }
    out.append("</title>");
    out.append("<pubDate>").append(rfc822.format(new Date())).append("</pubDate>");
    out.append("<description>Linux.org.ru: ").append(section.getName());
    if (group!=null) {
      out.append(" - ").append(group.getTitle());
    }
    out.append("</description>\n");

    Statement st=db.createStatement();

    ResultSet rs = st.executeQuery(
        "SELECT topics.title as subj, topics.lastmod, topics.stat1, postdate, userid, image, " +
            "groups.title as gtitle, topics.id as msgid, sections.vote, " +
            "groups.id as guid, topics.url, topics.linktext, imagepost, " +
            "postdate<(CURRENT_TIMESTAMP-expire) as expired, message, bbcode, commitdate " +
            "FROM topics,groups, sections, msgbase " +
            "WHERE sections.id=groups.section AND topics.id=msgbase.id " +
            "AND sections.id=" + sectionid + " AND (topics.moderate OR NOT sections.moderate) " +
            "AND topics.groupid=groups.id AND NOT deleted " +
            "AND postdate>(CURRENT_TIMESTAMP-'3 month'::interval) " +
            (group!=null?" AND groupid="+group.getId():"")+
            "ORDER BY commitdate DESC, postdate DESC LIMIT 10"
    );

    while (rs.next()) {
      int msgid = rs.getInt("msgid");
      boolean vote = rs.getBoolean("vote");
      String linktext = rs.getString("linktext");
      String url = rs.getString("url");
      String subj = rs.getString("subj");

      int userid = rs.getInt("userid");
      User user = User.getUserCached(db, userid);

      out.append("<item>");
      out.append("  <author>").append(user.getNick()).append("</author>\n");
      out.append("  <link>http://www.linux.org.ru/view-message.jsp?msgid=").append(msgid).append("</link>\n");
      out.append("  <guid>http://www.linux.org.ru/view-message.jsp?msgid=").append(msgid).append("</guid>\n");
      out.append("  <title>").append(HTMLFormatter.htmlSpecialChars(subj)).append("</title>\n");
      Timestamp postdate = rs.getTimestamp("postdate");
      Timestamp commitdate = rs.getTimestamp("commitdate");
      if (commitdate!=null) {
        out.append("  <pubDate>").append(rfc822.format(commitdate)).append("</pubDate>\n");
      } else {
        out.append("  <pubDate>").append(rfc822.format(postdate)).append("</pubDate>\n");
      }

      out.append("  <description>\n" + '\t');

      if (section.isImagepost()) {
        try {
          ImageInfo iconInfo = new ImageInfo(htmlPath + linktext);
          ImageInfo info = new ImageInfo(htmlPath + url);

          String message = rs.getString("message");
          boolean bbcode = rs.getBoolean("bbcode");
          if (bbcode) {
            BBCodeProcessor proc = new BBCodeProcessor();
            out.append(HTMLFormatter.htmlSpecialChars(proc.preparePostText(db, message)));
          } else {
            out.append(HTMLFormatter.htmlSpecialChars(message));
          }
          out.append(HTMLFormatter.htmlSpecialChars("<p><img src=\""+fullUrl+linktext+"\" ALT=\""+subj+"\" "+iconInfo.getCode()+" >"));
          out.append(HTMLFormatter.htmlSpecialChars("<p><i>"+info.getWidth()+'x'+info.getHeight()+", "+info.getSizeString()+"</i>"));
        } catch (BadImageException e) {
          // TODO write to log
        } catch (IOException e) {
          // TODO write to log
        }
      } else if (vote) {
        Poll poll = Poll.getPollByTopic(db, msgid);
        out.append(HTMLFormatter.htmlSpecialChars(poll.renderPoll(db, fullUrl))).append('\n');
      } else {
        String message = rs.getString("message");
        boolean bbcode = rs.getBoolean("bbcode");
        if (bbcode) {
          BBCodeProcessor proc = new BBCodeProcessor();
          out.append(HTMLFormatter.htmlSpecialChars(proc.preparePostText(db, message)));
        } else {
          out.append(HTMLFormatter.htmlSpecialChars(message));
        }
      }

      out.append("</description>\n");

      out.append("</item>");
    }

    return out.toString();
  }

  public static String getTopicRss(Connection db, String htmlPath, String fullUrl, Message topic) throws SQLException, PollNotFoundException, IOException, BadImageException {
    StringBuilder buf = new StringBuilder();

    if (topic.getSection().isImagepost()) {
      ImageInfo iconInfo = new ImageInfo(htmlPath + topic.getLinktext());
      ImageInfo info = new ImageInfo(htmlPath + topic.getUrl());

      buf.append(HTMLFormatter.htmlSpecialChars(topic.getProcessedMessage(db)));
      buf.append(HTMLFormatter.htmlSpecialChars("<p><img src=\"" + fullUrl + topic.getLinktext() + "\" ALT=\"" + topic.getTitle() + "\" " + iconInfo.getCode() + " >"));
      buf.append(HTMLFormatter.htmlSpecialChars("<p><i>" + info.getWidth() + 'x' + info.getHeight() + ", " + info.getSizeString() + "</i>"));
    } else if (topic.isVotePoll()) {
      Poll poll = Poll.getPollByTopic(db, topic.getId());
      buf.append(HTMLFormatter.htmlSpecialChars(poll.renderPoll(db, fullUrl)));
    } else {
      buf.append(HTMLFormatter.htmlSpecialChars(topic.getProcessedMessage(db)));
    }

    return buf.toString();
  }
}
