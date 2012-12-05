/*
 * Copyright 1998-2012 Linux.org.ru
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

package ru.org.linux.comment;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.spring.dao.DeleteInfoDao;
import ru.org.linux.user.*;
import ru.org.linux.util.StringUtil;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * Операции над комментариями
 */

@Repository
class CommentDaoImpl implements CommentDao {
  private static final Log logger = LogFactory.getLog(CommentDao.class);

  private static final String queryCommentById = "SELECT " +
    "postdate, topic, users.id as userid, comments.id as msgid, comments.title, " +
    "deleted, replyto, edit_count, edit_date, editors.nick as  edit_nick, " +
    "user_agents.name AS useragent, comments.postip " +
    "FROM comments " +
    "INNER JOIN users ON (users.id=comments.userid) " +
    "LEFT JOIN users as editors ON comments.editor_id=editors.id " +
    "LEFT JOIN user_agents ON (user_agents.id=comments.ua_id) " +
    "WHERE comments.id=?";

  /**
   * Запрос списка комментариев для топика ВКЛЮЧАЯ удаленные
   */
  private static final String queryCommentListByTopicId = "SELECT " +
    "comments.title, topic, postdate, userid, comments.id as msgid, " +
    "replyto, edit_count, edit_date, editors.nick as  edit_nick, deleted, " +
    "user_agents.name AS useragent, comments.postip " +
    "FROM comments " +
    "LEFT JOIN users as editors ON comments.editor_id=editors.id " +
    "LEFT JOIN user_agents ON (user_agents.id=comments.ua_id) " +
    "WHERE topic=? ORDER BY msgid ASC";

  /**
   * Запрос списка комментариев для топика ИСКЛЮЧАЯ удаленные
   */
  private static final String queryCommentListByTopicIdWithoutDeleted = "SELECT " +
    "comments.title, topic, postdate, userid, comments.id as msgid, " +
    "replyto, edit_count, edit_date, editors.nick as edit_nick, deleted, " +
    "user_agents.name AS useragent, comments.postip " +
    "FROM comments " +
    "LEFT JOIN users as editors ON comments.editor_id=editors.id " +
    "LEFT JOIN user_agents ON (user_agents.id=comments.ua_id) " +
    "WHERE topic=?  AND NOT deleted ORDER BY msgid ASC";

  private static final String replysForComment = "SELECT id FROM comments WHERE replyto=? AND NOT deleted FOR UPDATE";
  private static final String replysForCommentCount = "SELECT count(id) FROM comments WHERE replyto=? AND NOT deleted";
  private static final String deleteComment = "UPDATE comments SET deleted='t' WHERE id=? AND not deleted";
  private static final String updateScore = "UPDATE users SET score=score+? WHERE id=(SELECT userid FROM comments WHERE id=?)";

  private JdbcTemplate jdbcTemplate;
  private UserDao userDao;
  private DeleteInfoDao deleteInfoDao;

  private SimpleJdbcInsert insertMsgbase;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);

    insertMsgbase = new SimpleJdbcInsert(dataSource);
    insertMsgbase.setTableName("msgbase");
    insertMsgbase.usingColumns("id", "message", "bbcode");
  }

  @Autowired
  public void setUserDao(UserDao userDao) {
    this.userDao = userDao;
  }

  @Autowired
  public void setDeleteInfoDao(DeleteInfoDao deleteInfoDao) {
    this.deleteInfoDao = deleteInfoDao;
  }

  @Override
  public Comment getById(int id) throws MessageNotFoundException {
    Comment comment;
    try {
      comment = jdbcTemplate.queryForObject(queryCommentById, new RowMapper<Comment>() {
        @Override
        public Comment mapRow(ResultSet resultSet, int i) throws SQLException {
          return new Comment(resultSet, deleteInfoDao);
        }
      }, id);
    } catch (EmptyResultDataAccessException exception) {
      throw new MessageNotFoundException(id);
    }
    return comment;
  }

  @Override
  public List<Comment> getCommentList(int topicId, boolean showDeleted) {
    final List<Comment> comments = new ArrayList<Comment>();

    if (showDeleted) {
      jdbcTemplate.query(queryCommentListByTopicId, new RowCallbackHandler() {
        @Override
        public void processRow(ResultSet resultSet) throws SQLException {
          comments.add(new Comment(resultSet, deleteInfoDao));
        }
      }, topicId);
    } else {
      jdbcTemplate.query(queryCommentListByTopicIdWithoutDeleted, new RowCallbackHandler() {
        @Override
        public void processRow(ResultSet resultSet) throws SQLException {
          comments.add(new Comment(resultSet, deleteInfoDao));
        }
      }, topicId);
    }

    return comments;
  }

  /**
   * Удалить комментарий, не начиная транзакции. Обновить статистику, если было осуществлено удаление.
   *
   * @param msgid   идентификационнай номер комментария
   * @param reason  причина удаления
   * @param user    пользователь, удаляющий комментарий
   * @return true если комментарий был удалён, иначе false
   * @throws SQLException
   */
  private boolean deleteCommentWithoutTransaction(int msgid, String reason, User user) throws SQLException {
    if (getReplaysCount(msgid) != 0) {
      throw new SQLException("Нельзя удалить комментарий с ответами");
    }

    boolean deleted = deleteComment(msgid, reason, user, 0);

    if (deleted) {
      updateStatsAfterDelete(msgid, 1);
    }

    return deleted;
  }

  @Override
  public boolean deleteComment(int msgid, String reason, User user, int scoreBonus) {

    int inReplyId = jdbcTemplate.queryForInt("SELECT replyto FROM comments WHERE id=? AND NOT deleted", msgid);

    int deleteCount = jdbcTemplate.update(deleteComment, msgid);

    if (deleteCount > 0) {
      deleteInfoDao.insert(msgid, user, reason, scoreBonus);

      if (scoreBonus != 0) {
        jdbcTemplate.update(updateScore, scoreBonus, msgid);
      }

      logger.info("Удалено сообщение " + msgid + " пользователем " + user.getNick() + " по причине `" + reason + '\'');

      return true;
    } else {
      logger.info("Пропускаем удаление уже удаленного " + msgid);
      return false;
    }
  }

  @Override
  public void updateStatsAfterDelete(int commentId, int count) {
    int topicId = jdbcTemplate.queryForInt("SELECT topic FROM comments WHERE id=?", commentId);

    jdbcTemplate.update("UPDATE topics SET stat1=stat1-?, lastmod=CURRENT_TIMESTAMP WHERE id = ?", count, topicId);
    jdbcTemplate.update("UPDATE topics SET stat2=stat1 WHERE id=? AND stat2 > stat1", topicId);
    jdbcTemplate.update("UPDATE topics SET stat3=stat1 WHERE id=? AND stat3 > stat1", topicId);
    jdbcTemplate.update("UPDATE topics SET stat4=stat1 WHERE id=? AND stat4 > stat1", topicId);

    int groupId = jdbcTemplate.queryForInt("SELECT groupid FROM topics WHERE id = ?", topicId);
    jdbcTemplate.update("UPDATE groups SET stat1=stat1-? WHERE id = ?", count, groupId);
  }

  @Override
  public List<Integer> doDeleteReplys(int msgid, User user, boolean score) {
    List<Integer> deleted = deleteReplys(msgid, user, score, 0);

    if (!deleted.isEmpty()) {
      updateStatsAfterDelete(msgid, deleted.size());
    }

    return deleted;
  }

  /**
   * Помощник по рекурсивному удалению комментариев
   *
   * @param msgid  идентификационнай номер комментария
   * @param user   пользователь, удаляющий комментарий
   * @param score  снимать ли скор у автора комментария
   * @param depth  текущий уровень ответов
   * @return список идентификационных номеров удалённых комментариев
   */
  private List<Integer> deleteReplys(int msgid, User user, boolean score, int depth) {
    List<Integer> replys = getReplysForUpdate(msgid);
    List<Integer> deleted = new LinkedList<Integer>();
    for (Integer r : replys) {
      deleted.addAll(deleteReplys(r, user, score, depth + 1));

      boolean del;

      switch (depth) {
        case 0:
          if (score) {
            del = deleteComment(r, "7.1 Ответ на некорректное сообщение (авто, уровень 0)", user, -2);
          } else {
            del = deleteComment(r, "7.1 Ответ на некорректное сообщение (авто)", user, 0);
          }
          break;
        case 1:
          if (score) {
            del = deleteComment(r, "7.1 Ответ на некорректное сообщение (авто, уровень 1)", user, -1);
          } else {
            del = deleteComment(r, "7.1 Ответ на некорректное сообщение (авто)", user, 0);
          }
          break;
        default:
          del = deleteComment(r, "7.1 Ответ на некорректное сообщение (авто, уровень >1)", user, 0);
          break;
      }

      if (del) {
        deleted.add(r);
      }
    }

    return deleted;
  }

  /**
   * Какие ответы на комментарий
   *
   * @param msgid id комментария
   * @return список ответов на комментарий
   */
  private List<Integer> getReplysForUpdate(int msgid) {
    return jdbcTemplate.queryForList(replysForComment, Integer.class, msgid);
  }

  @Override
  public int getReplaysCount(int msgid) {
    return jdbcTemplate.queryForInt(replysForCommentCount, msgid);
  }

  @Override
  public List<Integer> deleteAllByUser(User user, final User moderator) {
    final List<Integer> deletedCommentIds = new ArrayList<Integer>();

    // Удаляем все комментарии
    jdbcTemplate.query("SELECT id FROM comments WHERE userid=? AND not deleted ORDER BY id DESC FOR update",
      new RowCallbackHandler() {
        @Override
        public void processRow(ResultSet resultSet) throws SQLException {
          int msgid = resultSet.getInt("id");
          deletedCommentIds.addAll(doDeleteReplys(msgid, moderator, false));
          if (deleteCommentWithoutTransaction(msgid, "Блокировка пользователя с удалением сообщений", moderator)) {
            deletedCommentIds.add(msgid);
          }
        }
      },
      user.getId()
    );
    return deletedCommentIds;
  }

  @Override
  public DeleteCommentResult deleteCommentsByIPAddress(String ip, Timestamp timedelta, final User moderator, final String reason) {

    final List<Integer> deletedTopicIds = new ArrayList<Integer>();
    final List<Integer> deletedCommentIds = new ArrayList<Integer>();

    final Map<Integer, String> deleteInfo = new HashMap<Integer, String>();
    // Удаляем топики
    jdbcTemplate.query("SELECT id FROM topics WHERE postip=?::inet AND not deleted AND postdate>? FOR UPDATE",
      new RowCallbackHandler() {
        @Override
        public void processRow(ResultSet resultSet) throws SQLException {
          int msgid = resultSet.getInt("id");
          deletedTopicIds.add(msgid);
          deleteInfo.put(msgid, "Топик " + msgid + " удален");
          jdbcTemplate.update("UPDATE topics SET deleted='t',sticky='f' WHERE id=?", msgid);
          deleteInfoDao.insert(msgid, moderator, reason, 0);
        }
      },
      ip, timedelta);
    // Удаляем комментарии если на них нет ответа
    jdbcTemplate.query("SELECT id FROM comments WHERE postip=?::inet AND not deleted AND postdate>? ORDER BY id DESC FOR update",
      new RowCallbackHandler() {
        @Override
        public void processRow(ResultSet resultSet) throws SQLException {
          int msgid = resultSet.getInt("id");
          if (getReplaysCount(msgid) == 0) {
            if (deleteCommentWithoutTransaction(msgid, reason, moderator)) {
              deletedCommentIds.add(msgid);
              deleteInfo.put(msgid, "Комментарий " + msgid + " удален");
            } else {
              deleteInfo.put(msgid, "Комментарий " + msgid + " уже был удален");
            }
          } else {
            deleteInfo.put(msgid, "Комментарий " + msgid + " пропущен");
          }
        }
      },
      ip, timedelta);

    return new DeleteCommentResult(deletedTopicIds, deletedCommentIds, deleteInfo);
  }

  @Override
  public int saveNewMessage(
    final Comment comment,
    String message
  ) {
    final int msgid = jdbcTemplate.queryForInt("select nextval('s_msgid') as msgid");

    jdbcTemplate.execute(
      "INSERT INTO comments (id, userid, title, postdate, replyto, deleted, topic, postip, ua_id) VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, 'f', ?, ?::inet, create_user_agent(?))",
      new PreparedStatementCallback<Object>() {
        @Override
        public Object doInPreparedStatement(PreparedStatement pst) throws SQLException, DataAccessException {
          pst.setInt(1, msgid);
          pst.setInt(2, comment.getUserid());
          pst.setString(3, comment.getTitle());
          pst.setInt(5, comment.getTopicId());
          pst.setString(6, comment.getPostIP());
          pst.setString(7, comment.getUserAgent());

          if (comment.getReplyTo() != 0) {
            pst.setInt(4, comment.getReplyTo());
          } else {
            pst.setNull(4, Types.INTEGER);
          }

          pst.executeUpdate();

          return null;
        }
      }
    );

    insertMsgbase.execute(ImmutableMap.<String, Object>of(
      "id", msgid,
      "message", message,
      "bbcode", true)
    );

    return msgid;
  }

  @Override
  public void edit(final Comment oldComment, final Comment newComment, final String commentBody) {
    jdbcTemplate.update(
      "UPDATE comments SET title=? WHERE id=?",
      newComment.getTitle(),
      oldComment.getId()
    );

    jdbcTemplate.update(
      "UPDATE msgbase SET message=? WHERE id=?",
      commentBody,
      oldComment.getId()
    );
  }

  @Override
  public void updateLatestEditorInfo (int id, int editorId, Date editDate, int editCount) {
    jdbcTemplate.update(
      "UPDATE comments set editor_id = ? , edit_date = ?, edit_count = ? WHERE id = ?",
      editorId,
      new Timestamp(editDate.getTime()),
      editCount,
      id
    );
  }

  @Override
  public List<CommentsListItem> getUserComments(int userId, int limit, int offset) {
    return jdbcTemplate.query(
      "SELECT sections.name as ptitle, groups.title as gtitle, topics.title, " +
        "topics.id as topicid, comments.id as msgid, comments.postdate " +
        "FROM sections, groups, topics, comments " +
        "WHERE sections.id=groups.section AND groups.id=topics.groupid " +
        "AND comments.topic=topics.id " +
        "AND comments.userid=? AND NOT comments.deleted ORDER BY postdate DESC LIMIT ? OFFSET ?",
      new RowMapper<CommentsListItem>() {
        @Override
        public CommentsListItem mapRow(ResultSet rs, int rowNum) throws SQLException {
          CommentsListItem item = new CommentsListItem();

          item.setSectionTitle(rs.getString("ptitle"));
          item.setGroupTitle(rs.getString("gtitle"));
          item.setTopicId(rs.getInt("topicid"));
          item.setCommentId(rs.getInt("msgid"));
          item.setTitle(StringUtil.makeTitle(rs.getString("title")));
          item.setPostdate(rs.getTimestamp("postdate"));

          return item;
        }
      },
      userId,
      limit,
      offset
    );
  }

  @Override
  public List<DeletedListItem> getLastDeletedCommentsForUser(int userId) {
    return jdbcTemplate.query(
      "SELECT " +
        "sections.name as ptitle, groups.title as gtitle, topics.title, topics.id as msgid, del_info.reason, deldate, bonus " +
        "FROM sections, groups, topics, comments, del_info " +
        "WHERE sections.id=groups.section " +
        "AND groups.id=topics.groupid " +
        "AND comments.topic=topics.id " +
        "AND del_info.msgid=comments.id " +
        "AND comments.userid=? " +
        "AND del_info.delby!=comments.userid " +
        "ORDER BY del_info.delDate DESC NULLS LAST, del_info.msgid DESC LIMIT 20",
      new RowMapper<DeletedListItem>() {
        @Override
        public DeletedListItem mapRow(ResultSet rs, int rowNum) throws SQLException {
          return new DeletedListItem(rs);
        }
      },
      userId
    );
  }

  @Override
  public boolean isHaveAnswers(int commentId) {
    int answersCount = jdbcTemplate.queryForInt("select count (id) from comments where replyto = ?",commentId);
    return answersCount != 0;
  }

  @Override
  public  List<DeletedCommentForUser> getDeletedCommentsForUser(User user, int offset, int limit) {
    StringBuilder query = new StringBuilder();
    List <Object> queryParameters = new ArrayList<Object>();
    query
        .append("SELECT")
        .append(" del_info.msgid as msgid, topics.title as subj, comments.title as comment_title, ")
        .append(" reason, bonus, delby, ")
        .append(" case deldate is null when 't' then '1970-01-01 00:00:00'::timestamp else deldate end as del_date ")
        .append(" FROM del_info ")
        .append(" JOIN comments ON comments.id = del_info.msgid ")
        .append(" JOIN topics ON topics.id = comments.topic ")
        .append(" WHERE comments.userid = ? ")
        .append(" AND del_info.delby != comments.userid ")
        .append(" ORDER BY del_date DESC ");

    queryParameters.add(user.getId());

    if(limit == 0) {
      query.append(" OFFSET ? ");
      queryParameters.add(offset);
    } else {
      query.append(" LIMIT ? OFFSET ?");
      queryParameters.add(limit);
      queryParameters.add(offset);
    }

    return jdbcTemplate.query(query.toString(), queryParameters.toArray(), new RowMapper<DeletedCommentForUser>() {
      @Override
      public DeletedCommentForUser mapRow(ResultSet resultSet, int i) throws SQLException {
        return new DeletedCommentForUser(resultSet);
      }
    });
  }

  @Override
  public int getCountDeletedCommentsForUser(User user) {
    return jdbcTemplate.queryForInt("SELECT count(del_info.msgid) FROM del_info JOIN comments ON comments.id = del_info.msgid WHERE del_info.delby != comments.userid AND comments.userid = ?", user.getId());
  }
}
