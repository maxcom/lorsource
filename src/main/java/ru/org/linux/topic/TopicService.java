package ru.org.linux.topic;

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
import ru.org.linux.tag.TagService;
import ru.org.linux.user.User;
import ru.org.linux.user.UserEventService;
import ru.org.linux.user.UserTagService;
import ru.org.linux.util.LorHttpUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

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

    String logmessage = "Написана тема " + msgid + ' ' + LorHttpUtils.getRequestIP(request);
    logger.info(logmessage);

    return msgid;
  }
}
