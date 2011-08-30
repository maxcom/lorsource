package ru.org.linux.spring.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.site.Message;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.site.User;
import ru.org.linux.site.UserErrorException;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Операции над сообщениями
 */

@Repository
public class MessageDao {
  /**
   * Запрос получения полной информации о топике
   */
  private final static String queryMessage = "SELECT " +
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
  /**
   * Удаление топика
   */
  private final static String updateDeleteMessage = "UPDATE topics SET deleted='t',sticky='f' WHERE id=?";
  /**
   * Обновление информации о удалении
   */
  private final static String updateDeleteInfo = "INSERT INTO del_info (msgid, delby, reason, deldate) values(?,?,?, CURRENT_TIMESTAMP)";

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setJdbcTemplate(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Autowired
  UserDao userDao;

  /**
   * Получить сообщение по id
   * @param id id нужного сообщения
   * @return сообщение
   * @throws MessageNotFoundException при отсутствии сообщения
   */
  public Message getById(int id) throws MessageNotFoundException {
    Message message;
    try {
      message = jdbcTemplate.queryForObject(queryMessage, new RowMapper<Message>() {
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

  /**
   * Удаление топика и если удаляет модератор изменить автору score
   * @param message удаляемый топик
   * @param user удаляющий пользователь
   * @param reason прчина удаления
   * @param bonus дельта изменения score автора топика
   * @throws MessageNotFoundException генерируется если сообщение не существует
   * @throws UserErrorException генерируется если некорректная делта score
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void deleteWithBonus(Message message, User user, String reason, int bonus) throws MessageNotFoundException, UserErrorException {
    String finalReason = reason;
    jdbcTemplate.update(updateDeleteMessage, message.getId());
    if (user.canModerate() && bonus!=0 && user.getId()!=message.getUid()) {
      if (bonus>20 || bonus<0) {
        throw new UserErrorException("Некорректное значение bonus");
      }
      userDao.changeScore(message.getId(), -bonus);
      finalReason += " ("+bonus+ ')';
    }
    jdbcTemplate.update(updateDeleteInfo, message.getId(), user.getId(), finalReason);
  }
}
