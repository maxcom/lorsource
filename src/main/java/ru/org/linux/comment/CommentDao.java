/*
 * Copyright 1998-2021 Linux.org.ru
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
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.user.User;
import ru.org.linux.util.StringUtil;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.Date;

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

  private final JdbcTemplate jdbcTemplate;
  private final NamedParameterJdbcTemplate namedJdbcTemplate;

  public CommentDao(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
    namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
  }

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
      comment = jdbcTemplate.queryForObject(queryCommentById, (resultSet, i) -> new Comment(resultSet), id);
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
    if (showDeleted) {
      return jdbcTemplate.query(queryCommentListByTopicId, (resultSet, rownum) -> new Comment(resultSet), topicId);
    } else {
      return jdbcTemplate.query(queryCommentListByTopicIdWithoutDeleted, (resultSet, rownum) -> new Comment(resultSet), topicId);
    }
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

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public void undeleteComment(Comment comment) {
    jdbcTemplate.update("UPDATE comments SET deleted='f' WHERE id=?", comment.getId());
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
    jdbcTemplate.update("UPDATE topics SET stat3=stat1 WHERE id=? AND stat3 > stat1", topicId);
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

  public List<Integer> getAllByUser(User user) {
    return jdbcTemplate.queryForList("SELECT id FROM comments WHERE userid=? AND not deleted", Integer.class, user.getId());
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
   * @return идентификационный номер нового комментария
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public int saveNewMessage(final Comment comment, final Optional<String> userAgent) {
    final int msgid = jdbcTemplate.queryForObject("select nextval('s_msgid') as msgid", Integer.class);

    userAgent.map(ua -> ua.substring(0, Math.min(511, ua.length())));

    jdbcTemplate.execute(
      "INSERT INTO comments (id, userid, title, postdate, replyto, deleted, topic, postip, ua_id) VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, 'f', ?, ?::inet, create_user_agent(?))",
            (PreparedStatement pst) -> {
              pst.setInt(1, msgid);
              pst.setInt(2, comment.getUserid());
              pst.setString(3, comment.getTitle());
              pst.setInt(5, comment.getTopicId());
              pst.setString(6, comment.getPostIP());
              pst.setString(7, userAgent.orElse(null));

              if (comment.getReplyTo() != 0) {
                pst.setInt(4, comment.getReplyTo());
              } else {
                pst.setNull(4, Types.INTEGER);
              }

              pst.executeUpdate();

              return null;
            }
    );

    return msgid;
  }

  /**
     * Редактирование комментария.
     *
   * @param oldComment  данные старого комментария
   * @param title       новый заголовок
   */
  public void changeTitle(Comment oldComment, String title) {
    jdbcTemplate.update(
      "UPDATE comments SET title=? WHERE id=?", title, oldComment.getId()
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
     * Получить список последних удалённых комментариев пользователя.
     *
     * @param userId идентификационный номер пользователя
     * @return список удалённых комментариев пользователя
     */
  public List<CommentsListItem> getDeletedComments(int userId) {
    return jdbcTemplate.query(
            "SELECT " +
                    "groups.title as gtitle, topics.title, topics.id as msgid, " +
                    "del_info.reason, deldate, bonus, comments.id as cid, comments.postdate " +
                    "FROM groups, topics, comments, del_info " +
                    "WHERE groups.id=topics.groupid " +
                    "AND comments.topic=topics.id " +
                    "AND del_info.msgid=comments.id " +
                    "AND comments.userid=? " +
                    "AND del_info.delby!=comments.userid " +
                    "ORDER BY del_info.delDate DESC NULLS LAST, del_info.msgid DESC LIMIT 20",
            (rs, rowNum) ->
                    new CommentsListItem(
                            rs.getString("gtitle"),
                            rs.getInt("msgid"),
                            StringUtil.makeTitle(rs.getString("title")),
                            rs.getString("reason"),
                            rs.getTimestamp("deldate"),
                            rs.getInt("bonus"),
                            rs.getInt("cid"),
                            true,
                            rs.getTimestamp("postdate"),
                            userId),
            userId
    );
  }

  public List<CommentsListItem> getCommentsByUAIP(@Nullable String ip, @Nullable Integer userAgent, int limit) {
    String ipQuery = ip!=null?"AND comments.postip <<= :ip::inet ":"";
    String userAgentQuery = userAgent!=null?"AND comments.ua_id=:userAgent ":"";

    Map<String, Object> params = new HashMap<>();

    params.put("ip", ip);
    params.put("userAgent", userAgent);
    params.put("limit", limit);

    return namedJdbcTemplate.query(
            "SELECT groups.title as gtitle, topics.title, topics.id as msgid, " +
                  "comments.id as cid, comments.postdate, comments.deleted, del_info.deldate, del_info.reason, " +
                  "del_info.bonus, comments.userid " +
                "FROM groups JOIN topics ON groups.id=topics.groupid " +
                  "JOIN comments ON comments.topic=topics.id " +
                  "LEFT JOIN del_info ON del_info.msgid=comments.id " +
                "WHERE comments.postdate>CURRENT_TIMESTAMP-'3 days'::interval " +
                  ipQuery +
                  userAgentQuery +
                "ORDER BY postdate DESC LIMIT :limit",
            params,
            (rs, rowNum) ->
                    new CommentsListItem(
                            rs.getString("gtitle"),
                            rs.getInt("msgid"),
                            StringUtil.makeTitle(rs.getString("title")),
                            rs.getString("reason"),
                            rs.getTimestamp("deldate"),
                            rs.getInt("bonus"),
                            rs.getInt("cid"),
                            rs.getBoolean("deleted"),
                            rs.getTimestamp("postdate"),
                            rs.getInt("userid"))
    );
  }
}
