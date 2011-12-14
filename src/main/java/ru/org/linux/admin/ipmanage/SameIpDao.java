/*
 * Copyright 1998-2011 Linux.org.ru
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
package ru.org.linux.admin.ipmanage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;
import ru.org.linux.message.MessageNotFoundException;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
class SameIpDao {

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  /**
   * Получить список пользователей, заходивших с указанного IP-адреса.
   *
   * @param ipAddress IP-адрес
   * @return Список пользователей
   */
  public List<SameIpDto.UserItem> getUsers(String ipAddress) {
    return jdbcTemplate.query(
      "SELECT MAX(c.postdate) AS lastdate, u.nick, c.ua_id, ua.name AS user_agent " +
        "FROM comments c LEFT JOIN user_agents ua ON c.ua_id = ua.id " +
        "JOIN users u ON c.userid = u.id " +
        "WHERE c.postip=?::inet " +
        "GROUP BY u.nick, c.ua_id, ua.name " +
        "ORDER BY MAX(c.postdate) DESC, u.nick, ua.name",
      new RowMapper<SameIpDto.UserItem>() {
        @Override
        public SameIpDto.UserItem mapRow(ResultSet rs, int rowNum) throws SQLException {
          return new SameIpDto.UserItem(rs);
        }
      },
      ipAddress
    );
  }

  /**
   * Получить список тем, написанных с указанного IP-адреса.
   *
   * @param ipAddress IP-адрес
   * @return список тем
   */
  public List<SameIpDto.TopicItem> getTopics(String ipAddress) {
    StringBuilder queryStr = new StringBuilder();

    queryStr
      .append("SELECT sections.name as ptitle, groups.title as gtitle, topics.title as title, ")
      .append("topics.id as topic_id, 0::int4 as comment_id, postdate, deleted ")
      .append("FROM topics, groups, sections, users ")
      .append("WHERE topics.groupid=groups.id ")
      .append("AND sections.id=groups.section ")
      .append("AND users.id=topics.userid ")
      .append("AND topics.postip=?::inet ")
      .append("AND postdate>CURRENT_TIMESTAMP-'3 days'::interval ORDER BY comment_id DESC");

    return jdbcTemplate.query(queryStr.toString(),
      new RowMapper<SameIpDto.TopicItem>() {
        @Override
        public SameIpDto.TopicItem mapRow(ResultSet rs, int rowNum) throws SQLException {
          return new SameIpDto.TopicItem(rs);
        }
      },
      ipAddress
    );
  }

  /**
   * Получить список комментариев, написанных с указанного IP-адреса.
   *
   * @param ipAddress IP-адрес
   * @return список комментариев
   */
  public List<SameIpDto.TopicItem> getComments(String ipAddress) {
    StringBuilder queryStr = new StringBuilder();

    queryStr
      .append("SELECT sections.name as ptitle, groups.title as gtitle, topics.title, ")
      .append("topics.id as topic_id, comments.id as comment_id, comments.postdate, comments.deleted ")
      .append("FROM sections, groups, topics, comments ")
      .append("WHERE sections.id=groups.section ")
      .append("AND groups.id=topics.groupid ")
      .append("AND comments.topic=topics.id ")
      .append("AND comments.postip=?::inet ")
      .append("AND comments.postdate>CURRENT_TIMESTAMP-'24 hour'::interval ")
      .append("ORDER BY postdate DESC");

    return jdbcTemplate.query(queryStr.toString(),
      new RowMapper<SameIpDto.TopicItem>() {
        @Override
        public SameIpDto.TopicItem mapRow(ResultSet rs, int rowNum) throws SQLException {
          return new SameIpDto.TopicItem(rs);
        }
      },
      ipAddress
    );
  }

  /**
   * получить информацию об IP-адресе и об идентификаторе userAgent по идентификатору сообщения.
   *
   * @param msgId идентификатор сообщения
   * @return объект SameIp.IpInfo, содержащий информацию об IP-адресе и об идентификаторе userAgent
   * @throws MessageNotFoundException если сообщение не найдено.
   */
  public SameIp.IpInfo getIpInfo(Integer msgId)
    throws MessageNotFoundException {

    SameIp.IpInfo ipInfo = new SameIp.IpInfo();

    SqlRowSet rs = jdbcTemplate.queryForRowSet(
      "SELECT postip, ua_id FROM topics WHERE id=?",
      msgId
    );

    if (!rs.next()) {
      rs = jdbcTemplate.queryForRowSet("SELECT postip, ua_id FROM comments WHERE id=?", msgId);
      if (!rs.next()) {
        throw new MessageNotFoundException(msgId);
      }
    }
    ipInfo.setIpAddress(rs.getString("postip"));
    ipInfo.setUserAgentId(rs.getInt("ua_id"));

    return ipInfo;
  }

}
