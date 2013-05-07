package ru.org.linux.topic;

import com.google.common.base.Predicates;
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
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.ScriptErrorException;
import ru.org.linux.spring.Configuration;
import ru.org.linux.spring.dao.DeleteInfoDao;
import ru.org.linux.tag.TagService;
import ru.org.linux.user.*;
import ru.org.linux.util.LorHttpUtils;
import ru.org.linux.util.bbcode.LorCodeService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TopicService {
  private static final Log logger = LogFactory.getLog(TopicService.class);

  @Autowired
  private TopicDao topicDao;

  @Autowired
  private SectionService sectionService;

  @Autowired
  private Configuration configuration;

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

    List<String> tags = tagService.parseSanitizeTags(form.getTags());

    topicTagService.updateTags(msgid, tags);
    tagService.updateCounters(ImmutableList.<String>of(), tags);

    sendEvents(message, msgid, tags, user);

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
  private void sendEvents(String message, int msgid, List<String> tags, User author) {
    Set<User> userRefs = lorCodeService.getReplierFromMessage(message);

    // оповещение пользователей по тегам
    List<Integer> userIdListByTags = userTagService.getUserIdListByTags(author, tags);

    final Set<Integer> userRefIds = new HashSet<>();
    for (User userRef : userRefs) {
      userRefIds.add(userRef.getId());
    }

    // не оповещать пользователей. которые ранее были оповещены через упоминание
    Iterable<Integer> tagUsers = Iterables.filter(
            userIdListByTags,
            Predicates.not(Predicates.in(userRefIds))
    );

    userEventService.addUserRefEvent(userRefIds, msgid);
    userEventService.addUserTagEvent(tagUsers, msgid);
  }

  /**
   * Удаление топика и если удаляет модератор изменить автору score
   * @param message удаляемый топик
   * @param user удаляющий пользователь
   * @param reason прчина удаления
   * @param bonus дельта изменения score автора топика
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void deleteWithBonus(Topic message, User user, String reason, int bonus) {
    if (bonus>20 || bonus<0) {
      throw new IllegalArgumentException("Некорректное значение bonus");
    }

    if (user.isModerator() && bonus!=0 && user.getId()!=message.getUid()) {
      deleteTopic(message.getId(), user, reason, -bonus);
      userDao.changeScore(message.getUid(), -bonus);
    } else {
      deleteTopic(message.getId(), user, reason, 0);
    }
  }

  private void deleteTopic(int mid, User moderator, String reason, int bonus) {
    topicDao.delete(mid);
    deleteInfoDao.insert(mid, moderator, reason, bonus);
    userEventService.processTopicDeleted(mid);
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

    for (int mid : topics) {
      deleteTopic(mid, moderator, "Блокировка пользователя с удалением сообщений", 0);
    }

    return topics;
  }
}
