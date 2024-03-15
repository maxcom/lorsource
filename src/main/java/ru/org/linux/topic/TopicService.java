/*
 * Copyright 1998-2024 Linux.org.ru
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

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.edithistory.EditHistoryDao;
import ru.org.linux.edithistory.EditHistoryRecord;
import ru.org.linux.gallery.Image;
import ru.org.linux.gallery.ImageDao;
import ru.org.linux.gallery.ImageService;
import ru.org.linux.gallery.UploadedImagePreview;
import ru.org.linux.group.Group;
import ru.org.linux.markup.MessageTextService;
import ru.org.linux.poll.PollDao;
import ru.org.linux.poll.PollVariant;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.ScriptErrorException;
import ru.org.linux.spring.SiteConfig;
import ru.org.linux.spring.dao.DeleteInfoDao;
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.tag.TagName;
import ru.org.linux.user.*;
import ru.org.linux.util.LorHttpUtils;
import scala.Tuple2;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Predicates.in;

@Service
public class TopicService {
  private static final Logger logger = LoggerFactory.getLogger(TopicService.class);
  private final TopicDao topicDao;
  private final MsgbaseDao msgbaseDao;
  private final SectionService sectionService;
  private final ImageService imageService;
  private final PollDao pollDao;
  private final UserEventService userEventService;
  private final TopicTagService topicTagService;
  private final UserService userService;
  private final UserTagService userTagService;
  private final UserDao userDao;
  private final DeleteInfoDao deleteInfoDao;
  private final MessageTextService textService;
  private final EditHistoryDao editHistoryDao;
  private final ImageDao imageDao;

  public TopicService(TopicDao topicDao, MsgbaseDao msgbaseDao, SectionService sectionService,
                      ImageService imageService, PollDao pollDao, UserEventService userEventService,
                      TopicTagService topicTagService, UserService userService, UserTagService userTagService,
                      UserDao userDao, DeleteInfoDao deleteInfoDao, MessageTextService textService,
                      EditHistoryDao editHistoryDao, ImageDao imageDao) {
    this.topicDao = topicDao;
    this.msgbaseDao = msgbaseDao;
    this.sectionService = sectionService;
    this.imageService = imageService;
    this.pollDao = pollDao;
    this.userEventService = userEventService;
    this.topicTagService = topicTagService;
    this.userService = userService;
    this.userTagService = userTagService;
    this.userDao = userDao;
    this.deleteInfoDao = deleteInfoDao;
    this.textService = textService;
    this.editHistoryDao = editHistoryDao;
    this.imageDao = imageDao;
  }

  @Autowired
  private SiteConfig siteConfig;

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public Tuple2<Integer, Set<Integer>> addMessage(
          HttpServletRequest request,
          AddTopicRequest form,
          MessageText message,
          Group group,
          User user,
          UploadedImagePreview imagePreview,
          Topic previewMsg
  ) throws ScriptErrorException {
    final int msgid = topicDao.saveNewMessage(
            previewMsg,
            user,
            request.getHeader("User-Agent"),
            group
    );

    msgbaseDao.saveNewMessage(message, msgid);

    Section section = sectionService.getSection(group.getSectionId());

    if (section.isImagepost() && imagePreview == null) {
      throw new ScriptErrorException("scrn==null!?");
    }

    if (imagePreview!=null) {
      imageService.saveScreenshot(imagePreview, msgid);
    }

    if (section.isPollPostAllowed()) {
      pollDao.createPoll(Arrays.asList(form.getPoll()), form.isMultiSelect(), msgid);
    }

    List<String> tags = TagName.parseAndSanitizeTagsJava(form.getTags());

    topicTagService.updateTags(msgid, tags);

    Set<Integer> notified;

    if (!previewMsg.isDraft()) {
      if (section.isPremoderated()) {
        notified = sendEvents(message, msgid, List.of(), user.getId());
      } else {
        notified = sendEvents(message, msgid, tags, user.getId());
      }
    } else {
      notified = Set.of();
    }

    String logmessage = "Написана тема " + msgid + ' ' + LorHttpUtils.getRequestIP(request);
    logger.info(logmessage);

    return Tuple2.apply(msgid, notified);
  }

  /**
   * Отправляет уведомления типа REF (ссылка на пользователя) и TAG (уведомление по тегу)
   *
   * @param message текст сообщения
   * @param msgid идентификатор сообщения
   * @param author автор сообщения (ему не будет отправлено уведомление)
   */
  private Set<Integer> sendEvents(MessageText message, int msgid, List<String> tags, int author) {
    Set<Integer> notifiedUsers = userEventService.getNotifiedUsers(msgid);

    Set<User> userRefs = textService.mentions(message);
    userRefs = userRefs.stream()
            .filter(p -> !userService.isIgnoring(p.getId(), author))
            .collect(Collectors.toSet());

    // оповещение пользователей по тегам
    List<Integer> userIdListByTags = userTagService.getUserIdListByTags(author, tags);

    Set<Integer> userRefIds = userRefs
            .stream()
            .map(User::getId)
            .filter(id -> !notifiedUsers.contains(id))
            .collect(Collectors.toSet());

    // Не оповещать пользователей, которые ранее были оповещены через упоминание
    List<Integer> tagUsers = userIdListByTags.stream()
            .filter((in(userRefIds).or(in(notifiedUsers))).negate()).collect(Collectors.toList());

    userEventService.addUserRefEvent(userRefIds, msgid);
    userEventService.addUserTagEvent(tagUsers, msgid);

    HashSet<Integer> notified = new HashSet<>();

    notified.addAll(userRefIds);
    notified.addAll(tagUsers);

    return notified;
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

    if (user.isModerator() && bonus!=0 && user.getId()!=message.getAuthorUserId() && !message.isDraft()) {
      boolean deleted = deleteTopic(message.getId(), user, reason, -bonus);

      if (deleted) {
        userDao.changeScore(message.getAuthorUserId(), -bonus);
      }
    } else {
      deleteTopic(message.getId(), user, reason, 0);
    }
  }

  private boolean deleteTopic(int mid, User moderator, String reason, int bonus) {
    boolean deleted = topicDao.delete(mid);

    if (deleted) {
      deleteInfoDao.insert(mid, moderator, reason, bonus);
      userEventService.processTopicDeleted(List.of(mid));
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

  private static boolean sendTagEventsNeeded(Section section, Topic oldMsg, boolean commit) {
    boolean needCommit = section.isPremoderated() && !oldMsg.isCommited();
    boolean fresh = oldMsg.getEffectiveDate().isAfter(DateTime.now().minusMonths(1));

    return commit || (!needCommit && fresh);
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public Tuple2<Boolean, Set<Integer>> updateAndCommit(
          Topic newMsg,
          Topic oldMsg,
          User user,
          @Nullable List<String> newTags,
          MessageText newText,
          boolean commit,
          Integer changeGroupId,
          int bonus,
          List<PollVariant> pollVariants,
          boolean multiselect,
          Map<Integer, Integer> editorBonus,
          UploadedImagePreview imagePreview
  ) throws IOException {
    EditHistoryRecord editHistoryRecord = new EditHistoryRecord();

    boolean modified = topicDao.updateMessage(editHistoryRecord, oldMsg, newMsg, user, newTags, newText.text(),
            pollVariants, multiselect);

    if (imagePreview!=null) {
      replaceImage(oldMsg, imagePreview, editHistoryRecord);

      modified = true;
    }

    if (modified) {
      editHistoryDao.insert(editHistoryRecord);
    }

    Set<Integer> notified;

    if (!newMsg.isDraft() && !newMsg.isExpired()) {
      Section section = sectionService.getSection(oldMsg.getSectionId());

      if (newTags!=null && sendTagEventsNeeded(section, oldMsg, commit)) {
        notified = sendEvents(newText, oldMsg.getId(), newTags, oldMsg.getAuthorUserId());
      } else {
        notified = sendEvents(newText, oldMsg.getId(), List.of(), oldMsg.getAuthorUserId());
      }
    } else {
      notified = Set.of();
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

    return Tuple2.apply(modified, notified);
  }

  private void replaceImage(Topic oldMsg, UploadedImagePreview imagePreview, EditHistoryRecord editHistoryRecord) throws IOException {
    Image oldImage = imageDao.imageForTopic(oldMsg);

    if (oldImage!=null) {
      imageDao.deleteImage(oldImage);
    }

    int id = imageDao.saveImage(oldMsg.getId(), imagePreview.extension());

    File galleryPath = new File(siteConfig.getUploadPath() + "/images");

    imagePreview.moveTo(galleryPath, Integer.toString(id));

    if (oldImage!=null) {
      editHistoryRecord.setOldimage(oldImage.getId());
    } else {
      editHistoryRecord.setOldimage(0);
    }
  }

  private void commit(Topic msg, User commiter, int bonus, Map<Integer, Integer> editorBonus) {
    if (bonus < 0 || bonus > 20) {
      throw new IllegalStateException("Неверное значение bonus");
    }

    if (msg.isDraft()) {
      topicDao.publish(msg);
    }

    topicDao.commit(msg, commiter);

    userDao.changeScore(msg.getAuthorUserId(), bonus);

    if (editorBonus!=null) {
      for (Map.Entry<Integer, Integer> entry : editorBonus.entrySet()) {
        userDao.changeScore(entry.getKey(), entry.getValue());
      }
    }
  }
}
