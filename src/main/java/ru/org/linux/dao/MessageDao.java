package ru.org.linux.dao;

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
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.dto.GroupDto;
import ru.org.linux.dto.MessageDto;
import ru.org.linux.dto.SectionDto;
import ru.org.linux.dto.UserDto;
import ru.org.linux.site.*;
import ru.org.linux.spring.AddMessageRequest;
import ru.org.linux.util.LorHttpUtils;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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

  @Autowired
  private TagCloudDao tagCloudDao;

  @Autowired
  private UserEventsDao userEventsDao;


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

  private static final String updateUndeleteMessage = "UPDATE topics SET deleted='f' WHERE id=?";
  private static final String updateUneleteInfo = "DELETE FROM del_info WHERE msgid=?";

  private static final String queryOnlyMessage = "SELECT message FROM msgbase WHERE id=?";

  private static final String queryTopicsIdByTime = "SELECT id FROM topics WHERE postdate>=? AND postdate<?";

  public static final String queryTimeFirstTopic = "SELECT min(postdate) FROM topics WHERE postdate!='epoch'::timestamp";

  private JdbcTemplate jdbcTemplate;
  private NamedParameterJdbcTemplate namedJdbcTemplate;
  private SimpleJdbcInsert editInsert;

  @Autowired
  private UserDao userDao;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
    namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    editInsert =
        new SimpleJdbcInsert(dataSource)
            .withTableName("edit_info")
            .usingColumns("msgid", "editor", "oldmessage", "oldtitle", "oldtags", "oldlinktext", "oldurl");
  }

  /**
   * Время создания первого топика.
   *
   * @return время
   */
  public Timestamp getTimeFirstTopic() {
    return jdbcTemplate.queryForObject(queryTimeFirstTopic, Timestamp.class);
  }

  /**
   * Получить содержимое топика.
   *
   * @param messageDto топик
   * @return содержимое
   */
  public String getMessage(MessageDto messageDto) {
    return jdbcTemplate.queryForObject(queryOnlyMessage, String.class, messageDto.getId());
  }

  /**
   * Получить сообщение по id.
   *
   * @param id id нужного сообщения
   * @return сообщение
   * @throws MessageNotFoundException при отсутствии сообщения
   */
  public MessageDto getById(int id) throws MessageNotFoundException {
    MessageDto messageDto;
    try {
      messageDto = jdbcTemplate.queryForObject(queryMessage, new RowMapper<MessageDto>() {
        @Override
        public MessageDto mapRow(ResultSet resultSet, int i) throws SQLException {
          return new MessageDto(resultSet);
        }
      }, id);
    } catch (EmptyResultDataAccessException exception) {
      throw new MessageNotFoundException(id);
    }
    return messageDto;
  }

  /**
   * Получить group messageDto.
   *
   * @param messageDto messageDto
   * @return group
   * @throws BadGroupException если что-то неправильно
   */
  public GroupDto getGroup(MessageDto messageDto) throws BadGroupException {
    return groupDao.getGroup(messageDto.getGroupId());
  }

  /**
   * Получить список топиков за месяц.
   *
   * @param year  год
   * @param month месяц
   * @return список топиков
   */
  public List<Integer> getMessageForMonth(int year, int month) {
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
   * Получить информации о редактировании топика.
   *
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
   * Получить тэги топика.
   * TODO перенести в service
   *
   * @param messageDto топик
   * @return список тэгов
   */
  public ImmutableList<String> getTags(MessageDto messageDto) {
    return tagCloudDao.getMessageTags(messageDto.getId());
  }

  /**
   * Удаление топика и если удаляет модератор изменить автору score.
   *
   * @param messageDto удаляемый топик
   * @param user    удаляющий пользователь
   * @param reason  прчина удаления
   * @param bonus   дельта изменения score автора топика
   * @throws UserErrorException генерируется если некорректная делта score
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void deleteWithBonus(MessageDto messageDto, UserDto user, String reason, int bonus) throws UserErrorException {
    String finalReason = reason;
    jdbcTemplate.update(updateDeleteMessage, messageDto.getId());
    if (user.isModerator() && bonus != 0 && user.getId() != messageDto.getUid()) {
      if (bonus > 20 || bonus < 0) {
        throw new UserErrorException("Некорректное значение bonus");
      }
      userDao.changeScore(messageDto.getUid(), -bonus);
      finalReason += " (" + bonus + ')';
    }
    jdbcTemplate.update(updateDeleteInfo, messageDto.getId(), user.getId(), finalReason);
  }

  /**
   *
   * @param messageDto
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void undelete(MessageDto messageDto) {
    jdbcTemplate.update(updateUndeleteMessage, messageDto.getId());
    jdbcTemplate.update(updateUneleteInfo, messageDto.getId());
  }

  /**
   *
   * @return
   */
  private int allocateMsgid() {
    return jdbcTemplate.queryForInt("select nextval('s_msgid') as msgid");
  }

  /**
   *
   * @param msg
   * @param tmpl
   * @param request
   * @param scrn
   * @param user
   * @return
   * @throws IOException
   * @throws ScriptErrorException
   */
// call in @Transactional environment
  public int saveNewMessage(final MessageDto msg, Template tmpl, final HttpServletRequest request, Screenshot scrn, final UserDto user)
      throws IOException, ScriptErrorException {

    final GroupDto groupDto = groupDao.getGroup(msg.getGroupId());

    final int msgid = allocateMsgid();

    String url = msg.getUrl();
    String linktext = msg.getLinktext();

    if (groupDto.isImagePostAllowed()) {
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
            pst.setInt(1, groupDto.getId());
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

  /**
   *
   *
   * @param request
   * @param form
   * @param tmpl
   * @param groupDto
   * @param user
   * @param scrn
   * @param previewMsg
   * @param userRefs
   * @return
   * @throws IOException
   * @throws ScriptErrorException
   * @throws UserErrorException
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public int addMessage(HttpServletRequest request, AddMessageRequest form, Template tmpl, GroupDto groupDto, UserDto user, Screenshot scrn, MessageDto previewMsg, Set<UserDto> userRefs) throws IOException, ScriptErrorException, UserErrorException {
    final int msgid = saveNewMessage(
        previewMsg,
        tmpl,
        request,
        scrn,
        user
    );

    if (groupDto.isPollPostAllowed()) {
      pollDao.createPoll(Arrays.asList(form.getPoll()), form.isMultiSelect(), msgid);
    }

    if (form.getTags() != null) {
      final List<String> tags = TagCloudDao.parseTags(form.getTags());

      tagCloudDao.updateTags(msgid, tags);
      tagCloudDao.updateCounters(Collections.<String>emptyList(), tags);
    }

    userEventsDao.addUserRefEvent(userRefs.toArray(new UserDto[userRefs.size()]), msgid);

    return msgid;
  }

  /**
   *
   * @param oldMsg
   * @param msg
   * @param editor
   * @param newTags
   * @return
   */
  private boolean updateMessage(MessageDto oldMsg, MessageDto msg, UserDto editor, List<String> newTags) {
    List<String> oldTags = tagCloudDao.getMessageTags(msg.getId());

    EditInfoDTO editInfo = new EditInfoDTO();

    editInfo.setMsgid(msg.getId());
    editInfo.setEditor(editor.getId());

    boolean modified = false;

    if (!oldMsg.getMessage().equals(msg.getMessage())) {
      editInfo.setOldmessage(oldMsg.getMessage());
      modified = true;

      namedJdbcTemplate.update(
          "UPDATE msgbase SET message=:message WHERE id=:msgid",
          ImmutableMap.of("message", msg.getMessage(), "msgid", msg.getId())
      );
    }

    if (!oldMsg.getTitle().equals(msg.getTitle())) {
      modified = true;
      editInfo.setOldtitle(oldMsg.getTitle());

      namedJdbcTemplate.update(
          "UPDATE topics SET title=:title WHERE id=:id",
          ImmutableMap.of("title", msg.getTitle(), "id", msg.getId())
      );
    }

    if (!equalStrings(oldMsg.getLinktext(), msg.getLinktext())) {
      modified = true;
      editInfo.setOldlinktext(oldMsg.getLinktext());

      namedJdbcTemplate.update(
          "UPDATE topics SET linktext=:linktext WHERE id=:id",
          ImmutableMap.of("linktext", msg.getLinktext(), "id", msg.getId())
      );
    }

    if (!equalStrings(oldMsg.getUrl(), msg.getUrl())) {
      modified = true;
      editInfo.setOldurl(oldMsg.getUrl());

      namedJdbcTemplate.update(
          "UPDATE topics SET url=:url WHERE id=:id",
          ImmutableMap.of("url", msg.getUrl(), "id", msg.getId())
      );
    }

    if (newTags != null) {
      boolean modifiedTags = tagCloudDao.updateTags(msg.getId(), newTags);

      if (modifiedTags) {
        editInfo.setOldtags(TagCloudDao.toString(oldTags));
        tagCloudDao.updateCounters(oldTags, newTags);
        modified = true;
      }
    }

    if (oldMsg.isMinor() != msg.isMinor()) {
      namedJdbcTemplate.update("UPDATE topics SET minor=:minor WHERE id=:id",
          ImmutableMap.of("minor", msg.isMinor(), "id", msg.getId()));
      modified = true;
    }

    if (modified) {
      editInsert.execute(new BeanPropertySqlParameterSource(editInfo));
    }

    return modified;
  }

  /**
   *
   * @param s1
   * @param s2
   * @return
   */
  private static boolean equalStrings(String s1, String s2) {
    if (Strings.isNullOrEmpty(s1)) {
      return Strings.isNullOrEmpty(s2);
    }

    return s1.equals(s2);
  }

  /**
   *
   * @param messageDto
   * @param newVariants
   * @param multiselect
   * @return
   * @throws PollNotFoundException
   */
  private boolean updatePoll(MessageDto messageDto, List<PollVariant> newVariants, boolean multiselect) throws PollNotFoundException {
    boolean modified = false;

    final Poll poll = pollDao.getPollByTopicId(messageDto.getId());

    ImmutableList<PollVariant> oldVariants = pollDao.getPollVariants(poll, Poll.ORDER_ID);

    Map<Integer, String> newMap = PollVariant.toMap(newVariants);

    for (final PollVariant var : oldVariants) {
      final String label = newMap.get(var.getId());

      if (!equalStrings(var.getLabel(), label)) {
        modified = true;
      }

      if (Strings.isNullOrEmpty(label)) {
        pollDao.removeVariant(var);
      } else {
        pollDao.updateVariant(var, label);
      }
    }

    for (final PollVariant var : newVariants) {
      if (var.getId() == 0 && !Strings.isNullOrEmpty(var.getLabel())) {
        modified = true;

        pollDao.addNewVariant(poll, var.getLabel());
      }
    }

    if (poll.isMultiSelect() != multiselect) {
      modified = true;
      jdbcTemplate.update("UPDATE votenames SET multiselect=? WHERE id=?", multiselect, poll.getId());
    }

    return modified;
  }

  /**
   *
   * @param newMsg
   * @param messageDto
   * @param user
   * @param newTags
   * @param commit
   * @param changeGroupId
   * @param bonus
   * @param pollVariants
   * @param multiselect
   * @return
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public boolean updateAndCommit(
      MessageDto newMsg,
      MessageDto messageDto,
      UserDto user,
      List<String> newTags,
      boolean commit,
      Integer changeGroupId,
      int bonus,
      List<PollVariant> pollVariants,
      boolean multiselect
  ) {
    boolean modified = updateMessage(messageDto, newMsg, user, newTags);

    try {
      if (pollVariants != null && updatePoll(messageDto, pollVariants, multiselect)) {
        modified = true;
      }
    } catch (PollNotFoundException e) {
      throw new RuntimeException(e);
    }

    if (commit) {
      if (changeGroupId != null) {
        if (messageDto.getGroupId() != changeGroupId) {
          jdbcTemplate.update("UPDATE topics SET groupid=? WHERE id=?", changeGroupId, messageDto.getId());
          jdbcTemplate.update("UPDATE groups SET stat4=stat4+1 WHERE id=? or id=?", messageDto.getGroupId(), changeGroupId);
        }
      }

      commit(messageDto, user, bonus);
    }

    if (modified) {
      logger.info("сообщение " + messageDto.getId() + " исправлено " + user.getNick());
    }

    return modified;
  }

  /**
   *
   * @param msg
   * @param commiter
   * @param bonus
   */
  private void commit(MessageDto msg, UserDto commiter, int bonus) {
    if (bonus < 0 || bonus > 20) {
      throw new IllegalStateException("Неверное значение bonus");
    }

    jdbcTemplate.update(
        "UPDATE topics SET moderate='t', commitby=?, commitdate='now' WHERE id=?",
        commiter.getId(),
        msg.getId()
    );

    UserDto author;
    try {
      author = userDao.getUser(msg.getUid());
    } catch (UserNotFoundException e) {
      throw new RuntimeException(e);
    }

    userDao.changeScore(author.getId(), bonus);
  }

  /**
   *
   * @param msg
   */
  public void uncommit(MessageDto msg) {
    jdbcTemplate.update("UPDATE topics SET moderate='f',commitby=NULL,commitdate=NULL WHERE id=?", msg.getId());
  }

  /**
   *
   * @param messageDto
   * @param currentUser
   * @return
   */
  public MessageDto getPreviousMessage(MessageDto messageDto, UserDto currentUser) {
    int scrollMode = SectionDto.getScrollMode(messageDto.getSectionId());

    List<Integer> res;

    switch (scrollMode) {
      case SectionDto.SCROLL_SECTION:
        res = jdbcTemplate.queryForList(
            "SELECT topics.id as msgid FROM topics WHERE topics.commitdate=(SELECT max(commitdate) FROM topics, groups, sections WHERE sections.id=groups.section AND topics.commitdate<? AND topics.groupid=groups.id AND groups.section=? AND (topics.moderate OR NOT sections.moderate) AND NOT deleted)",
            Integer.class,
            messageDto.getCommitDate(),
            messageDto.getSectionId()
        );
        break;

      case SectionDto.SCROLL_GROUP:
        if (currentUser == null || currentUser.isAnonymous()) {
          res = jdbcTemplate.queryForList(
              "SELECT max(topics.id) as msgid " +
                  "FROM topics " +
                  "WHERE topics.id<? AND topics.groupid=? AND NOT deleted",
              Integer.class,
              messageDto.getMessageId(),
              messageDto.getGroupId()
          );
        } else {
          res = jdbcTemplate.queryForList(
              "SELECT max(topics.id) as msgid " +
                  "FROM topics " +
                  "WHERE topics.id<? AND topics.groupid=? AND NOT deleted " +
                  "AND userid NOT IN (select ignored from ignore_list where userid=?)",
              Integer.class,
              messageDto.getMessageId(),
              messageDto.getGroupId(),
              currentUser.getId()
          );
        }

        break;

      case SectionDto.SCROLL_NOSCROLL:
      default:
        return null;
    }

    try {
      if (res.isEmpty() || res.get(0) == null) {
        return null;
      }

      int prevMsgid = res.get(0);

      return getById(prevMsgid);
    } catch (MessageNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   *
   * @param messageDto
   * @param currentUser
   * @return
   */
  public MessageDto getNextMessage(MessageDto messageDto, UserDto currentUser) {
    int scrollMode = SectionDto.getScrollMode(messageDto.getSectionId());

    List<Integer> res;

    switch (scrollMode) {
      case SectionDto.SCROLL_SECTION:
        res = jdbcTemplate.queryForList(
            "SELECT topics.id as msgid FROM topics WHERE topics.commitdate=(SELECT min(commitdate) FROM topics, groups, sections WHERE sections.id=groups.section AND topics.commitdate>? AND topics.groupid=groups.id AND groups.section=? AND (topics.moderate OR NOT sections.moderate) AND NOT deleted)",
            Integer.class,
            messageDto.getCommitDate(),
            messageDto.getSectionId()
        );
        break;

      case SectionDto.SCROLL_GROUP:
        if (currentUser == null || currentUser.isAnonymous()) {
          res = jdbcTemplate.queryForList(
              "SELECT min(topics.id) as msgid " +
                  "FROM topics " +
                  "WHERE topics.id>? AND topics.groupid=? AND NOT deleted",
              Integer.class,
              messageDto.getId(),
              messageDto.getGroupId()
          );
        } else {
          res = jdbcTemplate.queryForList(
              "SELECT min(topics.id) as msgid " +
                  "FROM topics " +
                  "WHERE topics.id>? AND topics.groupid=? AND NOT deleted " +
                  "AND userid NOT IN (select ignored from ignore_list where userid=?)",
              Integer.class,
              messageDto.getId(),
              messageDto.getGroupId(),
              currentUser.getId()
          );
        }
        break;

      case SectionDto.SCROLL_NOSCROLL:
      default:
        return null;
    }

    try {
      if (res.isEmpty() || res.get(0) == null) {
        return null;
      }

      int nextMsgid = res.get(0);

      return getById(nextMsgid);
    } catch (MessageNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   *
   * @param msgid
   * @return
   */
  public List<EditInfoDTO> loadEditInfo(int msgid) {
    List<EditInfoDTO> list = jdbcTemplate.query(
        "SELECT * FROM edit_info WHERE msgid=? ORDER BY id DESC",
        BeanPropertyRowMapper.newInstance(EditInfoDTO.class),
        msgid
    );

    return ImmutableList.copyOf(list);
  }

  /**
   *
   * @param msgid
   * @param b
   */
  public void resolveMessage(int msgid, boolean b) {
    jdbcTemplate.update(
        "UPDATE topics SET resolved=?,lastmod=lastmod+'1 second'::interval WHERE id=?",
        b,
        msgid
    );
  }

  /**
   *
   * @param msg
   * @param postscore
   * @param sticky
   * @param notop
   * @param minor
   */
  public void setTopicOptions(MessageDto msg, int postscore, boolean sticky, boolean notop, boolean minor) {
    jdbcTemplate.update(
        "UPDATE topics SET postscore=?, sticky=?, notop=?, lastmod=CURRENT_TIMESTAMP,minor=? WHERE id=?",
        postscore,
        sticky,
        notop,
        minor,
        msg.getId()
    );
  }

  /**
   *
   * @param msg
   * @param newGrp
   * @param moveBy
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void moveTopic(MessageDto msg, GroupDto newGrp, UserDto moveBy) {
    String url = msg.getUrl();

    jdbcTemplate.update("UPDATE topics SET groupid=?,lastmod=CURRENT_TIMESTAMP WHERE id=?", newGrp.getId(), msg.getId());

    if (!newGrp.isLinksAllowed() && !newGrp.isImagePostAllowed()) {
      jdbcTemplate.update("UPDATE topics SET linktext=null, url=null WHERE id=?", msg.getId());

      String title = msg.getGroupTitle();
      String linktext = msg.getLinktext();

      /* if url is not null, update the topic text */
      String link;

      if (!Strings.isNullOrEmpty(url)) {
        if (msg.isLorcode()) {
          link = "\n[url=" + url + ']' + linktext + "[/url]\n";
        } else {
          link = "<br><a href=\"" + url + "\">" + linktext + "</a>\n<br>\n";
        }
      } else {
        link = "";
      }

      String add;

      if (msg.isLorcode()) {
        add = '\n' + link + "\n[i]Перемещено " + moveBy.getNick() + " из " + title + "[/i]\n";
      } else {
        add = '\n' + link + "<br><i>Перемещено " + moveBy.getNick() + " из " + title + "</i>\n";
      }

      jdbcTemplate.update("UPDATE msgbase SET message=message||? WHERE id=?", add, msg.getId());
    }

    if (!newGrp.isModerated()) {
      ImmutableList<String> oldTags = tagCloudDao.getMessageTags(msg.getId());
      tagCloudDao.updateTags(msg.getId(), ImmutableList.<String>of());
      tagCloudDao.updateCounters(oldTags, Collections.<String>emptyList());
    }
  }
}
