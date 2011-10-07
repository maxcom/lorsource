package ru.org.linux.spring.dao;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.site.*;
import ru.org.linux.spring.AddMessageRequest;
import ru.org.linux.util.LorHttpUtils;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * Операции над сообщениями
 */

@Repository
public class MessageDao {
  private static final Log logger = LogFactory.getLog(MessageDao.class);

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private PollDao pollDao;

  /**
   * Запрос получения полной информации о топике
   */
  private static final String queryMessage = "SELECT " +
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
  private static final String updateDeleteMessage = "UPDATE topics SET deleted='t',sticky='f' WHERE id=?";
  /**
   * Обновление информации о удалении
   */
  private static final String updateDeleteInfo = "INSERT INTO del_info (msgid, delby, reason, deldate) values(?,?,?, CURRENT_TIMESTAMP)";

  private static final String queryEditInfo = "SELECT * FROM edit_info WHERE msgid=? ORDER BY id DESC";

  private static final String queryTags = "SELECT tags_values.value FROM tags, tags_values WHERE tags.msgid=? AND tags_values.id=tags.tagid ORDER BY value";

  private static final String updateUndeleteMessage = "UPDATE topics SET deleted='f' WHERE id=?";
  private static final String updateUneleteInfo = "DELETE FROM del_info WHERE msgid=?";

  private static final String queryOnlyMessage = "SELECT message FROM msgbase WHERE id=?";

  private static final String queryTopicsIdByTime = "SELECT id FROM topics WHERE postdate>=? AND postdate<?";

  public static final String queryTimeFirstTopic = "SELECT min(postdate) FROM topics WHERE postdate!='epoch'::timestamp";

  private JdbcTemplate jdbcTemplate;

  @Autowired
  private UserDao userDao;

  @Autowired
  public void setJdbcTemplate(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  /**
   * Время создания первого топика
   * @return время
   */
  public Timestamp getTimeFirstTopic() {
    return jdbcTemplate.queryForObject(queryTimeFirstTopic, Timestamp.class);
  }

  /**
   * Получить содержимое топика
   * @param message топик
   * @return содержимое
   */
  public String getMessage(Message message) {
    return jdbcTemplate.queryForObject(queryOnlyMessage, String.class, message.getId());
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
   * Получить список топиков за месяц
   * @param year год
   * @param month месяц
   * @return список топиков
   */
  public List<Integer> getMessageForMonth(int year, int month){
    Calendar calendar = Calendar.getInstance();
    calendar.set(year, month, 1);
    Timestamp ts_start = new Timestamp(calendar.getTimeInMillis());
    calendar.add(Calendar.MONTH, 1);
    Timestamp ts_end = new Timestamp(calendar.getTimeInMillis());
    return jdbcTemplate.query(queryTopicsIdByTime, new RowMapper<Integer>() {
      @Override
      public Integer mapRow(ResultSet resultSet, int i) throws SQLException {
        return resultSet.getInt("id");
      }
    }, ts_start, ts_end);
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
   * @throws UserErrorException генерируется если некорректная делта score
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void deleteWithBonus(Message message, User user, String reason, int bonus) throws UserErrorException {
    String finalReason = reason;
    jdbcTemplate.update(updateDeleteMessage, message.getId());
    if (user.canModerate() && bonus!=0 && user.getId()!=message.getUid()) {
      if (bonus>20 || bonus<0) {
        throw new UserErrorException("Некорректное значение bonus");
      }
      userDao.changeScore(message.getUid(), -bonus);
      finalReason += " ("+bonus+ ')';
    }
    jdbcTemplate.update(updateDeleteInfo, message.getId(), user.getId(), finalReason);
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void undelete(Message message) {
    jdbcTemplate.update(updateUndeleteMessage, message.getId());
    jdbcTemplate.update(updateUneleteInfo, message.getId());
  }

  private int allocateMsgid() {
    return jdbcTemplate.queryForInt("select nextval('s_msgid') as msgid");
  }

  // call in @Transactional environment
  public int saveNewMessage(final Message msg, Template tmpl, final HttpServletRequest request, Screenshot scrn, final User user)
    throws  IOException,  ScriptErrorException {

    final Group group = groupDao.getGroup(msg.getGroupId());

    final int msgid = allocateMsgid();

    String url = msg.getUrl();
    String linktext = msg.getLinktext();

    if (group.isImagePostAllowed()) {
      if (scrn == null) {
        throw new ScriptErrorException("scrn==null!?");
      }

      Screenshot screenshot = scrn.moveTo(tmpl.getObjectConfig().getHTMLPathPrefix() + "/gallery", Integer.toString(msgid));

      url = "gallery/" + screenshot.getMainFile().getName();
      linktext = "gallery/" + screenshot.getIconFile().getName();
    }

    final String finalUrl = url;
    final String finalLinktext = linktext;
    jdbcTemplate.execute(
            "INSERT INTO topics (groupid, userid, title, url, moderate, postdate, id, linktext, deleted, ua_id, postip) VALUES (?, ?, ?, ?, 'f', CURRENT_TIMESTAMP, ?, ?, 'f', create_user_agent(?),?::inet)",
            new PreparedStatementCallback<String>() {
              @Override
              public String doInPreparedStatement(PreparedStatement pst) throws SQLException, DataAccessException {
                pst.setInt(1, group.getId());
                pst.setInt(2, user.getId());
                pst.setString(3, msg.getTitle());
                pst.setString(4, finalUrl);
                pst.setInt(5, msgid);
                pst.setString(6, finalLinktext);
                pst.setString(7, request.getHeader("User-Agent"));
                pst.setString(8, msg.getPostIP());
                pst.executeUpdate();

                return null;
              }
            }
    );

    // insert message text
    jdbcTemplate.update(
            "INSERT INTO msgbase (id, message, bbcode) values (?,?, ?)",
            msgid, msg.getMessage(), msg.isLorcode()
    );

    String logmessage = "Написана тема " + msgid + ' ' + LorHttpUtils.getRequestIP(request);
    logger.info(logmessage);

    return msgid;
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public int addMessage(HttpServletRequest request, AddMessageRequest form, Template tmpl, Group group, User user, Screenshot scrn, Message previewMsg) throws IOException, ScriptErrorException, UserErrorException {
    final int msgid = saveNewMessage(
            previewMsg,
            tmpl,
            request,
            scrn,
            user
    );

    if (group.isPollPostAllowed()) {
      pollDao.createPoll(Arrays.asList(form.getPoll()), form.isMultiSelect(), msgid);
    }

    if (form.getTags() != null) {
      final List<String> tags = TagDao.parseTags(form.getTags());

      jdbcTemplate.execute(new ConnectionCallback<String>() {
        @Override
        public String doInConnection(Connection db) throws SQLException, DataAccessException {
          TagDao.updateTags(db, msgid, tags);
          TagDao.updateCounters(db, Collections.<String>emptyList(), tags);

          return null;
        }
      });
    }

    return msgid;
  }

  public static boolean updateMessage(Connection db, Message msg, User editor, List<String> newTags) throws SQLException {
    SingleConnectionDataSource scds = new SingleConnectionDataSource(db, true);
    NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(scds);

    PreparedStatement pstGet = db.prepareStatement("SELECT message,title,linktext,url,minor FROM msgbase JOIN topics ON msgbase.id=topics.id WHERE topics.id=? FOR UPDATE");

    pstGet.setInt(1, msg.getId());
    ResultSet rs = pstGet.executeQuery();
    if (!rs.next()) {
      throw new RuntimeException("Can't fetch previous message text");
    }

    String oldMessage = rs.getString("message");
    String oldTitle = rs.getString("title");
    String oldLinkText = rs.getString("linktext");
    String oldURL = rs.getString("url");
    boolean oldMinor = rs.getBoolean("minor");

    rs.close();
    pstGet.close();

    List<String> oldTags = TagDao.getMessageTags(db, msg.getId());

    EditInfoDTO editInfo = new EditInfoDTO();

    editInfo.setMsgid(msg.getId());
    editInfo.setEditor(editor.getId());

    boolean modified = false;

    if (!oldMessage.equals(msg.getMessage())) {
      editInfo.setOldmessage(oldMessage);
      modified = true;

      jdbcTemplate.update(
        "UPDATE msgbase SET message=:message WHERE id=:msgid",
        ImmutableMap.of("message", msg.getMessage(), "msgid", msg.getId())
      );
    }

    if (!oldTitle.equals(msg.getTitle())) {
      modified = true;
      editInfo.setOldtitle(oldTitle);

      jdbcTemplate.update(
        "UPDATE topics SET title=:title WHERE id=:id",
        ImmutableMap.of("title", msg.getTitle(), "id", msg.getId())
      );
    }

    if (!equalStrings(oldLinkText, msg.getLinktext())) {
      modified = true;
      editInfo.setOldlinktext(oldLinkText);

      jdbcTemplate.update(
        "UPDATE topics SET linktext=:linktext WHERE id=:id",
        ImmutableMap.of("linktext", msg.getLinktext(), "id", msg.getId())
      );
    }

    if (!equalStrings(oldURL, msg.getUrl())) {
      modified = true;
      editInfo.setOldurl(oldURL);

      jdbcTemplate.update(
        "UPDATE topics SET url=:url WHERE id=:id",
        ImmutableMap.of("url", msg.getUrl(), "id", msg.getId())
      );
    }

    if (newTags != null) {
      boolean modifiedTags = TagDao.updateTags(db, msg.getId(), newTags);

      if (modifiedTags) {
        editInfo.setOldtags(TagDao.toString(oldTags));
        TagDao.updateCounters(db, oldTags, newTags);
        modified = true;
      }
    }

    if (oldMinor != msg.isMinor()) {
      jdbcTemplate.update("UPDATE topics SET minor=:minor WHERE id=:id",
              ImmutableMap.of("minor", msg.isMinor(), "id", msg.getId()));
      modified = true;
    }

    if (modified) {
      SimpleJdbcInsert insert =
        new SimpleJdbcInsert(scds)
          .withTableName("edit_info")
          .usingColumns("msgid", "editor", "oldmessage", "oldtitle", "oldtags", "oldlinktext", "oldurl");

      insert.execute(new BeanPropertySqlParameterSource(editInfo));
    }

    return modified;
  }

  private static boolean equalStrings(String s1, String s2) {
    if (Strings.isNullOrEmpty(s1)) {
      return Strings.isNullOrEmpty(s2);
    }

    return s1.equals(s2);
  }
}
