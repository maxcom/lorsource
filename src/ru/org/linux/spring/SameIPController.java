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

import java.sql.*;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import ru.org.linux.site.*;
import ru.org.linux.util.ServletParameterParser;
import ru.org.linux.util.StringUtil;

import com.google.common.collect.ImmutableList;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class SameIPController {
  @RequestMapping("/sameip.jsp")
  public ModelAndView sameIP(HttpServletRequest request) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      int userAgentId = 0;
      String ip;
      if (request.getParameter("msgid") != null) {
        Statement ipst = db.createStatement();
        int msgid = new ServletParameterParser(request).getInt("msgid");

        ResultSet rs = ipst.executeQuery("SELECT postip, ua_id FROM topics WHERE id=" + msgid);

        if (!rs.next()) {
          rs.close();
          rs = ipst.executeQuery("SELECT postip, ua_id FROM comments WHERE id=" + msgid);
          if (!rs.next()) {
            throw new MessageNotFoundException(msgid);
          }
        }

        ip = rs.getString("postip");
        userAgentId = rs.getInt("ua_id");

        if (ip == null) {
          throw new ScriptErrorException("No IP data for #" + msgid);
        }

        rs.close();
        ipst.close();
      } else {
        ip = new ServletParameterParser(request).getIP("ip");
      }


      ModelAndView mv = new ModelAndView("sameip");

      mv.getModel().put("ip", ip);
      mv.getModel().put("uaId", userAgentId);

      mv.getModel().put("blockInfo", IPBlockInfo.getBlockInfo(db, ip));

      mv.getModel().put("topics", getTopics(db, ip));
      mv.getModel().put("comments", getComments(db, ip));

      return mv;
    } finally {
      JdbcUtils.closeConnection(db);
    }
  }

  private static List<TopicItem> getTopics(Connection db, String ip) throws SQLException {
    Statement st=db.createStatement();
    ResultSet rs=st.executeQuery(
      "SELECT sections.name as ptitle, groups.title as gtitle, topics.title as title, topics.id as msgid, postdate " +
        "FROM topics, groups, sections, users " +
        "WHERE topics.groupid=groups.id " +
        "AND sections.id=groups.section " +
        "AND users.id=topics.userid " +
        "AND topics.postip='"+ip+"' " +
        "AND postdate>CURRENT_TIMESTAMP-'3 days'::interval ORDER BY msgid DESC");

    ImmutableList.Builder<TopicItem> res = ImmutableList.builder();

    while (rs.next()) {
      res.add(new TopicItem(rs, false));
    }

    rs.close();

    return res.build();
  }

  private static List<TopicItem> getComments(Connection db, String ip) throws SQLException {
    Statement st=db.createStatement();
    ResultSet rs=st.executeQuery(
      "SELECT sections.name as ptitle, groups.title as gtitle, topics.title, topics.id as topicid, comments.id as msgid, comments.postdate " +
        "FROM sections, groups, topics, comments " +
        "WHERE sections.id=groups.section " +
        "AND groups.id=topics.groupid " +
        "AND comments.topic=topics.id " +
        "AND comments.postip='"+ip+"' " +
        "AND comments.postdate>CURRENT_TIMESTAMP-'24 hour'::interval " +
        "ORDER BY postdate DESC;");

    ImmutableList.Builder<TopicItem> res = ImmutableList.builder();

    while (rs.next()) {
      res.add(new TopicItem(rs, true));
    }

    rs.close();

    return res.build();
  }

  public static class TopicItem {
    private final String ptitle;
    private final String gtitle;
    private final int id;
    private final String title;
    private final Timestamp postdate;
    private final int topicId;

    private TopicItem(ResultSet rs, boolean isComment) throws SQLException {
      ptitle = rs.getString("ptitle");
      gtitle = rs.getString("gtitle");
      id = rs.getInt("msgid");
      title = StringUtil.makeTitle(rs.getString("title"));
      postdate = rs.getTimestamp("postdate");

      if (isComment) {
        topicId = rs.getInt("topicid");
      } else {
        topicId = 0;
      }
    }

    public String getPtitle() {
      return ptitle;
    }

    public String getGtitle() {
      return gtitle;
    }

    public int getId() {
      return id;
    }

    public String getTitle() {
      return title;
    }

    public Timestamp getPostdate() {
      return postdate;
    }

    public int getTopicId() {
      return topicId;
    }
  }
}
