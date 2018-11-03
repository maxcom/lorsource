/*
 * Copyright 1998-2016 Linux.org.ru
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.auth.IPBlockDao;
import ru.org.linux.auth.IPBlockInfo;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.site.ScriptErrorException;
import ru.org.linux.site.Template;
import ru.org.linux.user.UserDao;
import ru.org.linux.util.ServletParameterParser;
import ru.org.linux.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Controller
public class SameIPController {
  private IPBlockDao ipBlockDao;
  private UserDao userDao;
  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setIpBlockDao(IPBlockDao ipBlockDao) {
    this.ipBlockDao = ipBlockDao;
  }

  @Autowired
  public void setUserDao(UserDao userDao) {
    this.userDao = userDao;
  }

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  @RequestMapping("/sameip.jsp")
  public ModelAndView sameIP(
    HttpServletRequest request,
    @RequestParam(required = false) Integer msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    String ip;

    ModelAndView mv = new ModelAndView("sameip");

    int userAgentId = 0;

    if (msgid != null) {
      SqlRowSet rs = jdbcTemplate.queryForRowSet(
              "SELECT postip, ua_id FROM topics WHERE id=?",
              msgid
      );

      if (!rs.next()) {
        rs = jdbcTemplate.queryForRowSet("SELECT postip, ua_id FROM comments WHERE id=?", msgid);
        if (!rs.next()) {
          throw new MessageNotFoundException(msgid);
        }
      }

      ip = rs.getString("postip");
      userAgentId = rs.getInt("ua_id");

      if (ip == null) {
        throw new ScriptErrorException("No IP data for #" + msgid);
      }
    } else {
      ip = ServletParameterParser.getIP(request, "ip");
    }

    mv.getModel().put("ip", ip);
    mv.getModel().put("uaId", userAgentId);

    mv.getModel().put("topics", getTopics(ip));
    mv.getModel().put("comments", getComments(ip));
    mv.getModel().put("users", getUsers(ip, userAgentId));

    IPBlockInfo blockInfo = ipBlockDao.getBlockInfo(ip);

    Boolean allowPosting = false;
    Boolean captchaRequired = true;
    if (blockInfo.isInitialized()) {
      mv.getModel().put("blockInfo", blockInfo);
      allowPosting = blockInfo.isAllowRegistredPosting();
      captchaRequired = blockInfo.isCaptchaRequired();
      mv.getModel().put("blockModerator", userDao.getUserCached(blockInfo.getModerator()));
    }
    mv.addObject("allowPosting", allowPosting);
    mv.addObject("captchaRequired", captchaRequired);

    return mv;
  }

  private List<TopicItem> getTopics(String ip) {
    return jdbcTemplate.query(
            "SELECT sections.name as ptitle, groups.title as gtitle, topics.title as title, topics.id as msgid, postdate, deleted " +
                    "FROM topics, groups, sections, users " +
                    "WHERE topics.groupid=groups.id " +
                    "AND sections.id=groups.section " +
                    "AND users.id=topics.userid " +
                    "AND topics.postip=?::inet " +
                    "AND postdate>CURRENT_TIMESTAMP-'3 days'::interval ORDER BY msgid DESC",
            new RowMapper<TopicItem>() {
              @Override
              public TopicItem mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new TopicItem(rs, false);
              }
            },
            ip
    );
  }

  private List<TopicItem> getComments(String ip) {
    return jdbcTemplate.query(
            "SELECT sections.name as ptitle, groups.title as gtitle, topics.title, topics.id as topicid, comments.id as msgid, comments.postdate, comments.deleted " +
                    "FROM sections, groups, topics, comments " +
                    "WHERE sections.id=groups.section " +
                    "AND groups.id=topics.groupid " +
                    "AND comments.topic=topics.id " +
                    "AND comments.postip=?::inet " +
                    "AND comments.postdate>CURRENT_TIMESTAMP-'3 days'::interval " +
                    "ORDER BY postdate DESC",
            new RowMapper<TopicItem>() {
              @Override
              public TopicItem mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new TopicItem(rs, true);
              }
            },
            ip
    );
  }

  private List<UserItem> getUsers(String ip, final int uaId) {
    return jdbcTemplate.query(
            "SELECT MAX(c.postdate) AS lastdate, u.nick, c.ua_id, ua.name AS user_agent " +
                    "FROM comments c LEFT JOIN user_agents ua ON c.ua_id = ua.id " +
                    "JOIN users u ON c.userid = u.id " +
                    "WHERE c.postip=?::inet " +
                    "GROUP BY u.nick, c.ua_id, ua.name " +
                    "ORDER BY MAX(c.postdate) DESC, u.nick, ua.name",
            new RowMapper<UserItem>() {
              @Override
              public UserItem mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new UserItem(rs, uaId);
              }
            },
            ip
    );
  }

  public static class TopicItem {
    private final String ptitle;
    private final String gtitle;
    private final int id;
    private final String title;
    private final Timestamp postdate;
    private final int topicId;
    private final boolean deleted;

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

      deleted = rs.getBoolean("deleted");
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

    public boolean isDeleted() {
      return deleted;
    }
  }

  public static class UserItem {
    private final Timestamp lastdate;
    private final String nick;
    private final boolean sameUa;
    private final String userAgent;

    private UserItem(ResultSet rs, int uaId) throws SQLException {
      lastdate = rs.getTimestamp("lastdate");
      nick = rs.getString("nick");
      sameUa = uaId == rs.getInt("ua_id");
      userAgent = rs.getString("user_agent");
    }

    public Timestamp getLastdate() {
      return lastdate;
    }

    public String getNick() {
      return nick;
    }

    public boolean isSameUa() {
      return sameUa;
    }

    public String getUserAgent() {
      return userAgent;
    }
  }
}
