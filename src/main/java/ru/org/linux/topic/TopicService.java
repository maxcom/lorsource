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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.gallery.ImageDao;
import ru.org.linux.gallery.Screenshot;
import ru.org.linux.group.Group;
import ru.org.linux.poll.PollDao;
import ru.org.linux.poll.PollNotFoundException;
import ru.org.linux.poll.PollVariant;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.ScriptErrorException;
import ru.org.linux.spring.SiteConfig;
import ru.org.linux.spring.dao.DeleteInfoDao;
import ru.org.linux.tag.TagService;
import ru.org.linux.user.*;
import ru.org.linux.util.LorHttpUtils;
import ru.org.linux.util.bbcode.LorCodeService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

import static com.google.common.base.Predicates.*;

@Service
public class TopicService {
  private static final Log logger = LogFactory.getLog(TopicService.class);

  @Autowired
  private TopicDao topicDao;

  @Autowired
  private SectionService sectionService;

  @Autowired
  private SiteConfig siteConfig;

  @Autowired
  private ImageDao imageDao;

  @Autowired
  private PollDao pollDao;

  @Autowired
  private UserEventService userEventService;

  @Autowired
  private TagService tagService;

  @Autowired
  private TopicTagService topicTagService;

  @Autowired
  private UserTagService userTagService;

  @Autowired
  private UserDao userDao;

  @Autowired
  private DeleteInfoDao deleteInfoDao;

  @Autowired
  private LorCodeService lorCodeService;

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public int addMessage(
          HttpServletRequest request,
          AddTopicRequest form,
          String message,
          Group group,
          User user,
          Screenshot scrn,
          Topic previewMsg
  ) throws IOException, ScriptErrorException {
    final int msgid = topicDao.saveNewMessage(
            previewMsg,
            user,
            message,
            request.getHeader("User-Agent"),
            group
    );

    Section section = sectionService.getSection(group.getSectionId());

    if (section.isImagepost() && scrn == null) {
      throw new ScriptErrorException("scrn==null!?");
    }

    if (scrn!=null) {
      Screenshot screenShot = scrn.moveTo(siteConfig.getHTMLPathPrefix() + "/gallery", Integer.toString(msgid));

      imageDao.saveImage(
              msgid,
              "gallery/" + screenShot.getMainFile().getName(),
              "gallery/" + screenShot.getIconFile().getName()
      );
    }

    if (section.isPollPostAllowed()) {
      pollDao.createPoll(Arrays.asList(form.getPoll()), form.isMultiSelect(), msgid);
    }

    List<String> tags = tagService.parseSanitizeTags(form.getTags());

    topicTagService.updateTags(msgid, tags);
    tagService.updateCounters(ImmutableList.<String>of(), tags);

    if (!previewMsg.isDraft()) {
      if (section.isPremoderated()) {
        sendEvents(message, msgid, ImmutableList.<String>of(), user.getId());
      } else {
        sendEvents(message, msgid, tags, user.getId());
      }
    }

    String logmessage = "Написана тема " + msgid + ' ' + LorHttpUtils.getRequestIP(request);
    logger.info(logmessage);

    return msgid;
  }

  /**
   * Отправляет уведомления типа REF (ссылка на пользователя) и TAG (уведомление по тегу)
   *
   * @param message текст сообщения
   * @param msgid идентификатор сообщения
   * @param author автор сообщения (ему не будет отправлено уведомление)
   */
  private void sendEvents(String message, int msgid, List<String> tags, int author) {
    Set<Integer> notifiedUsers = userEventService.getNotifiedUsers(msgid);

    Set<User> userRefs = lorCodeService.getReplierFromMessage(message);

    // оповещение пользователей по тегам
    List<Integer> userIdListByTags = userTagService.getUserIdListByTags(author, tags);

    final Set<Integer> userRefIds = new HashSet<>();
    for (User userRef : userRefs) {
      if (!notifiedUsers.contains(userRef.getId())) {
        userRefIds.add(userRef.getId());
      }
    }

    // не оповещать пользователей. которые ранее были оповещены через упоминание
    Iterable<Integer> tagUsers = Iterables.filter(
            userIdListByTags,
            not(or(in(userRefIds), in(notifiedUsers)))
    );

    userEventService.addUserRefEvent(userRefIds, msgid);
    userEventService.addUserTagEvent(tagUsers, msgid);
  }

  /**
   * Удаление топика и если удаляет модератор изменить автору score
   * @param message удаляемый топик
   * @param user удаляющий пользователь
   * @param reason причина удаления
   * @param bonus дельта изменения score автора топика
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void deleteWithBonus(Topic message, User user, String reason, int bonus) {
    if (bonus>20 || bonus<0) {
      throw new IllegalArgumentException("Некорректное значение bonus");
    }

    if (user.isModerator() && bonus!=0 && user.getId()!=message.getUid() && !message.isDraft()) {
      boolean deleted = deleteTopic(message.getId(), user, reason, -bonus);

      if (deleted) {
        userDao.changeScore(message.getUid(), -bonus);
      }
    } else {
      deleteTopic(message.getId(), user, reason, 0);
    }
  }

  private boolean deleteTopic(int mid, User moderator, String reason, int bonus) {
    boolean deleted = topicDao.delete(mid);

    if (deleted) {
      deleteInfoDao.insert(mid, moderator, reason, bonus);
      userEventService.processTopicDeleted(ImmutableList.of(mid));
    }

    return deleted;
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public List<Integer> deleteByIPAddress(String ip, Timestamp startTime, User moderator, String reason) {
    List<Integer> topicIds = topicDao.getAllByIPForUpdate(ip, startTime);

    return massDelete(moderator, topicIds, reason);
  }

  /**
   * Массовое удаление всех топиков пользователя.
   *
   * @param user      пользователь для экзекуции
   * @param moderator экзекутор-модератор
   * @return список удаленных топиков
   * @throws UserNotFoundException генерирует исключение если пользователь отсутствует
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public List<Integer> deleteAllByUser(User user, User moderator) {
    List<Integer> topics = topicDao.getUserTopicForUpdate(user);

    return massDelete(moderator, topics, "Блокировка пользователя с удалением сообщений");
  }

  private List<Integer> massDelete(User moderator, Iterable<Integer> topics, String reason) {
    List<Integer> deletedTopics = new ArrayList<>();

    for (int mid : topics) {
      boolean deleted = topicDao.delete(mid);

      if (deleted) {
        deleteInfoDao.insert(mid, moderator, reason, 0);
        deletedTopics.add(mid);
      }
    }

    userEventService.processTopicDeleted(deletedTopics);

    return deletedTopics;
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
    boolean modified = topicDao.updateMessage(oldMsg, newMsg, user, newTags, newText);

    if ((modified || commit) && !newMsg.isDraft()) {
      Section section = sectionService.getSection(oldMsg.getSectionId());

      if (section.isPremoderated() && !oldMsg.isCommited() && !commit) {
        sendEvents(newText, oldMsg.getId(), ImmutableList.<String>of(), oldMsg.getUid());
      } else {
        sendEvents(newText, oldMsg.getId(), newTags, oldMsg.getUid());
      }
    }

    try {
      if (pollVariants!=null && pollDao.updatePoll(oldMsg, pollVariants, multiselect)) {
        modified = true;
      }
    } catch (PollNotFoundException e) {
      throw new RuntimeException(e);
    }

    if (oldMsg.isDraft() && !newMsg.isDraft()) {
      topicDao.publish(newMsg);
    }

    if (commit) {
      if (changeGroupId != null) {
        if (oldMsg.getGroupId() != changeGroupId) {
          topicDao.changeGroup(oldMsg, changeGroupId);
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

    if (msg.isDraft()) {
      topicDao.publish(msg);
    }

    topicDao.commit(msg, commiter);

    userDao.changeScore(msg.getUid(), bonus);

    if (editorBonus!=null) {
      for (Map.Entry<Integer, Integer> entry : editorBonus.entrySet()) {
        userDao.changeScore(entry.getKey(), entry.getValue());
      }
    }
  }
}
