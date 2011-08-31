package ru.org.linux.spring.dao;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.site.*;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

  private final static String queryEditInfo = "SELECT * FROM edit_info WHERE msgid=? ORDER BY id DESC";

  private final static String queryTags = "SELECT tags_values.value FROM tags, tags_values WHERE tags.msgid=? AND tags_values.id=tags.tagid ORDER BY value";

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
   * Получить информации о редактировании топика
   * @param id id топика
   * @return список изменений топика
   */
  public List<EditInfoDTO> getEditInfo(int id) {
    final List<EditInfoDTO> editInfoDTOs = new ArrayList<EditInfoDTO>();
    jdbcTemplate.query(queryEditInfo, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet resultSet) throws SQLException {
        EditInfoDTO editInfoDTO = new EditInfoDTO();
        editInfoDTO.setId(resultSet.getInt("id"));
        editInfoDTO.setMsgid(resultSet.getInt("msgid"));
        editInfoDTO.setEditor(resultSet.getInt("editor"));
        editInfoDTO.setOldmessage(resultSet.getString("oldmessage"));
        editInfoDTO.setEditdate(resultSet.getTimestamp("editdate"));
        editInfoDTO.setOldtitle(resultSet.getString("oldtitle"));
        editInfoDTO.setOldtags(resultSet.getString("oldtags"));
        editInfoDTOs.add(editInfoDTO);
      }
    }, id);
    return editInfoDTOs;
  }

  /**
   * Получить тэги топика
   * TODO возможно надо сделать TagDao ?
   * @param message топик
   * @return список тэгов
   */
  public ImmutableList<String> getTags(Message message) {
    final ImmutableList.Builder<String> tags = ImmutableList.builder();

    jdbcTemplate.query(queryTags, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet resultSet) throws SQLException {
        tags.add(resultSet.getString("value"));
      }
    }, message.getId());

    return tags.build();
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
