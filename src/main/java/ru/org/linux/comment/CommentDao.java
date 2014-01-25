/*
 * Copyright 1998-2013 Linux.org.ru
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.msg.MsgDao;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.user.User;
import ru.org.linux.util.StringUtil;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Операции над комментариями
 */

@Repository
public class CommentDao {
  private static final Logger logger = LoggerFactory.getLogger(CommentDao.class);

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

  private static final String replysForCommentCount = "SELECT count(id) FROM comments WHERE replyto=? AND NOT deleted";
  private static final String deleteComment = "UPDATE comments SET deleted='t' WHERE id=? AND not deleted";

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Autowired
  private MsgDao msgDao;

  /**
     * Получить комментарий по id
     *
     * @param id id нужного комментария
     * @return нужный комментарий
     * @throws MessageNotFoundException при отсутствии сообщения
     */
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

  /**
     * Список комментариев топика
     *
     * @param topicId     id топика
     * @param showDeleted вместе с удаленными
     * @return список комментариев топика
     */
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

  /**
     * Удалить комментарий.
     *
     *
   * @param msgid      идентификационнай номер комментария
   * @param reason     причина удаления
   * @param user       пользователь, удаляющий комментарий
   * @return true если комментарий был удалён, иначе false
     */
  public boolean deleteComment(int msgid, String reason, User user) {
    int deleteCount = jdbcTemplate.update(deleteComment, msgid);

    if (deleteCount > 0) {
      logger.info("Удалено сообщение " + msgid + " пользователем " + user.getNick() + " по причине `" + reason + '\'');

      return true;
    } else {
      logger.info("Пропускаем удаление уже удаленного " + msgid);
      return false;
    }
  }

  /**
     * Обновляет статистику после удаления комментариев в одном топике.
     *
     * @param commentId идентификатор любого из удаленных комментариев (обычно корневой в цепочке)
     * @param count     количество удаленных комментариев
     */
  public void updateStatsAfterDelete(int commentId, int count) {
    int topicId = jdbcTemplate.queryForObject("SELECT topic FROM comments WHERE id=?", Integer.class, commentId);

    jdbcTemplate.update("UPDATE topics SET stat1=stat1-?, lastmod=CURRENT_TIMESTAMP WHERE id = ?", count, topicId);
    jdbcTemplate.update("UPDATE topics SET stat2=stat1 WHERE id=? AND stat2 > stat1", topicId);
    jdbcTemplate.update("UPDATE topics SET stat3=stat1 WHERE id=? AND stat3 > stat1", topicId);
    jdbcTemplate.update("UPDATE topics SET stat4=stat1 WHERE id=? AND stat4 > stat1", topicId);

    int groupId = jdbcTemplate.queryForObject("SELECT groupid FROM topics WHERE id = ?", Integer.class, topicId);
    jdbcTemplate.update("UPDATE groups SET stat1=stat1-? WHERE id = ?", count, groupId);
  }

  /**
     * Сколько ответов на комментарий
     *
     * @param msgid id комментария
     * @return число ответов на комментарий
     */
  public int getReplaysCount(int msgid) {
    return jdbcTemplate.queryForObject(replysForCommentCount, Integer.class, msgid);
  }

  /**
   * Массовое удаление комментариев пользователя со всеми ответами на комментарии.
   *
   * @param user пользователь для экзекуции
   * @return список удаленных комментариев
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public List<Integer> getAllByUserForUpdate(User user) {
    return jdbcTemplate.queryForList("SELECT id FROM comments WHERE userid=? AND not deleted ORDER BY id DESC FOR update",
            Integer.class,
            user.getId()
    );
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public List<Integer> getCommentsByIPAddressForUpdate(String ip, Timestamp timedelta) {
    return jdbcTemplate.queryForList("SELECT id FROM comments WHERE postip=?::inet AND not deleted AND postdate>? ORDER BY id DESC FOR update",
            Integer.class,
            ip, timedelta);
  }

  /**
     * Добавить новый комментарий.
     *
     *
     * @param comment данные комментария
     * @param message текст комментария
     * @param userAgent
     * @return идентификационный номер нового комментария
     * @throws MessageNotFoundException
     */
  public int saveNewMessage(
          final Comment comment,
          String message,
          final String userAgent) {
    final int msgid = jdbcTemplate.queryForObject("select nextval('s_msgid') as msgid", Integer.class);

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

    msgDao.addMessage(msgid, message);

    return msgid;
  }

  /**
     * Редактирование комментария.
     *
     * @param oldComment  данные старого комментария
     * @param newComment  данные нового комментария
     * @param commentBody текст нового комментария
     */
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

  /**
     * Обновить информацию о последнем редакторе комментария.
     *
     * @param id        идентификационный номер комментария
     * @param editorId  идентификационный номер редактора комментария
     * @param editDate  дата редактирования
     * @param editCount количество исправлений
     */
  public void updateLatestEditorInfo (int id, int editorId, Date editDate, int editCount) {
    jdbcTemplate.update(
      "UPDATE comments set editor_id = ? , edit_date = ?, edit_count = ? WHERE id = ?",
      editorId,
      new Timestamp(editDate.getTime()),
      editCount,
      id
    );
  }

  /**
     * Получить список комментариев пользователя.
     *
     * @param userId идентификационный номер пользователя
     * @param limit  сколько записей должно быть в ответе
     * @param offset начиная с какой позиции выдать ответ
     * @return список комментариев пользователя
     */
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

  /**
     * Получить список последних удалённых комментариев пользователя.
     *
     * @param userId идентификационный номер пользователя
     * @return список удалённых комментариев пользователя
     */
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

  /**
   * DTO-класс, описывающий данные удалённого комментария
   */
  public static class DeletedListItem {
    private final String ptitle;
    private final String gtitle;
    private final int msgid;
    private final String title;
    private final String reason;
    private final Timestamp delDate;
    private final int bonus;
    private final int cid;

    public DeletedListItem(ResultSet rs) throws SQLException {
      ptitle = rs.getString("ptitle");
      gtitle = rs.getString("gtitle");
      msgid = rs.getInt("msgid");
      title = StringUtil.makeTitle(rs.getString("title"));
      reason = rs.getString("reason");
      delDate = rs.getTimestamp("deldate");
      bonus = rs.getInt("bonus");
      cid = rs.getInt("cid");
    }

    public String getPtitle() {
      return ptitle;
    }

    public String getGtitle() {
      return gtitle;
    }

    public int getMsgid() {
      return msgid;
    }

    public String getTitle() {
      return title;
    }

    public String getReason() {
      return reason;
    }

    public Timestamp getDelDate() {
      return delDate;
    }

    public int getBonus() {
      return bonus;
    }

    public int getCommentId() {
      return cid;
    }
  }

  public static class CommentsListItem {
    private String sectionTitle;
    private String groupTitle;
    private int topicId;
    private int commentId;
    private String title;
    private Timestamp postdate;

    public String getSectionTitle() {
      return sectionTitle;
    }

    public void setSectionTitle(String sectionTitle) {
      this.sectionTitle = sectionTitle;
    }

    public String getGroupTitle() {
      return groupTitle;
    }

    public void setGroupTitle(String groupTitle) {
      this.groupTitle = groupTitle;
    }

    public int getTopicId() {
      return topicId;
    }

    public void setTopicId(int topicId) {
      this.topicId = topicId;
    }

    public int getCommentId() {
      return commentId;
    }

    public void setCommentId(int commentId) {
      this.commentId = commentId;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public Timestamp getPostdate() {
      return postdate;
    }

    public void setPostdate(Timestamp postdate) {
      this.postdate = postdate;
    }


  }
}
