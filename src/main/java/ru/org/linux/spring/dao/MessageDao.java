package ru.org.linux.spring.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.org.linux.site.Message;
import ru.org.linux.site.MessageNotFoundException;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Операции над сообщениями
 */

@Repository
public class MessageDao {
  private final String queryMessageById = "SELECT " +
        "postdate, topics.id as msgid, userid, topics.title, " +
        "topics.groupid as guid, topics.url, topics.linktext, ua_id, " +
        "groups.title as gtitle, urlname, vote, havelink, section, topics.sticky, topics.postip, " +
        "postdate<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby, " +
        "commitdate, topics.stat1, postscore, topics.moderate, message, notop,bbcode, " +
        "topics.resolved, restrict_comments, minor " +
        "FROM topics " +
        "INNER JOIN groups ON (groups.id=topics.groupid) " +
        "INNER JOIN sections ON (sections.id=groups.section) " +
        "INNER JOIN msgbase ON (msgbase.id=topics.id) " +
        "WHERE topics.id=?";

   private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setJdbcTemplate(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }


  /**
   * Получить сообщение по id
   * @param id id нужного сообщения
   * @return сообщение
   * @throws MessageNotFoundException при отсутствии сообщения
   */
  public Message getById(int id) throws MessageNotFoundException {
    Message message;
    try {
      message = jdbcTemplate.queryForObject(queryMessageById, new RowMapper<Message>() {
        @Override
        public Message mapRow(ResultSet resultSet, int i) throws SQLException {
          return new Message(resultSet);
        }
      }, id);
    } catch (EmptyResultDataAccessException exception) {
      throw new MessageNotFoundException(id);
    }
    return message;
  }
}
