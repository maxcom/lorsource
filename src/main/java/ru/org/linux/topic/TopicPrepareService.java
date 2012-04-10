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

import org.apache.commons.httpclient.URI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.gallery.Screenshot;
import ru.org.linux.group.BadGroupException;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.group.GroupPermissionService;
import ru.org.linux.poll.Poll;
import ru.org.linux.poll.PollNotFoundException;
import ru.org.linux.poll.PollPrepareService;
import ru.org.linux.poll.PreparedPoll;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionNotFoundException;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.DeleteInfo;
import ru.org.linux.spring.Configuration;
import ru.org.linux.spring.dao.DeleteInfoDao;
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.spring.dao.UserAgentDao;
import ru.org.linux.user.MemoriesDao;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserNotFoundException;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ImageInfo;
import ru.org.linux.util.LorURL;
import ru.org.linux.util.bbcode.LorCodeService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class TopicPrepareService {
  private static final Log logger = LogFactory.getLog(TopicPrepareService.class);
  
  @Autowired
  private TopicDao messageDao;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private UserDao userDao;

  @Autowired
  private SectionService sectionService;

  @Autowired
  private DeleteInfoDao deleteInfoDao;

  @Autowired
  private PollPrepareService pollPrepareService;

  @Autowired
  private LorCodeService lorCodeService;

  @Autowired
  private Configuration configuration;

  @Autowired
  private UserAgentDao userAgentDao;

  @Autowired
  private MemoriesDao memoriesDao;

  @Autowired
  private TopicPermissionService topicPermissionService;
  
  @Autowired
  private GroupPermissionService groupPermissionService;
  
  @Autowired
  private MsgbaseDao msgbaseDao;
  
  public PreparedTopic prepareTopic(Topic message, boolean minimizeCut, boolean secure, User user) {
    return prepareMessage(message, messageDao.getTags(message), minimizeCut, null, secure, user, null);
  }

  public PreparedTopic prepareTopicPreview(
          Topic message,
          List<String> tags,
          Poll newPoll,
          boolean secure,
          String text
  ) {
    return prepareMessage(
            message,
            tags,
            false,
            newPoll!=null?pollPrepareService.preparePollPreview(newPoll):null,
            secure,
            text
    );
  }

  private PreparedTopic prepareMessage(Topic message, List<String> tags, boolean minimizeCut, PreparedPoll poll, boolean secure, String text) {
    return prepareMessage(message, tags, minimizeCut, poll, secure, null, text);
  }

  /**
   * Функция подготовки топика
   * @param message топик
   * @param tags список тэгов
   * @param minimizeCut сворачивать ли cut
   * @param poll опрос к топику
   * @param secure является ли соединение https
   * @param user пользователь
   * @return подготовленный топик
   */
  private PreparedTopic prepareMessage(
          Topic message, 
          List<String> tags, 
          boolean minimizeCut, 
          PreparedPoll poll,
          boolean secure, 
          User user,
          String text) {
    try {
      Group group = groupDao.getGroup(message.getGroupId());
      User author = userDao.getUserCached(message.getUid());
      Section section = sectionService.getSection(message.getSectionId());

      DeleteInfo deleteInfo;
      User deleteUser;
      if (message.isDeleted()) {
        deleteInfo = deleteInfoDao.getDeleteInfo(message.getId());

        if (deleteInfo!=null) {
          deleteUser = userDao.getUserCached(deleteInfo.getUserid());
        } else {
          deleteUser = null;
        }
      } else {
        deleteInfo = null;
        deleteUser = null;
      }

      PreparedPoll preparedPoll;

      if (section.isVotePoll()) {
        if (poll==null) {
          preparedPoll = pollPrepareService.preparePoll(message, user);
        } else {
          preparedPoll = poll;
        }
      } else {
        preparedPoll = null;
      }

      User commiter;

      if (message.getCommitby()!=0) {
        commiter = userDao.getUserCached(message.getCommitby());
      } else {
        commiter = null;
      }

      List<EditInfoDto> editInfo = messageDao.getEditInfo(message.getId());
      EditInfoDto lastEditInfo;
      User lastEditor;
      int editCount;

      if (!editInfo.isEmpty()) {
        lastEditInfo = editInfo.get(0);
        lastEditor = userDao.getUserCached(lastEditInfo.getEditor());
        editCount = editInfo.size();
      } else {
        lastEditInfo = null;
        lastEditor = null;
        editCount = 0;
      }

      String processedMessage;
      boolean lorcode;

      if (text == null) {
        MessageText messageText = msgbaseDao.getMessageText(message.getId());
        lorcode = messageText.isLorcode();

        if (messageText.isLorcode()) {
          if (minimizeCut) {
            String url = configuration.getMainUrl() + message.getLink();
            processedMessage = lorCodeService.parseTopicWithMinimizedCut(
                    messageText.getText(),
                    url,
                    secure
            );
          } else {
            processedMessage = lorCodeService.parseTopic(messageText.getText(), secure);
          }
        } else {
          processedMessage = "<p>" + messageText.getText();
        }
      } else {
        lorcode = true;

        if (minimizeCut) {
          String url = configuration.getMainUrl() + message.getLink();
          processedMessage = lorCodeService.parseTopicWithMinimizedCut(
                  text,
                  url,
                  secure
          );
        } else {
          processedMessage = lorCodeService.parseTopic(text, secure);
        }
      }

      String userAgent = userAgentDao.getUserAgentById(message.getUserAgent());
      
      PreparedImage preparedImage;

      if (group.isImagePostAllowed() && message.getUrl()!=null) {
        preparedImage = prepareImage(message, secure);
      } else {
        preparedImage = null;
      }

      return new PreparedTopic(
              message, 
              author, 
              deleteInfo, 
              deleteUser, 
              processedMessage, 
              preparedPoll, 
              commiter, 
              tags,
              group,
              section,
              lastEditInfo,
              lastEditor, 
              editCount,
              userAgent, 
              lorcode, 
              preparedImage, 
              TopicPermissionService.getPostScoreInfo(message.getPostScore())
      );
    } catch (BadGroupException e) {
      throw new RuntimeException(e);
    } catch (UserNotFoundException e) {
      throw new RuntimeException(e);
    } catch (PollNotFoundException e) {
      throw new RuntimeException(e);
    } catch (SectionNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
  
  private PreparedImage prepareImage(Topic topic, boolean secure) {
    String mediumName = Screenshot.getMediumName(topic.getUrl());

    String htmlPath = configuration.getHTMLPathPrefix();
    if (!new File(htmlPath, mediumName).exists()) {
      mediumName = topic.getLinktext();
    }

    try {
      ImageInfo mediumImageInfo = new ImageInfo(htmlPath + mediumName);
      String fullName = htmlPath + topic.getUrl();
      ImageInfo fullInfo = new ImageInfo(
              fullName,
              ImageInfo.detectImageType(new File(fullName))
      );
      LorURL medURI = new LorURL(configuration.getMainURI(), configuration.getMainUrl()+mediumName);
      LorURL fullURI = new LorURL(configuration.getMainURI(), configuration.getMainUrl()+topic.getUrl());

      return new PreparedImage(medURI.fixScheme(secure), mediumImageInfo, fullURI.fixScheme(secure), fullInfo);
    } catch (BadImageException e) {
      logger.warn(e);
      return null;
    } catch (IOException e) {
      logger.warn(e);
      return null;
    }
  }

  /**
   * Подготовка ленты топиков, используется в TopicListController например
   * сообщения рендерятся со свернутым cut
   * @param messages список топиков
   * @param secure является ли соединение https
   * @return список подготовленных топиков
   */
  public List<PersonalizedPreparedTopic> prepareMessagesForUser(List<Topic> messages, boolean secure, User user) {
    List<PersonalizedPreparedTopic> pm = new ArrayList<PersonalizedPreparedTopic>(messages.size());

    for (Topic message : messages) {
      PreparedTopic preparedMessage = prepareMessage(message, messageDao.getTags(message), true, null, secure, null);
      TopicMenu topicMenu = getMessageMenu(preparedMessage, user);
      pm.add(new PersonalizedPreparedTopic(preparedMessage, topicMenu));
    }

    return pm;
  }

  /**
   * Подготовка ленты топиков, используется в TopicListController например
   * сообщения рендерятся со свернутым cut
   * @param messages список топиков
   * @param secure является ли соединение https
   * @return список подготовленных топиков
   */
  public List<PreparedTopic> prepareMessages(List<Topic> messages, boolean secure) {
    List<PreparedTopic> pm = new ArrayList<PreparedTopic>(messages.size());

    for (Topic message : messages) {
      PreparedTopic preparedMessage = prepareMessage(message, messageDao.getTags(message), true, null, secure, null);
      pm.add(preparedMessage);
    }

    return pm;
  }

  public TopicMenu getMessageMenu(PreparedTopic message, User currentUser) {
    boolean editable = currentUser!=null && (groupPermissionService.isEditable(message, currentUser) || groupPermissionService.isTagsEditable(message, currentUser)) ;
    boolean resolvable;
    int memoriesId;
    boolean deletable;

    if (currentUser!=null) {
      resolvable = (currentUser.isModerator() || (message.getAuthor().getId()==currentUser.getId())) &&
            message.getGroup().isResolvable();

      memoriesId = memoriesDao.getId(currentUser, message.getMessage());
      deletable = groupPermissionService.isDeletable(message.getMessage(), currentUser);
    } else {
      resolvable = false;
      memoriesId = 0;
      deletable = false;
    }

    return new TopicMenu(
            editable, 
            resolvable, 
            memoriesId, 
            topicPermissionService.isCommentsAllowed(message.getMessage(), currentUser), 
            deletable
    );
  }
}
