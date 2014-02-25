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

package ru.org.linux.topic;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.edithistory.EditHistoryDto;
import ru.org.linux.edithistory.EditHistoryObjectTypeEnum;
import ru.org.linux.edithistory.EditHistoryService;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.section.SectionScrollModeEnum;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.DeleteInfo;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.spring.dao.DeleteInfoDao;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.tag.TagModificationService;
import ru.org.linux.tag.TagService;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

/**
 * Операции над сообщениями
 */

@Repository
public class TopicDao {
  private static final Logger logger = LoggerFactory.getLogger(TopicDao.class);

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private TagModificationService tagService;

  @Autowired
  private TopicTagService topicTagService;

  @Autowired
  private SectionService sectionService;

  @Autowired
  private MsgbaseDao msgbaseDao;

  @Autowired
  private DeleteInfoDao deleteInfoDao;

  @Autowired
  private EditHistoryService editHistoryService;

  /**
   * Запрос получения полной информации о топике
   */
  private static final String queryMessage = "SELECT " +
        "postdate, topics.id as msgid, userid, topics.title, " +
        "topics.groupid as guid, topics.url, topics.linktext, ua_id, " +
        "urlname, section, topics.sticky, topics.postip, " +
        "postdate<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby, " +
        "commitdate, topics.stat1, postscore, topics.moderate, notop, " +
        "topics.resolved, minor, draft " +
        "FROM topics " +
        "INNER JOIN groups ON (groups.id=topics.groupid) " +
        "INNER JOIN sections ON (sections.id=groups.section) " +
        "WHERE topics.id=?";
  /**
   * Удаление топика
   */
  private static final String updateUndeleteMessage = "UPDATE topics SET deleted='f' WHERE id=?";
  private static final String updateUneleteInfo = "DELETE FROM del_info WHERE msgid=?";

  private static final String queryTopicsIdByTime = "SELECT id FROM topics WHERE postdate>=? AND postdate<?";

  private static final String queryTimeFirstTopic = "SELECT min(postdate) FROM topics WHERE postdate!='epoch'::timestamp";

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
  public void updateLastmod(int topicId, boolean bump) {
    if (bump) {
      jdbcTemplate.update("UPDATE topics SET lastmod=now() WHERE id=?", topicId);
    } else {
      jdbcTemplate.update("UPDATE topics SET lastmod=lastmod+'1 second'::interval WHERE id=?", topicId);
    }
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
   *
   * @param message топик
   * @return список тэгов
   */
  public List<String> getTags(Topic message) {
    return topicTagService.getTags(message.getId());
  }

  public boolean delete(int msgid) {
    return jdbcTemplate.update("UPDATE topics SET deleted='t',sticky='f' WHERE id=? AND NOT deleted", msgid)>0;
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
    return jdbcTemplate.queryForObject("select nextval('s_msgid') as msgid", Integer.class);
  }

  /**
   * Сохраняем новое сообщение
   *
   * @return msgid
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public int saveNewMessage(
          final Topic msg,
          final User user,
          String text,
          final String userAgent,
          final Group group
  ) {
    final int msgid = allocateMsgid();

    String url = msg.getUrl();
    String linktext = msg.getLinktext();

    final String finalUrl = url;
    final String finalLinktext = linktext;
    jdbcTemplate.execute(
            "INSERT INTO topics (groupid, userid, title, url, moderate, postdate, id, linktext, deleted, ua_id, postip, draft) VALUES (?, ?, ?, ?, 'f', CURRENT_TIMESTAMP, ?, ?, 'f', create_user_agent(?),?::inet, ?)",
            new PreparedStatementCallback<String>() {
              @Override
              public String doInPreparedStatement(PreparedStatement pst) throws SQLException, DataAccessException {
                pst.setInt(1, group.getId());
                pst.setInt(2, user.getId());
                pst.setString(3, msg.getTitle());
                pst.setString(4, finalUrl);
                pst.setInt(5, msgid);
                pst.setString(6, finalLinktext);
                pst.setString(7, userAgent);
                pst.setString(8, msg.getPostIP());
                pst.setBoolean(9, msg.isDraft());
                pst.executeUpdate();

                return null;
              }
            }
    );

    msgbaseDao.saveNewMessage(text, msgid);

    return msgid;
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public boolean updateMessage(Topic oldMsg, Topic msg, User editor, List<String> newTags, String newText) {
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
      List<String> oldTags = topicTagService.getTags(msg.getId());

      boolean modifiedTags = topicTagService.updateTags(msg.getId(), oldTags, newTags);

      if (modifiedTags) {
        editHistoryDto.setOldtags(TagService.tagsToString(oldTags));
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
      updateLastmod(msg.getId(), false);
    }

    return modified;
  }

  public static boolean equalStrings(String s1, String s2) {
    if (Strings.isNullOrEmpty(s1)) {
      return Strings.isNullOrEmpty(s2);
    }

    return s1.equals(s2);
  }

  public void commit(Topic msg, User commiter) {
    jdbcTemplate.update(
            "UPDATE topics SET moderate='t', commitby=?, commitdate=CURRENT_TIMESTAMP, lastmod=CURRENT_TIMESTAMP WHERE id=?",
            commiter.getId(),
            msg.getId()
    );
  }

  public void publish(Topic msg) {
    jdbcTemplate.update(
            "UPDATE topics SET draft='f',postdate=CURRENT_TIMESTAMP,lastmod=CURRENT_TIMESTAMP WHERE id=? AND draft",
            msg.getId()
    );
  }

  public void uncommit(Topic msg) {
    jdbcTemplate.update("UPDATE topics SET moderate='f',commitby=NULL,commitdate=NULL WHERE id=?", msg.getId());
  }

  public Topic getPreviousMessage(Topic message, User currentUser) {
    if (message.isSticky()) {
      return null;
    }

    SectionScrollModeEnum sectionScrollMode;

    sectionScrollMode = sectionService.getScrollMode(message.getSectionId());

    List<Integer> res;

    switch (sectionScrollMode) {
      case SECTION:
        res = jdbcTemplate.queryForList(
                "SELECT topics.id as msgid " +
                        "FROM topics " +
                        "WHERE topics.commitdate=" +
                        "(SELECT commitdate FROM topics, groups, sections WHERE NOT draft AND sections.id=groups.section AND topics.commitdate<? AND topics.groupid=groups.id AND groups.section=? AND (topics.moderate OR NOT sections.moderate) AND NOT deleted AND NOT sticky ORDER BY commitdate DESC LIMIT 1)",
                        //"(SELECT max(commitdate) FROM topics, groups, sections WHERE sections.id=groups.section AND topics.commitdate<? AND topics.groupid=groups.id AND groups.section=? AND (topics.moderate OR NOT sections.moderate) AND NOT deleted AND not sticky)",
                Integer.class,
                message.getCommitDate(),
                message.getSectionId()
        );
        break;

      case GROUP:
        if (currentUser == null || currentUser.isAnonymous()) {
          res = jdbcTemplate.queryForList(
                  "SELECT topics.id " +
                          "FROM topics " +
                          "WHERE NOT draft AND topics.postdate<? AND topics.groupid=? AND NOT deleted AND NOT sticky ORDER BY postdate DESC LIMIT 1",
                  Integer.class,
                  message.getPostdate(),
                  message.getGroupId()
          );
        } else {
            res = jdbcTemplate.queryForList(
                    "SELECT topics.id as msgid " +
                            "FROM topics " +
                            "WHERE NOT draft AND topics.postdate<? AND topics.groupid=? AND NOT deleted AND NOT sticky " +
                            "AND userid NOT IN (select ignored from ignore_list where userid=?) ORDER BY postdate DESC LIMIT 1",
                    Integer.class,
                    message.getPostdate(),
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

    sectionScrollMode = sectionService.getScrollMode(message.getSectionId());

    List<Integer> res;

    switch (sectionScrollMode) {
      case SECTION:
        res = jdbcTemplate.queryForList(
                "SELECT topics.id as msgid " +
                        "FROM topics " +
                        "WHERE topics.commitdate=" +
                        "(SELECT commitdate FROM topics, groups, sections WHERE NOT draft AND sections.id=groups.section AND topics.commitdate>? AND topics.groupid=groups.id AND groups.section=? AND (topics.moderate OR NOT sections.moderate) AND NOT deleted AND NOT sticky ORDER BY commitdate ASC LIMIT 1)",
//                        "(SELECT min(commitdate) FROM topics, groups, sections WHERE sections.id=groups.section AND topics.commitdate>? AND topics.groupid=groups.id AND groups.section=? AND (topics.moderate OR NOT sections.moderate) AND NOT deleted AND NOT sticky)",
                Integer.class,
                message.getCommitDate(),
                message.getSectionId()
        );
        break;

      case GROUP:
        if (currentUser == null || currentUser.isAnonymous()) {
          res = jdbcTemplate.queryForList(
                  "SELECT topics.id as msgid " +
                          "FROM topics " +
                          "WHERE NOT draft AND topics.postdate>? AND topics.groupid=? AND NOT deleted AND NOT sticky ORDER BY postdate ASC LIMIT 1",
                  Integer.class,
                  message.getPostdate(),
                  message.getGroupId()
          );
        } else {
          res = jdbcTemplate.queryForList(
                  "SELECT topics.id as msgid " +
                          "FROM topics " +
                          "WHERE NOT draft AND topics.postdate>? AND topics.groupid=? AND NOT deleted AND NOT sticky " +
                          "AND userid NOT IN (select ignored from ignore_list where userid=?) ORDER BY postdate ASC LIMIT 1",
                  Integer.class,
                  message.getPostdate(),
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

  public void changeGroup(Topic msg, int changeGroupId) {
    jdbcTemplate.update("UPDATE topics SET groupid=?,lastmod=CURRENT_TIMESTAMP WHERE id=?", changeGroupId, msg.getId());
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void moveTopic(Topic msg, Group newGrp, User moveBy) {
    String url = msg.getUrl();

    int oldId = jdbcTemplate.queryForObject("SELECT groupid FROM topics WHERE id=? FOR UPDATE", Integer.class, msg.getId());

    if (oldId==newGrp.getId()) {
      return;
    }

    boolean lorcode = msgbaseDao.getMessageText(msg.getId()).isLorcode();

    changeGroup(msg, newGrp.getId());

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

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public List<Integer> getUserTopicForUpdate(User user) {
    return jdbcTemplate.queryForList("SELECT id FROM topics WHERE userid=? AND not deleted FOR UPDATE", Integer.class, user.getId());
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public List<Integer> getAllByIPForUpdate(String ip, Timestamp startTime) {
    return jdbcTemplate.queryForList("SELECT id FROM topics WHERE postip=?::inet AND not deleted AND postdate>? FOR UPDATE",
            Integer.class,
            ip,
            startTime
    );
  }

  public int getUncommitedCount() {
    return jdbcTemplate.queryForObject(
            "select count(*) from topics,groups,sections where section=sections.id AND sections.moderate and not draft and topics.groupid=groups.id and not deleted and not topics.moderate AND postdate>(CURRENT_TIMESTAMP-'1 month'::interval)",
            Integer.class
    );
  }

  public int getUncommitedCount(int section) {
    return jdbcTemplate.queryForObject(
            "select count(*) from topics,groups where section=? AND topics.groupid=groups.id and not deleted and not draft and not topics.moderate AND postdate>(CURRENT_TIMESTAMP-'1 month'::interval)",
            Integer.class,
            section
    );
  }

  public boolean hasDrafts(User author) {
    List<Integer> res = jdbcTemplate.queryForList(
            "select id FROM topics WHERE draft AND userid=? LIMIT 1",
            Integer.class,
            author.getId()
    );

    return !res.isEmpty();
  }
}
