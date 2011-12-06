package ru.org.linux.dao;

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
import ru.org.linux.dto.*;
import ru.org.linux.site.*;
import ru.org.linux.spring.commons.CacheProvider;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.bbcode.LorCodeService;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * Операции над комментариями
 */

@Repository
public class CommentDao {
  private static final Log logger = LogFactory.getLog(CommentDao.class);

  private static final String queryCommentById = "SELECT " +
          "postdate, topic, users.id as userid, comments.id as msgid, comments.title, " +
          "deleted, replyto, user_agents.name AS useragent, comments.postip " +
          "FROM comments " +
          "INNER JOIN users ON (users.id=comments.userid) " +
          "LEFT JOIN user_agents ON (user_agents.id=comments.ua_id) " +
          "WHERE comments.id=?";

  /**
   * Запрос списка комментариев для топика ВКЛЮЧАЯ удаленные
   */
  private static final String queryCommentListByTopicId = "SELECT " +
          "comments.title, topic, postdate, userid, comments.id as msgid, " +
          "replyto, deleted, user_agents.name AS useragent, comments.postip " +
          "FROM comments " +
          "LEFT JOIN user_agents ON (user_agents.id=comments.ua_id) " +
          "WHERE topic=? ORDER BY msgid ASC";

  /**
   * Запрос списка комментариев для топика ИСКЛЮЧАЯ удаленные
   */
  private static final String queryCommentListByTopicIdWithoutDeleted = "SELECT " +
          "comments.title, topic, postdate, userid, comments.id as msgid, " +
          "replyto, deleted, user_agents.name AS useragent, comments.postip " +
          "FROM comments " +
          "LEFT JOIN user_agents ON (user_agents.id=comments.ua_id) " +
          "WHERE topic=?  AND NOT deleted ORDER BY msgid ASC";

  /**
   * Запрос тела сообщения и признака bbcode для сообщения
   */
  private static final String queryCommentForPrepare = "SELECT message, bbcode FROM msgbase WHERE id=?";

  private static final String queryOnlyMessage = "SELECT message FROM msgbase WHERE id=?";

  private static final String replysForComment = "SELECT id FROM comments WHERE replyto=? AND NOT deleted FOR UPDATE";
  private static final String replysForCommentCount = "SELECT count(id) FROM comments WHERE replyto=? AND NOT deleted";
  private static final String deleteComment = "UPDATE comments SET deleted='t' WHERE id=? AND not deleted";
  private static final String insertDelinfo = "INSERT INTO del_info (msgid, delby, reason, deldate) values(?,?,?, CURRENT_TIMESTAMP)";
  private static final String updateScore = "UPDATE users SET score=score+? WHERE id=(SELECT userid FROM comments WHERE id=?)";

  private JdbcTemplate jdbcTemplate;
  private UserDao userDao;
  private DeleteInfoDao deleteInfoDao;

  private SimpleJdbcInsert insertMsgbase;

  private UserEventsDao userEventsDao;

  private LorCodeService lorCodeService;

  @Autowired
  private IgnoreListDao ignoreListDao;

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

  @Autowired
  public void setUserEventsDao(UserEventsDao userEventsDao) {
    this.userEventsDao = userEventsDao;
  }

  @Autowired
  public void setLorCodeService(LorCodeService lorCodeService) {
    this.lorCodeService = lorCodeService;
  }

  /**
   * Получить содержимое комментария
   *
   * @param comment комментарий
   * @return содержимое комментария
   */
  public String getMessage(CommentDto comment) {
    return jdbcTemplate.queryForObject(queryOnlyMessage, String.class, comment.getId());
  }

  /**
   * Получить комментарий по id
   *
   * @param id id нужного комментария
   * @return нужный комментарий
   * @throws MessageNotFoundException при отсутствии сообщения
   */
  public CommentDto getById(int id) throws MessageNotFoundException {
    CommentDto comment;
    try {
      comment = jdbcTemplate.queryForObject(queryCommentById, new RowMapper<CommentDto>() {
        @Override
        public CommentDto mapRow(ResultSet resultSet, int i) throws SQLException {
          return new CommentDto(resultSet, deleteInfoDao);
        }
      }, id);
    } catch (EmptyResultDataAccessException exception) {
      throw new MessageNotFoundException(id);
    }
    return comment;
  }

  /**
   * Список комментариев топикоа
   *
   * @param topicId     id топика
   * @param showDeleted вместе с удаленными
   * @return список комментариев топика
   */
  public List<CommentDto> getCommentList(int topicId, boolean showDeleted) {
    final List<CommentDto> comments = new ArrayList<CommentDto>();

    if (showDeleted) {
      jdbcTemplate.query(queryCommentListByTopicId, new RowCallbackHandler() {
        @Override
        public void processRow(ResultSet resultSet) throws SQLException {
          comments.add(new CommentDto(resultSet, deleteInfoDao));
        }
      }, topicId);
    } else {
      jdbcTemplate.query(queryCommentListByTopicIdWithoutDeleted, new RowCallbackHandler() {
        @Override
        public void processRow(ResultSet resultSet) throws SQLException {
          comments.add(new CommentDto(resultSet, deleteInfoDao));
        }
      }, topicId);
    }

    return comments;
  }

  /**
   * Получить html представление текста комментария
   *
   * @param id id комментария
   * @param secure https соединение?
   * @return строку html комментария
   */
  public String getPreparedComment(int id, final boolean secure) {
    return jdbcTemplate.queryForObject(queryCommentForPrepare, new RowMapper<String>() {
      @Override
      public String mapRow(ResultSet resultSet, int i) throws SQLException {
        String text = resultSet.getString("message");
        boolean isLorcode = resultSet.getBoolean("bbcode");
        if (isLorcode) {
          return lorCodeService.parseComment(text, secure);
        } else {
          return "<p>" + text + "</p>";
        }
      }
    }, id);
  }

  /**
   * Получить RSS представление текста комментария
   *
   * @param id id комментария
   * @param secure https соединение?
   * @return строку html комментария
   */
  public String getPreparedCommentRSS(int id, final boolean secure) {
    return jdbcTemplate.queryForObject(queryCommentForPrepare, new RowMapper<String>() {
      @Override
      public String mapRow(ResultSet resultSet, int i) throws SQLException {
        String text = resultSet.getString("message");
        boolean isLorcode = resultSet.getBoolean("bbcode");
        if (isLorcode) {
          return lorCodeService.parseCommentRSS(text, secure);
        } else {
          return "<p>" + text + "</p>";
        }
      }
    }, id);
  }

  /**
   * Удаляем клментарий, если на комментарий есть ответы - генерируем исключение
   *
   * @param msgid      id удаляемого сообщения
   * @param reason     причина удаления
   * @param user       модератор который удаляет
   * @param scoreBonus кол-во отрезаемого шкворца
   * @throws ScriptErrorException генерируем исключение если на комментарий есть ответы
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public boolean deleteComment(int msgid, String reason, UserDto user, int scoreBonus) throws ScriptErrorException {
    if (getReplaysCount(msgid) != 0) {
      throw new ScriptErrorException("Нельзя удалить комментарий с ответами");
    }

    return doDeleteComment(msgid, reason, user, scoreBonus);
  }

  private boolean deleteCommentWithoutTransaction(int msgid, String reason, UserDto user) throws SQLException {
    if (getReplaysCount(msgid) != 0) {
      throw new SQLException("Нельзя удалить комментарий с ответами");
    }

    return doDeleteComment(msgid, reason, user, 0);
  }

  private boolean doDeleteComment(int msgid, String reason, UserDto user, int scoreBonus) {
    int deleteCount = jdbcTemplate.update(deleteComment, msgid);

    if (deleteCount > 0) {
      jdbcTemplate.update(insertDelinfo, msgid, user.getId(), reason + " (" + scoreBonus + ')');

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

  public List<Integer> deleteReplys(int msgid, UserDto user, boolean score) {
    return deleteReplys(msgid, user, score, 0);
  }

  private List<Integer> deleteReplys(int msgid, UserDto user, boolean score, int depth) {
    List<Integer> replys = getReplysForUpdate(msgid);
    List<Integer> deleted = new LinkedList<Integer>();
    for (Integer r : replys) {
      deleted.addAll(deleteReplys(r, user, score, depth + 1));

      boolean del;

      switch (depth) {
        case 0:
          if (score) {
            del = doDeleteComment(r, "7.1 Ответ на некорректное сообщение (авто, уровень 0)", user, -2);
          } else {
            del = doDeleteComment(r, "7.1 Ответ на некорректное сообщение (авто)", user, 0);
          }
          break;
        case 1:
          if (score) {
            del = doDeleteComment(r, "7.1 Ответ на некорректное сообщение (авто, уровень 1)", user, -1);
          } else {
            del = doDeleteComment(r, "7.1 Ответ на некорректное сообщение (авто)", user, 0);
          }
          break;
        default:
          del = doDeleteComment(r, "7.1 Ответ на некорректное сообщение (авто, уровень >1)", user, 0);
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

  /**
   * Сколько ответов на комментарий
   *
   * @param msgid id комментария
   * @return число ответов на комментарий
   */
  public int getReplaysCount(int msgid) {
    return jdbcTemplate.queryForInt(replysForCommentCount, msgid);
  }

  /**
   * Блокировка и массивное удаление всех топиков и комментариев пользователя со всеми ответами на комментарии
   *
   * @param user      пользователь для экзекуции
   * @param moderator экзекутор-модератор
   * @param reason    прична блокировки
   * @return список удаленных комментариев
   * @throws UserNotFoundException генерирует исключение если пользователь отсутствует
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public DeleteCommentDto deleteAllCommentsAndBlock(UserDto user, final UserDto moderator, String reason) {
    final List<Integer> deletedTopicIds = new ArrayList<Integer>();
    final List<Integer> deletedCommentIds = new ArrayList<Integer>();

    userDao.blockWithoutTransaction(user, moderator, reason);

    // Удаляем все топики
    jdbcTemplate.query("SELECT id FROM topics WHERE userid=? AND not deleted FOR UPDATE",
            new RowCallbackHandler() {
              @Override
              public void processRow(ResultSet rs) throws SQLException {
                int mid = rs.getInt("id");
                jdbcTemplate.update("UPDATE topics SET deleted='t',sticky='f' WHERE id=?", mid);
                jdbcTemplate.update("INSERT INTO del_info (msgid, delby, reason, deldate) values(?,?,?, CURRENT_TIMESTAMP)",
                        mid, moderator.getId(), "Блокировка пользователя с удалением сообщений");
                deletedTopicIds.add(mid);
              }
            },
            user.getId());

    // Удаляем все комментарии
    jdbcTemplate.query("SELECT id FROM comments WHERE userid=? AND not deleted ORDER BY id DESC FOR update",
            new RowCallbackHandler() {
              @Override
              public void processRow(ResultSet resultSet) throws SQLException {
                int msgid = resultSet.getInt("id");
                deletedCommentIds.addAll(deleteReplys(msgid, moderator, false));
                if (deleteCommentWithoutTransaction(msgid, "Блокировка пользователя с удалением сообщений", moderator)) {
                  deletedCommentIds.add(msgid);
                }
              }
            },
            user.getId());

    return new DeleteCommentDto(deletedTopicIds, deletedCommentIds, null);
  }

  /**
   * Удаление топиков, сообщений по ip и за определнный период времени, те комментарии на которые существуют ответы пропускаем
   *
   * @param ip        ip для которых удаляем сообщения (не проверяется на корректность)
   * @param timedelta врменной промежуток удаления (не проверяется на корректность)
   * @param moderator экзекутор-можератор
   * @param reason    причина удаления, которая будет вписана для удаляемых топиков
   * @return список id удаленных сообщений
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public DeleteCommentDto deleteCommentsByIPAddress(String ip, Timestamp timedelta, final UserDto moderator, final String reason) {

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
                jdbcTemplate.update("INSERT INTO del_info (msgid, delby, reason, deldate) values(?,?,?, CURRENT_TIMESTAMP)",
                        msgid, moderator.getId(), reason);
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

    return new DeleteCommentDto(deletedTopicIds, deletedCommentIds, deleteInfo);
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public int saveNewMessage(
          final CommentDto comment,
          String message,
          Set<UserDto> userRefs
  ) throws MessageNotFoundException {
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

    userEventsDao.addUserRefEvent(userRefs.toArray(new UserDto[userRefs.size()]), comment.getTopicId(), msgid);

    if (comment.getReplyTo() != 0) {
      try {
        CommentDto parentComment = getById(comment.getReplyTo());


        if (parentComment.getUserid() != comment.getUserid()) {
          UserDto parentAuthor = userDao.getUserCached(parentComment.getUserid());

          if (!parentAuthor.isAnonymous()) {
            Set<Integer> ignoreList = ignoreListDao.get(parentAuthor);

            if (!ignoreList.contains(comment.getUserid())) {
              userEventsDao.addReplyEvent(
                      parentAuthor,
                      comment.getTopicId(),
                      msgid
              );
            }
          }
        }
      } catch (UserNotFoundException e) {
        throw new RuntimeException(e);
      }
    }

    return msgid;
  }

  public CommentList getCommentList(MessageDto topic, boolean showDeleted) {
    CacheProvider mcc = MemCachedSettings.getCache();

    String cacheId = "commentList?msgid=" + topic.getMessageId() + "&showDeleted=" + showDeleted;

    CommentList commentList = (CommentList) mcc.getFromCache(cacheId);

    if (commentList == null || commentList.getLastmod() != topic.getLastModified().getTime()) {
      commentList = new CommentList(getCommentList(topic.getId(), showDeleted), topic.getLastModified().getTime());
      mcc.storeToCache(cacheId, commentList);
    }

    return commentList;
  }

  public List<CommentListItemDto> getUserComments(UserDto user, int limit, int offset) {
    return jdbcTemplate.query(
            "SELECT sections.name as ptitle, groups.title as gtitle, topics.title, " +
            "topics.id as topicid, comments.id as msgid, comments.postdate " +
            "FROM sections, groups, topics, comments " +
            "WHERE sections.id=groups.section AND groups.id=topics.groupid " +
            "AND comments.topic=topics.id " +
            "AND comments.userid=? AND NOT comments.deleted ORDER BY postdate DESC LIMIT ? OFFSET ?",
            new RowMapper<CommentListItemDto>() {
              @Override
              public CommentListItemDto mapRow(ResultSet rs, int rowNum) throws SQLException {
                CommentListItemDto item = new CommentListItemDto();

                item.setSectionTitle(rs.getString("ptitle"));
                item.setGroupTitle(rs.getString("gtitle"));
                item.setTopicId(rs.getInt("topicid"));
                item.setCommentId(rs.getInt("msgid"));
                item.setTitle(StringUtil.makeTitle(rs.getString("title")));
                item.setPostdate(rs.getTimestamp("postdate"));

                return item;
              }
            },
            user.getId(),
            limit,
            offset
    );
  }

  public List<DeletedListItem> getDeletedComments(UserDto user) {
    return jdbcTemplate.query(
            "SELECT " +
                    "sections.name as ptitle, groups.title as gtitle, topics.title, topics.id as msgid, del_info.reason, deldate " +
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
            user.getId()
    );
  }

  public static class DeletedListItem {
    private final String ptitle;
    private final String gtitle;
    private final int msgid;
    private final String title;
    private final String reason;
    private final Timestamp delDate;

    public DeletedListItem(ResultSet rs) throws SQLException {
      ptitle = rs.getString("ptitle");
      gtitle = rs.getString("gtitle");
      msgid = rs.getInt("msgid");
      title = StringUtil.makeTitle(rs.getString("title"));
      reason = rs.getString("reason");
      delDate = rs.getTimestamp("deldate");
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
  }

}
