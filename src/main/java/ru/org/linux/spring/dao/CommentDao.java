package ru.org.linux.spring.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.site.ScriptErrorException;
import ru.org.linux.site.User;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * Пока замена CommentDeleter в будущем должен содержать остальную часть доступа из Comments
 */

@Repository
public class CommentDao {
  private static final Log logger = LogFactory.getLog(CommentDao.class);

  private final static String replysForComment = "SELECT id FROM comments WHERE replyto=? AND NOT deleted FOR UPDATE";
  private final static String deleteComment = "UPDATE comments SET deleted='t' WHERE id=?";
  private final static String insertDelinfo = "INSERT INTO del_info (msgid, delby, reason, deldate) values(?,?,?, CURRENT_TIMESTAMP)";
  private final static String updateScore = "UPDATE users SET score=score+? WHERE id=(SELECT userid FROM comments WHERE id=?)";

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setJdbcTemplate(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }


  /**
   * Удаляем клментарий, если на комментарий есть ответы - генерируем исключение
   * @param msgid id удаляемого сообщения
   * @param reason причина удаления
   * @param user модератор который удаляет
   * @param scoreBonus кол-во отрезаемого шкворца
   * @throws ScriptErrorException генерируем исключение если на комментарий есть ответы
   */

  public void deleteComment(int msgid, String reason, User user, int scoreBonus) throws ScriptErrorException {
    if (!getReplys(msgid).isEmpty()) {
        throw new ScriptErrorException("Нельзя удалить комментарий с ответами");
    }
    doDeleteComment(msgid, reason, user, scoreBonus);
  }

  /**
   * Удаляем клментарий, если на комментарий есть ответы генерируем исключений SQL, используется в
   * массовом удалении коментариев @see UserDao
   * @param msgid id удаляемого сообщения
   * @param reason причина удаления
   * @param user модератор который удаляет
   * @param scoreBonus кол-во отрезаемого шкворца
   * @throws SQLException генерируем исключение если на комментарий есть ответы
   */
  public void deleteCommentWithSQLException(int msgid, String reason, User user, int scoreBonus) throws SQLException {
    if (!getReplys(msgid).isEmpty()) {
        throw new SQLException("Нельзя удалить комментарий с ответами");
    }
    doDeleteComment(msgid, reason, user, scoreBonus);
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  private void doDeleteComment(int msgid, String reason, User user, int scoreBonus) {
    jdbcTemplate.update(deleteComment, msgid);
    jdbcTemplate.update(insertDelinfo, msgid, user.getId(), reason+" ("+scoreBonus+')');
    jdbcTemplate.update(updateScore, scoreBonus, msgid);
    logger.info("Удалено сообщение " + msgid + " пользователем " + user.getNick() + " по причине `" + reason + '\'');
  }

  public List<Integer> deleteReplys(int msgid, User user, boolean score)  {
    return deleteReplys(msgid, user, score, 0);
  }

  private List<Integer> deleteReplys(int msgid, User user, boolean score, int depth) {
    List<Integer> replys = getReplys(msgid);
    List<Integer> deleted = new LinkedList<Integer>();
    for (Integer r : replys) {
      deleted.addAll(deleteReplys(r, user, score, depth+1));
      deleted.add(r);
      switch (depth) {
        case 0:
          if (score) {
            doDeleteComment(r, "7.1 Ответ на некорректное сообщение (авто, уровень 0)", user, -2);
          } else {
            doDeleteComment(r, "7.1 Ответ на некорректное сообщение (авто)", user, 0);
          }
          break;
        case 1:
          if (score) {
            doDeleteComment(r, "7.1 Ответ на некорректное сообщение (авто, уровень 1)", user, -1);
          } else {
            doDeleteComment(r, "7.1 Ответ на некорректное сообщение (авто)", user, 0);
          }
          break;
        default:
          doDeleteComment(r, "7.1 Ответ на некорректное сообщение (авто, уровень >1)", user, 0);
          break;
      }
    }

    return deleted;
  }

  public List<Integer> getReplys(int msgid){
    return jdbcTemplate.query(replysForComment, new RowMapper<Integer>() {
      @Override
      public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getInt(1);
      }
    },msgid);
  }

  /**
   * Массивное удаление всех комментариев пользователя, чо всеми ответами на них
   * @param user пользователь для экзекуции
   * @param moderator экзекутор-модератор
   * @return список удаленных комментариев
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public List<Integer> deleteAllComments(final User user, final User moderator) {
    final List<Integer> deleted = new LinkedList<Integer>();

    // Удаляем все топики
    jdbcTemplate.query("SELECT id FROM topics WHERE userid=? AND not deleted FOR UPDATE",
        new RowCallbackHandler(){
          @Override
          public void processRow(ResultSet rs) throws SQLException {
            int mid = rs.getInt("id");
            jdbcTemplate.update("UPDATE topics SET deleted='t',sticky='f' WHERE id=?", mid);
            jdbcTemplate.update("INSERT INTO del_info (msgid, delby, reason, deldate) values(?,?,?, CURRENT_TIMESTAMP)",
                mid, moderator.getId(), "Блокировка пользователя с удалением сообщений");
          }
        },
        user.getId());

    // Удаляем все комментарии
    jdbcTemplate.query("SELECT id FROM comments WHERE userid=? AND not deleted ORDER BY id DESC FOR update",
        new RowCallbackHandler() {
          @Override
          public void processRow(ResultSet resultSet) throws SQLException {
            int msgid = resultSet.getInt("id");
            deleted.add(msgid);
            deleted.addAll(deleteReplys(msgid, moderator, false));
            deleteCommentWithSQLException(msgid, "Блокировка пользователя с удалением сообщений", moderator, 0);
          }
        },
        user.getId());

    return deleted;
  }

}
