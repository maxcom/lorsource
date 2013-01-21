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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.edithistory.EditHistoryDto;
import ru.org.linux.edithistory.EditHistoryObjectTypeEnum;
import ru.org.linux.edithistory.EditHistoryService;
import ru.org.linux.gallery.Image;
import ru.org.linux.gallery.ImageDao;
import ru.org.linux.group.BadGroupException;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.group.GroupPermissionService;
import ru.org.linux.poll.Poll;
import ru.org.linux.poll.PollNotFoundException;
import ru.org.linux.poll.PollPrepareService;
import ru.org.linux.poll.PreparedPoll;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.DeleteInfo;
import ru.org.linux.spring.Configuration;
import ru.org.linux.spring.dao.DeleteInfoDao;
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.spring.dao.UserAgentDao;
import ru.org.linux.user.*;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ImageInfo;
import ru.org.linux.util.LorURL;
import ru.org.linux.util.bbcode.LorCodeService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

  @Autowired
  private EditHistoryService editHistoryService;

  @Autowired
  private ImageDao imageDao;

  @Autowired
  private UserService userService;
  
  public PreparedTopic prepareTopic(Topic message, boolean secure, User user) {
    return prepareMessage(message, messageDao.getTags(message), false, null, secure, user, null, null);
  }

  public PreparedTopic prepareTopicPreview(
          Topic message,
          List<String> tags,
          Poll newPoll,
          boolean secure,
          String text,
          Image image
  ) {
    return prepareMessage(
            message,
            tags,
            false,
            newPoll!=null?pollPrepareService.preparePollPreview(newPoll):null,
            secure,
            null,
            new MessageText(text, true),
            image
    );
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
          MessageText text,
          @Nullable Image image) {
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

      if (section.isPollPostAllowed()) {
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

      List<EditHistoryDto> editHistoryDtoList = editHistoryService.getEditInfo(message.getId(), EditHistoryObjectTypeEnum.TOPIC);
      EditHistoryDto editHistoryDto;
      User lastEditor;
      int editCount;

      if (!editHistoryDtoList.isEmpty()) {
        editHistoryDto = editHistoryDtoList.get(0);
        lastEditor = userDao.getUserCached(editHistoryDto.getEditor());
        editCount = editHistoryDtoList.size();
      } else {
        editHistoryDto = null;
        lastEditor = null;
        editCount = 0;
      }

      if (text == null) {
        text = msgbaseDao.getMessageText(message.getId());
      }

      String processedMessage;
      String ogDescription;

      if (text.isLorcode()) {
        if (minimizeCut) {
          String url = configuration.getMainUrl() + message.getLink();
          processedMessage = lorCodeService.parseTopicWithMinimizedCut(
                  text.getText(),
                  url,
                  secure,
                  ! topicPermissionService.followInTopic(message, author)
          );
        } else {
          processedMessage = lorCodeService.parseTopic(text.getText(), secure, ! topicPermissionService.followInTopic(message, author));
        }

        ogDescription = lorCodeService.parseForOgDescription(text.getText());
      } else {
        processedMessage = "<p>" + text.getText();
        ogDescription = "";
      }

      String userAgent = userAgentDao.getUserAgentById(message.getUserAgent());
      
      PreparedImage preparedImage = null;

      if (section.isImagepost() || section.isImageAllowed()) {
        if (message.getId()!=0) {
          image = imageDao.imageForTopic(message);
        }

        if (image != null) {
          preparedImage = prepareImage(image, secure);
        }
      }
      Remark remark = null;
      if (user != null ){
        remark = userDao.getRemark(user, author);
      }

      return new PreparedTopic(
              message, 
              author, 
              deleteInfo, 
              deleteUser, 
              processedMessage,
              ogDescription,
              preparedPoll, 
              commiter, 
              tags,
              group,
              section,
              editHistoryDto,
              lastEditor, 
              editCount,
              userAgent, 
              text.isLorcode(),
              preparedImage, 
              TopicPermissionService.getPostScoreInfo(message.getPostScore()),
              remark);
    } catch (BadGroupException e) {
      throw new RuntimeException(e);
    } catch (UserNotFoundException e) {
      throw new RuntimeException(e);
    } catch (PollNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
  
  private PreparedImage prepareImage(@Nonnull Image image, boolean secure) {
    Preconditions.checkNotNull(image);

    String mediumName = image.getMedium();

    String htmlPath = configuration.getHTMLPathPrefix();
    if (!new File(htmlPath, mediumName).exists()) {
      mediumName = image.getIcon();
    }

    try {
      ImageInfo mediumImageInfo = new ImageInfo(htmlPath + mediumName);
      String fullName = htmlPath + image.getOriginal();
      ImageInfo fullInfo = new ImageInfo(
              fullName,
              ImageInfo.detectImageType(new File(fullName))
      );
      LorURL medURI = new LorURL(configuration.getMainURI(), configuration.getMainUrl()+mediumName);
      LorURL fullURI = new LorURL(configuration.getMainURI(), configuration.getMainUrl()+image.getOriginal());

      return new PreparedImage(medURI.fixScheme(secure), mediumImageInfo, fullURI.fixScheme(secure), fullInfo, image);
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
  public List<PersonalizedPreparedTopic> prepareMessagesForUser(
          List<Topic> messages,
          boolean secure,
          User user,
          ProfileProperties profileProperties,
          boolean loadUserpics
  ) {
    List<PersonalizedPreparedTopic> pm = new ArrayList<PersonalizedPreparedTopic>(messages.size());

    Map<Integer,MessageText> textMap = loadTexts(messages);
    ImmutableListMultimap<Integer,String> tags = messageDao.getTags(messages);

    for (Topic message : messages) {
      PreparedTopic preparedMessage = prepareMessage(
              message,
              tags.get(message.getId()),
              true,
              null,
              secure,
              user,
              textMap.get(message.getId()),
              null
      );

      TopicMenu topicMenu = getTopicMenu(
              preparedMessage,
              user,
              secure,
              profileProperties,
              loadUserpics
      );

      pm.add(new PersonalizedPreparedTopic(preparedMessage, topicMenu));
    }

    return pm;
  }

  private Map<Integer, MessageText> loadTexts(List<Topic> messages) {
    return msgbaseDao.getMessageText(
            Lists.newArrayList(
                    Iterables.transform(messages, new Function<Topic, Integer>() {
                      @Override
                      public Integer apply(Topic comment) {
                        return comment.getId();
                      }
                    })
            )
    );
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

    Map<Integer,MessageText> textMap = loadTexts(messages);
    ImmutableListMultimap<Integer,String> tags = messageDao.getTags(messages);

    for (Topic message : messages) {
      PreparedTopic preparedMessage = prepareMessage(
              message,
              tags.get(message.getId()),
              true,
              null,
              secure,
              null,
              textMap.get(message.getId()),
              null
      );

      pm.add(preparedMessage);
    }

    return pm;
  }

  @Nonnull
  public TopicMenu getTopicMenu(
          @Nonnull PreparedTopic message,
          @Nullable User currentUser,
          boolean secure,
          ProfileProperties profileProperties,
          boolean loadUserpics
  ) {
    boolean topicEditable = groupPermissionService.isEditable(message, currentUser);
    boolean tagsEditable = groupPermissionService.isTagsEditable(message, currentUser);
    boolean resolvable;
    int memoriesId;
    int favsId;
    boolean deletable;

    List<Integer> topicStats = memoriesDao.getTopicStats(message.getMessage().getId());

    if (currentUser!=null) {
      resolvable = (currentUser.isModerator() || (message.getAuthor().getId()==currentUser.getId())) &&
            message.getGroup().isResolvable();

      memoriesId = memoriesDao.getId(currentUser, message.getMessage(), true);
      favsId = memoriesDao.getId(currentUser, message.getMessage(), false);
      deletable = groupPermissionService.isDeletable(message.getMessage(), currentUser);
    } else {
      resolvable = false;
      memoriesId = 0;
      favsId = 0;
      deletable = false;
    }

    Userpic userpic = null;

    if (loadUserpics && profileProperties.isShowPhotos()) {
      userpic = userService.getUserpic(
              message.getAuthor(),
              secure,
              profileProperties.getAvatarMode()
      );
    }

    return new TopicMenu(
            topicEditable,
            tagsEditable,
            resolvable, 
            memoriesId,
            favsId,
            topicStats.get(0),
            topicStats.get(1),
            topicPermissionService.isCommentsAllowed(message.getMessage(), currentUser),
            deletable,
            userpic);
  }
}
