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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.spring.dao.DeleteInfoDao;
import ru.org.linux.user.User;
import ru.org.linux.util.StringUtil;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Операции над комментариями
 */

@Repository
class CommentDaoImpl implements CommentDao {
  private static final Log logger = LogFactory.getLog(CommentDao.class);

  private static final String queryCommentById = "SELECT " +
    "postdate, topic, userid, comments.id as msgid, comments.title, " +
    "deleted, replyto, edit_count, edit_date, editor_id, " +
    "ua_id, comments.postip " +
    "FROM comments " +
    "WHERE comments.id=?";

  /**
   * Запрос списка комментариев для топика ВКЛЮЧАЯ удаленные
   */
  private static final String queryCommentListByTopicId = "SELECT " +
    "comments.title, topic, postdate, userid, comments.id as msgid, " +
    "replyto, edit_count, edit_date, editor_id, deleted, " +
    "ua_id, comments.postip " +
    "FROM comments " +
    "WHERE topic=? ORDER BY msgid ASC";

  /**
   * Запрос списка комментариев для топика ИСКЛЮЧАЯ удаленные
   */
  private static final String queryCommentListByTopicIdWithoutDeleted = "SELECT " +
    "comments.title, topic, postdate, userid, comments.id as msgid, " +
    "replyto, edit_count, edit_date, editor_id, deleted, " +
    "ua_id, comments.postip " +
    "FROM comments " +
    "WHERE topic=?  AND NOT deleted ORDER BY msgid ASC";

  private static final String replysForComment = "SELECT id FROM comments WHERE replyto=? AND NOT deleted FOR UPDATE";
  private static final String replysForCommentCount = "SELECT count(id) FROM comments WHERE replyto=? AND NOT deleted";
  private static final String deleteComment = "UPDATE comments SET deleted='t' WHERE id=? AND not deleted";
  private static final String updateScore = "UPDATE users SET score=score+? WHERE id=(SELECT userid FROM comments WHERE id=?)";

  private JdbcTemplate jdbcTemplate;
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
          return new Comment(resultSet);
        }
      }, id);
    } catch (EmptyResultDataAccessException exception) {
      throw new MessageNotFoundException(id);
    }
    return comment;
  }

  @Override
  public List<Comment> getCommentList(int topicId, boolean showDeleted) {
    final List<Comment> comments = new ArrayList<>();

    if (showDeleted) {
      jdbcTemplate.query(queryCommentListByTopicId, new RowCallbackHandler() {
        @Override
        public void processRow(ResultSet resultSet) throws SQLException {
          comments.add(new Comment(resultSet));
        }
      }, topicId);
    } else {
      jdbcTemplate.query(queryCommentListByTopicIdWithoutDeleted, new RowCallbackHandler() {
        @Override
        public void processRow(ResultSet resultSet) throws SQLException {
          comments.add(new Comment(resultSet));
        }
      }, topicId);
    }

    return comments;
  }

  @Override
  public boolean deleteComment(int msgid, String reason, User user, int scoreBonus) {
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
  public List<Integer> deleteReplys(int msgid, User user, boolean score) {
    return doDeleteReplys(msgid, user, score, 0);
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
  private List<Integer> doDeleteReplys(int msgid, User user, boolean score, int depth) {
    List<Integer> replys = getReplysForUpdate(msgid);
    List<Integer> deleted = new LinkedList<>();
    for (Integer r : replys) {
      deleted.addAll(doDeleteReplys(r, user, score, depth + 1));

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
    final List<Integer> deletedCommentIds = new ArrayList<>();

    // Удаляем все комментарии
    List<Integer> commentIds = jdbcTemplate.queryForList("SELECT id FROM comments WHERE userid=? AND not deleted ORDER BY id DESC FOR update",
            Integer.class,
            user.getId()
    );

    for (int msgid : commentIds) {
      List<Integer> deletedReplys = deleteReplys(msgid, moderator, false);
      deletedCommentIds.addAll(deletedReplys);

      deleteComment(msgid, "Блокировка пользователя с удалением сообщений", moderator, 0);
      updateStatsAfterDelete(msgid, 1 + deletedReplys.size());
      deletedCommentIds.add(msgid);
    }

    return deletedCommentIds;
  }

  @Override
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public List<Integer> getCommentsByIPAddressForUpdate(String ip, Timestamp timedelta) {
    return jdbcTemplate.queryForList("SELECT id FROM comments WHERE postip=?::inet AND not deleted AND postdate>? ORDER BY id DESC FOR update",
            Integer.class,
            ip, timedelta);
  }

  @Override
  public int saveNewMessage(
          final Comment comment,
          String message,
          final String userAgent) {
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
          pst.setString(7, userAgent);

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
  public List<DeletedListItem> getDeletedComments(int userId) {
    return jdbcTemplate.query(
      "SELECT " +
        "sections.name as ptitle, groups.title as gtitle, topics.title, topics.id as msgid, del_info.reason, deldate, bonus, comments.id as cid " +
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
}
