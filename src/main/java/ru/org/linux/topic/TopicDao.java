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

package ru.org.linux.topic;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.google.common.collect.ImmutableList.Builder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.edithistory.EditHistoryDto;
import ru.org.linux.edithistory.EditHistoryObjectTypeEnum;
import ru.org.linux.edithistory.EditHistoryService;
import ru.org.linux.gallery.ImageDao;
import ru.org.linux.gallery.Screenshot;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.poll.Poll;
import ru.org.linux.poll.PollDao;
import ru.org.linux.poll.PollNotFoundException;
import ru.org.linux.poll.PollVariant;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionNotFoundException;
import ru.org.linux.section.SectionScrollModeEnum;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.DeleteInfo;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.site.ScriptErrorException;
import ru.org.linux.spring.Configuration;
import ru.org.linux.spring.dao.DeleteInfoDao;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.tag.TagService;
import ru.org.linux.user.*;
import ru.org.linux.util.LorHttpUtils;

import javax.annotation.Nonnull;
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
public class TopicDao {
  private static final Log logger = LogFactory.getLog(TopicDao.class);

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private PollDao pollDao;

  @Autowired
  private TagService tagService;

  @Autowired
  private TopicTagService topicTagService;

  @Autowired
  private UserEventService userEventService;

  @Autowired
  private SectionService sectionService;

  @Autowired
  private Configuration configuration;

  @Autowired
  private MsgbaseDao msgbaseDao;

  @Autowired
  private DeleteInfoDao deleteInfoDao;

  @Autowired
  private UserTagService userTagService;

  @Autowired
  private EditHistoryService editHistoryService;

  @Autowired
  private ImageDao imageDao;

  /**
   * Запрос получения полной информации о топике
   */
  private static final String queryMessage = "SELECT " +
        "postdate, topics.id as msgid, userid, topics.title, " +
        "topics.groupid as guid, topics.url, topics.linktext, ua_id, " +
        "urlname, havelink, section, topics.sticky, topics.postip, " +
        "postdate<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby, " +
        "commitdate, topics.stat1, postscore, topics.moderate, notop, " +
        "topics.resolved, restrict_comments, minor " +
        "FROM topics " +
        "INNER JOIN groups ON (groups.id=topics.groupid) " +
        "INNER JOIN sections ON (sections.id=groups.section) " +
        "WHERE topics.id=?";
  /**
   * Удаление топика
   */
  private static final String updateDeleteMessage = "UPDATE topics SET deleted='t',sticky='f' WHERE id=?";

  private static final String queryTags = "SELECT tags_values.value FROM tags, tags_values WHERE tags.msgid=? AND tags_values.id=tags.tagid ORDER BY value";

  private static final String updateUndeleteMessage = "UPDATE topics SET deleted='f' WHERE id=?";
  private static final String updateUneleteInfo = "DELETE FROM del_info WHERE msgid=?";

  private static final String queryTopicsIdByTime = "SELECT id FROM topics WHERE postdate>=? AND postdate<?";

  private static final String queryTimeFirstTopic = "SELECT min(postdate) FROM topics WHERE postdate!='epoch'::timestamp";

  private static final String updateLastmodToCurrentTime = "UPDATE topics SET lastmod=now() WHERE id=?";



  private JdbcTemplate jdbcTemplate;
  private NamedParameterJdbcTemplate namedJdbcTemplate;

  @Autowired
  private UserDao userDao;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
    namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
  }

  /**
   * Время создания первого топика
   * @return время
   */
  public Timestamp getTimeFirstTopic() {
    return jdbcTemplate.queryForObject(queryTimeFirstTopic, Timestamp.class);
  }

  /**
   * Обновление времени последнего изменения топика.
   *
   * @param topicId идентификационный номер топика
   */
  public void updateLastModifiedToCurrentTime(int topicId) {
    jdbcTemplate.update(
      updateLastmodToCurrentTime,
      topicId
    );
  }

  /**
   * Получить сообщение по id
   * @param id id нужного сообщения
   * @return сообщение
   * @throws MessageNotFoundException при отсутствии сообщения
   */
  @Nonnull
  public Topic getById(int id) throws MessageNotFoundException {
    Topic message;
    try {
      message = jdbcTemplate.queryForObject(queryMessage, new RowMapper<Topic>() {
        @Override
        public Topic mapRow(ResultSet resultSet, int i) throws SQLException {
          return new Topic(resultSet);
        }
      }, id);
    } catch (EmptyResultDataAccessException exception) {
      //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
      throw new MessageNotFoundException(id);
    }
    return message;
  }

  /**
   * Получить group message
   * @param message message
   * @return group
   */
  public Group getGroup(Topic message) {
    return groupDao.getGroup(message.getGroupId());
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
   * Получить тэги топика
   * TODO возможно надо сделать TagDao ?
   * @param message топик
   * @return список тэгов
   */
  public ImmutableList<String> getTags(Topic message) {
    final Builder<String> tags = ImmutableList.builder();

    jdbcTemplate.query(queryTags, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet resultSet) throws SQLException {
        tags.add(resultSet.getString("value"));
      }
    }, message.getId());

    return tags.build();
  }

  @Nonnull
  public ImmutableListMultimap<Integer, String> getTags(@Nonnull List<Topic> topics) {
    if (topics.isEmpty()) {
      return ImmutableListMultimap.of();
    }

    ArrayList<Integer> topicIds = Lists.newArrayList(
            Iterables.transform(topics, new Function<Topic, Integer>() {
              @Override
              public Integer apply(Topic topic) {
                return topic.getId();
              }
            })
    );

    final ImmutableListMultimap.Builder<Integer, String> tags = ImmutableListMultimap.builder();

    namedJdbcTemplate.query(
            "SELECT msgid, tags_values.value FROM tags, tags_values WHERE tags.msgid in (:list) AND tags_values.id=tags.tagid ORDER BY value",
            ImmutableMap.of("list", topicIds),
            new RowCallbackHandler() {
              @Override
              public void processRow(ResultSet resultSet) throws SQLException {
                tags.put(resultSet.getInt("msgid"), resultSet.getString("value"));
              }
            });

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
  public void deleteWithBonus(Topic message, User user, String reason, int bonus) throws UserErrorException {
    jdbcTemplate.update(updateDeleteMessage, message.getId());
    if (user.isModerator() && bonus!=0 && user.getId()!=message.getUid()) {
      if (bonus>20 || bonus<0) {
        throw new UserErrorException("Некорректное значение bonus");
      }
      userDao.changeScore(message.getUid(), -bonus);
    }

    deleteInfoDao.insert(message.getId(), user, reason, -bonus);
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void undelete(Topic message) {
    DeleteInfo deleteInfo = deleteInfoDao.getDeleteInfo(message.getId(), true);

    if (deleteInfo!=null && deleteInfo.getBonus()!=0) {
      userDao.changeScore(message.getUid(), -deleteInfo.getBonus());
    }

    jdbcTemplate.update(updateUndeleteMessage, message.getId());
    jdbcTemplate.update(updateUneleteInfo, message.getId());
  }

  private int allocateMsgid() {
    return jdbcTemplate.queryForInt("select nextval('s_msgid') as msgid");
  }

  /**
   * Сохраняем новое сообщение
   *
   *
   * @param msg
   * @param request
   * @param user
   * @return
   * @throws IOException
   * @throws ScriptErrorException
   */
// call in @Transactional environment
  private int saveNewMessage(
          final Topic msg,
          final HttpServletRequest request,
          final User user,
          String text
  ) {
    final Group group = groupDao.getGroup(msg.getGroupId());

    final int msgid = allocateMsgid();

    String url = msg.getUrl();
    String linktext = msg.getLinktext();

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
            msgid, text, true
    );

    String logmessage = "Написана тема " + msgid + ' ' + LorHttpUtils.getRequestIP(request);
    logger.info(logmessage);

    return msgid;
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public int addMessage(
          HttpServletRequest request,
          AddTopicRequest form,
          String message,
          Group group,
          User user,
          Screenshot scrn,
          Topic previewMsg,
          Set<User> userRefs
  ) throws IOException, ScriptErrorException {
    final int msgid = saveNewMessage(
            previewMsg,
            request,
            user,
            message
    );

    Section section = sectionService.getSection(group.getSectionId());

    if (section.isImagepost() && scrn == null) {
      throw new ScriptErrorException("scrn==null!?");
    }

    if (scrn!=null) {
      Screenshot screenShot = scrn.moveTo(configuration.getHTMLPathPrefix() + "/gallery", Integer.toString(msgid));

      imageDao.saveImage(
              msgid,
              "gallery/" + screenShot.getMainFile().getName(),
              "gallery/" + screenShot.getIconFile().getName()
      );
    }

    if (section.isPollPostAllowed()) {
      pollDao.createPoll(Arrays.asList(form.getPoll()), form.isMultiSelect(), msgid);
    }

    if (!userRefs.isEmpty()) {
      userEventService.addUserRefEvent(userRefs.toArray(new User[userRefs.size()]), msgid);
    }

    if (form.getTags() != null) {
      List<String> tags = tagService.parseSanitizeTags(form.getTags());

      topicTagService.updateTags(msgid, tags);
      tagService.updateCounters(Collections.<String>emptyList(), tags);

      // оповещение пользователей по тегам
      List<Integer> userIdListByTags = userTagService.getUserIdListByTags(user, tags);

      List<Integer> userRefIds = new ArrayList<>();
      for (User userRef: userRefs) {
        userRefIds.add(userRef.getId());
      }

      // не оповещать пользователей. которые ранее были оповещены через упоминание
      Iterator<Integer> userTagIterator = userIdListByTags.iterator();

      while (userTagIterator.hasNext()) {
        Integer userId = userTagIterator.next();
        if (userRefIds.contains(userId)) {
          userTagIterator.remove();
        }
      }
      userEventService.addUserTagEvent(userIdListByTags, msgid);
    }

    return msgid;
  }

  private boolean updateMessage(Topic oldMsg, Topic msg, User editor, List<String> newTags, String newText) {
    List<String> oldTags = topicTagService.getMessageTags(msg.getId());

    EditHistoryDto editHistoryDto = new EditHistoryDto();

    editHistoryDto.setMsgid(msg.getId());
    editHistoryDto.setObjectType(EditHistoryObjectTypeEnum.TOPIC);
    editHistoryDto.setEditor(editor.getId());

    boolean modified = false;

    String oldText = msgbaseDao.getMessageText(msg.getId()).getText();

    if (!oldText.equals(newText)) {
      editHistoryDto.setOldmessage(oldText);
      modified = true;

      msgbaseDao.updateMessage(msg.getId(), newText);
    }

    if (!oldMsg.getTitle().equals(msg.getTitle())) {
      modified = true;
      editHistoryDto.setOldtitle(oldMsg.getTitle());

      namedJdbcTemplate.update(
        "UPDATE topics SET title=:title WHERE id=:id",
        ImmutableMap.of("title", msg.getTitle(), "id", msg.getId())
      );
    }

    if (!equalStrings(oldMsg.getLinktext(), msg.getLinktext())) {
      modified = true;
      editHistoryDto.setOldlinktext(oldMsg.getLinktext());

      namedJdbcTemplate.update(
        "UPDATE topics SET linktext=:linktext WHERE id=:id",
        ImmutableMap.of("linktext", msg.getLinktext(), "id", msg.getId())
      );
    }

    if (!equalStrings(oldMsg.getUrl(), msg.getUrl())) {
      modified = true;
      editHistoryDto.setOldurl(oldMsg.getUrl());

      namedJdbcTemplate.update(
        "UPDATE topics SET url=:url WHERE id=:id",
        ImmutableMap.of("url", msg.getUrl(), "id", msg.getId())
      );
    }

    if (newTags != null) {
      boolean modifiedTags = topicTagService.updateTags(msg.getId(), newTags);

      if (modifiedTags) {
        editHistoryDto.setOldtags(TagService.toString(oldTags));
        tagService.updateCounters(oldTags, newTags);
        modified = true;
      }
    }

    if (oldMsg.isMinor() != msg.isMinor()) {
      namedJdbcTemplate.update("UPDATE topics SET minor=:minor WHERE id=:id",
              ImmutableMap.of("minor", msg.isMinor(), "id", msg.getId()));

      editHistoryDto.setOldminor(oldMsg.isMinor());

      modified = true;
    }

    if (modified) {
      editHistoryService.insert(editHistoryDto);
    }

    return modified;
  }

  private static boolean equalStrings(String s1, String s2) {
    if (Strings.isNullOrEmpty(s1)) {
      return Strings.isNullOrEmpty(s2);
    }

    return s1.equals(s2);
  }

  private boolean updatePoll(Topic message, List<PollVariant> newVariants, boolean multiselect) throws PollNotFoundException {
    boolean modified = false;

    final Poll poll = pollDao.getPollByTopicId(message.getId());

    ImmutableList<PollVariant> oldVariants = poll.getVariants();

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
      if (var.getId()==0 && !Strings.isNullOrEmpty(var.getLabel())) {
        modified = true;

        pollDao.addNewVariant(poll, var.getLabel());
      }
    }

    if (poll.isMultiSelect()!=multiselect) {
      modified = true;
      pollDao.updateMultiselect(poll, multiselect);
    }

    return modified;
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public boolean updateAndCommit(
          Topic newMsg,
          Topic oldMsg,
          User user,
          List<String> newTags,
          String newText,
          boolean commit,
          Integer changeGroupId,
          int bonus,
          List<PollVariant> pollVariants,
          boolean multiselect,
          Map<Integer, Integer> editorBonus
  )  {
    boolean modified = updateMessage(oldMsg, newMsg, user, newTags, newText);

    try {
      if (pollVariants!=null && updatePoll(oldMsg, pollVariants, multiselect)) {
        modified = true;
      }
    } catch (PollNotFoundException e) {
      throw new RuntimeException(e);
    }

    if (commit) {
      if (changeGroupId != null) {
        if (oldMsg.getGroupId() != changeGroupId) {
          jdbcTemplate.update("UPDATE topics SET groupid=? WHERE id=?", changeGroupId, oldMsg.getId());
          jdbcTemplate.update("UPDATE groups SET stat3=stat3+1 WHERE id=? or id=?", oldMsg.getGroupId(), changeGroupId);
        }
      }

      commit(oldMsg, user, bonus, editorBonus);
    }

    if (modified) {
      logger.info("сообщение " + oldMsg.getId() + " исправлено " + user.getNick());
    }

    return modified;
  }

  private void commit(Topic msg, User commiter, int bonus, Map<Integer, Integer> editorBonus) {
    if (bonus < 0 || bonus > 20) {
      throw new IllegalStateException("Неверное значение bonus");
    }

    jdbcTemplate.update(
            "UPDATE topics SET moderate='t', commitby=?, commitdate='now' WHERE id=?",
            commiter.getId(),
            msg.getId()
    );

    userDao.changeScore(msg.getUid(), bonus);

    if (editorBonus!=null) {
      for (Map.Entry<Integer, Integer> entry : editorBonus.entrySet()) {
        userDao.changeScore(entry.getKey(), entry.getValue());
      }
    }
  }

  public void uncommit(Topic msg) {
    jdbcTemplate.update("UPDATE topics SET moderate='f',commitby=NULL,commitdate=NULL WHERE id=?", msg.getId());
  }

  public Topic getPreviousMessage(Topic message, User currentUser) {
    if (message.isSticky()) {
      return null;
    }

    SectionScrollModeEnum sectionScrollMode;

    try {
      sectionScrollMode = sectionService.getScrollMode(message.getSectionId());
    } catch (SectionNotFoundException e) {
      logger.error(e);
      return null;
    }

    List<Integer> res;

    switch (sectionScrollMode) {
      case SECTION:
        res = jdbcTemplate.queryForList(
                "SELECT topics.id as msgid " +
                        "FROM topics " +
                        "WHERE topics.commitdate=(SELECT max(commitdate) FROM topics, groups, sections WHERE sections.id=groups.section AND topics.commitdate<? AND topics.groupid=groups.id AND groups.section=? AND (topics.moderate OR NOT sections.moderate) AND NOT deleted AND not sticky)",
                Integer.class,
                message.getCommitDate(),
                message.getSectionId()
        );
        break;

      case GROUP:
        if (currentUser == null || currentUser.isAnonymous()) {
          res = jdbcTemplate.queryForList(
                  "SELECT max(topics.id) as msgid " +
                          "FROM topics " +
                          "WHERE topics.id<? AND topics.groupid=? AND NOT deleted AND NOT sticky",
                  Integer.class,
                  message.getId(),
                  message.getGroupId()
          );
        } else {
            res = jdbcTemplate.queryForList(
                    "SELECT max(topics.id) as msgid " +
                            "FROM topics " +
                            "WHERE topics.id<? AND topics.groupid=? AND NOT deleted AND NOT sticky " +
                            "AND userid NOT IN (select ignored from ignore_list where userid=?)",
                    Integer.class,
                    message.getId(),
                    message.getGroupId(),
                    currentUser.getId()
            );
        }

        break;

      case NO_SCROLL:
      default:
        return null;
    }

    try {
      if (res.isEmpty() || res.get(0)==null) {
        return null;
      }

      int prevMsgid = res.get(0);

      return getById(prevMsgid);
    } catch (MessageNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public Topic getNextMessage(Topic message, User currentUser) {
    if (message.isSticky()) {
      return null;
    }

    SectionScrollModeEnum sectionScrollMode;

    try {
      sectionScrollMode = sectionService.getScrollMode(message.getSectionId());
    } catch (SectionNotFoundException e) {
      logger.error(e);
      return null;
    }

    List<Integer> res;

    switch (sectionScrollMode) {
      case SECTION:
        res = jdbcTemplate.queryForList(
                "SELECT topics.id as msgid " +
                        "FROM topics " +
                        "WHERE topics.commitdate=" +
                        "(SELECT min(commitdate) FROM topics, groups, sections WHERE sections.id=groups.section AND topics.commitdate>? AND topics.groupid=groups.id AND groups.section=? AND (topics.moderate OR NOT sections.moderate) AND NOT deleted AND NOT sticky)",
                Integer.class,
                message.getCommitDate(),
                message.getSectionId()
        );
        break;

      case GROUP:
        if (currentUser == null || currentUser.isAnonymous()) {
          res = jdbcTemplate.queryForList(
                  "SELECT min(topics.id) as msgid " +
                          "FROM topics " +
                          "WHERE topics.id>? AND topics.groupid=? AND NOT deleted AND NOT sticky",
                  Integer.class,
                  message.getId(),
                  message.getGroupId()
          );
        } else {
          res = jdbcTemplate.queryForList(
                  "SELECT min(topics.id) as msgid " +
                          "FROM topics " +
                          "WHERE topics.id>? AND topics.groupid=? AND NOT deleted AND NOT sticky " +
                          "AND userid NOT IN (select ignored from ignore_list where userid=?)",
                  Integer.class,
                  message.getId(),
                  message.getGroupId(),
                  currentUser.getId()
          );
        }
        break;

      case NO_SCROLL:
      default:
        return null;
    }

    try {
      if (res.isEmpty() || res.get(0)==null) {
        return null;
      }

      int nextMsgid = res.get(0);

      return getById(nextMsgid);
    } catch (MessageNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public void resolveMessage(int msgid, boolean b) {
    jdbcTemplate.update(
            "UPDATE topics SET resolved=?,lastmod=lastmod+'1 second'::interval WHERE id=?",
            b,
            msgid
    );
  }

  public void setTopicOptions(Topic msg, int postscore, boolean sticky, boolean notop) {
    jdbcTemplate.update(
            "UPDATE topics SET postscore=?, sticky=?, notop=?, lastmod=CURRENT_TIMESTAMP WHERE id=?",
            postscore,
            sticky,
            notop,
            msg.getId()
    );
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void moveTopic(Topic msg, Group newGrp, User moveBy) {
    String url = msg.getUrl();

    int oldId = jdbcTemplate.queryForInt("SELECT groupid FROM topics WHERE id=? FOR UPDATE", msg.getId());

    if (oldId==newGrp.getId()) {
      return;
    }

    boolean lorcode = msgbaseDao.getMessageText(msg.getId()).isLorcode();

    jdbcTemplate.update("UPDATE topics SET groupid=?,lastmod=CURRENT_TIMESTAMP WHERE id=?", newGrp.getId(), msg.getId());

    if (!newGrp.isLinksAllowed()) {
      jdbcTemplate.update("UPDATE topics SET linktext=null, url=null WHERE id=?", msg.getId());

      String title = msg.getGroupUrl();
      String linktext = msg.getLinktext();

      /* if url is not null, update the topic text */
      String link;

      if (!Strings.isNullOrEmpty(url)) {
        if (lorcode) {
          link = "\n[url=" + url + ']' + linktext + "[/url]\n";
        } else {
          link = "<br><a href=\"" + url + "\">" + linktext + "</a>\n<br>\n";
        }
      } else {
        link = "";
      }

      String add;

      if (lorcode) {
        add = '\n' + link + "\n\n[i]Перемещено " + moveBy.getNick() + " из " + title + "[/i]\n";
      } else {
        add = '\n' + link + "<br><i>Перемещено " + moveBy.getNick() + " из " + title + "</i>\n";
      }

      msgbaseDao.appendMessage(msg.getId(), add);
    }

    logger.info("topic " + msg.getId() + " moved" +
          " by " + moveBy.getNick() + " from news/forum " + msg.getGroupUrl() + " to forum " + newGrp.getTitle());
  }
  /**
   * Массовое удаление всех топиков пользователя.
   *
   * @param user      пользователь для экзекуции
   * @param moderator экзекутор-модератор
   * @return список удаленных топиков
   * @throws UserNotFoundException генерирует исключение если пользователь отсутствует
   */
  public List<Integer> deleteAllByUser(User user, final User moderator) {
    final List<Integer> deletedTopicIds = new ArrayList<>();
    // Удаляем все топики
    jdbcTemplate.query("SELECT id FROM topics WHERE userid=? AND not deleted FOR UPDATE",
      new RowCallbackHandler() {
        @Override
        public void processRow(ResultSet rs) throws SQLException {
          int mid = rs.getInt("id");
          jdbcTemplate.update("UPDATE topics SET deleted='t',sticky='f' WHERE id=?", mid);
          deleteInfoDao.insert(mid, moderator, "Блокировка пользователя с удалением сообщений", 0);
          deletedTopicIds.add(mid);
        }
      },
      user.getId());
    return deletedTopicIds;
  }

}
