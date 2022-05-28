/*
 * Copyright 1998-2022 Linux.org.ru
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

import com.google.common.collect.ImmutableListMultimap;
import org.springframework.stereotype.Service;
import ru.org.linux.edithistory.EditInfoSummary;
import ru.org.linux.gallery.Image;
import ru.org.linux.gallery.ImageService;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.group.GroupPermissionService;
import ru.org.linux.markup.MessageTextService;
import ru.org.linux.poll.Poll;
import ru.org.linux.poll.PollNotFoundException;
import ru.org.linux.poll.PollPrepareService;
import ru.org.linux.poll.PreparedPoll;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.DeleteInfo;
import ru.org.linux.spring.SiteConfig;
import ru.org.linux.spring.dao.DeleteInfoDao;
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.tag.TagRef;
import ru.org.linux.user.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TopicPrepareService {
  private final GroupDao groupDao;
  private final UserDao userDao;
  private final SectionService sectionService;
  private final DeleteInfoDao deleteInfoDao;
  private final PollPrepareService pollPrepareService;
  private final MessageTextService textService;
  private final SiteConfig siteConfig;
  private final TopicPermissionService topicPermissionService;
  private final GroupPermissionService groupPermissionService;
  private final MsgbaseDao msgbaseDao;
  private final ImageService imageService;
  private final UserService userService;
  private final TopicTagService topicTagService;
  private final RemarkDao remarkDao;

  public TopicPrepareService(SectionService sectionService, GroupDao groupDao, UserDao userDao,
                             DeleteInfoDao deleteInfoDao, PollPrepareService pollPrepareService, RemarkDao remarkDao,
                             MessageTextService textService, SiteConfig siteConfig, UserService userService,
                             TopicPermissionService topicPermissionService,
                             GroupPermissionService groupPermissionService, TopicTagService topicTagService,
                             MsgbaseDao msgbaseDao, ImageService imageService) {
    this.sectionService = sectionService;
    this.groupDao = groupDao;
    this.userDao = userDao;
    this.deleteInfoDao = deleteInfoDao;
    this.pollPrepareService = pollPrepareService;
    this.remarkDao = remarkDao;
    this.textService = textService;
    this.siteConfig = siteConfig;
    this.userService = userService;
    this.topicPermissionService = topicPermissionService;
    this.groupPermissionService = groupPermissionService;
    this.topicTagService = topicTagService;
    this.msgbaseDao = msgbaseDao;
    this.imageService = imageService;
  }

  public PreparedTopic prepareTopic(Topic message, User user) {
    return prepareTopic(
            message,
            topicTagService.getTagRefs(message),
            false,
            Optional.empty(),
            user,
            msgbaseDao.getMessageText(message.getId()),
            Optional.empty()
    );
  }

  public PreparedTopic prepareTopic(Topic message, List<TagRef> tags, User user, MessageText text) {
    return prepareTopic(
            message,
            tags,
            false,
            Optional.empty(),
            user,
            text,
            Optional.empty()
    );
  }

  public PreparedTopic prepareTopicPreview(
          Topic message,
          List<TagRef> tags,
          Poll newPoll,
          MessageText text,
          Image image
  ) {
    return prepareTopic(
            message,
            tags,
            false,
            Optional.ofNullable(newPoll).map(pollPrepareService::preparePollPreview),
            null,
            text,
            Optional.ofNullable(image)
    );
  }

  public PreparedEditInfoSummary prepareEditInfo(EditInfoSummary editInfo) {
    String lastEditor = userDao.getUserCached(editInfo.editor()).getNick();
    int editCount = editInfo.editCount();
    Date lastEditDate = editInfo.editdate();
    return PreparedEditInfoSummary.apply(lastEditor, editCount, lastEditDate);
  }

  /**
   * Функция подготовки топика
   * @param topic топик
   * @param tags список тэгов
   * @param minimizeCut сворачивать ли cut
   * @param poll опрос к топику
   * @param user пользователь
   * @return подготовленный топик
   */
  private PreparedTopic prepareTopic(
          Topic topic,
          List<TagRef> tags,
          boolean minimizeCut, 
          Optional<PreparedPoll> poll,
          User user,
          MessageText text,
          Optional<Image> image) {
    try {
      Group group = groupDao.getGroup(topic.getGroupId());
      User author = userDao.getUserCached(topic.getAuthorUserId());
      Section section = sectionService.getSection(topic.getSectionId());

      Optional<DeleteInfo> deleteInfo;
      Optional<User> deleteUser;

      if (topic.isDeleted()) {
        deleteInfo = deleteInfoDao.getDeleteInfo(topic.getId());
      } else {
        deleteInfo = Optional.empty();
      }

      deleteUser = deleteInfo.map(DeleteInfo::getUserid).map(userDao::getUserCached);

      Optional<PreparedPoll> preparedPoll;

      if (section.isPollPostAllowed()) {
        preparedPoll = Optional.of(poll.orElseGet(() -> pollPrepareService.preparePoll(topic, user)));
      } else {
        preparedPoll = Optional.empty();
      }

      Optional<User> commiter;

      if (topic.getCommitby()!=0) {
        commiter = Optional.of(userDao.getUserCached(topic.getCommitby()));
      } else {
        commiter = Optional.empty();
      }

      String url = siteConfig.getSecureUrlWithoutSlash() + topic.getLink();

      String processedMessage =
              textService.renderTopic(text, minimizeCut, !topicPermissionService.followInTopic(topic, author), url);

      Optional<PreparedImage> preparedImage = Optional.empty();

      if (section.isImagepost() || section.isImageAllowed()) {
        if (topic.getId()!=0) {
          image = imageService.imageForTopic(topic);
        }

        if (image.isPresent()) {
          preparedImage = image.flatMap(imageService::prepareImageJava);
        }
      }

      Optional<Remark> remark;

      if (user != null) {
        remark = remarkDao.getRemarkJava(user, author);
      } else {
        remark = Optional.empty();
      }

      int postscore = topicPermissionService.getPostscore(group, topic);

      return new PreparedTopic(
              topic,
              author, 
              deleteInfo.orElse(null),
              deleteUser.orElse(null),
              processedMessage,
              preparedPoll.orElse(null),
              commiter.orElse(null),
              tags,
              group,
              section,
              text.markup(),
              preparedImage.orElse(null),
              TopicPermissionService.getPostScoreInfo(postscore),
              remark.orElse(null));
    } catch (PollNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Подготовка ленты топиков для пользователя
   * сообщения рендерятся со свернутым cut
   * @param messages список топиков
   * @param user пользователь
   * @param profile профиль пользователя
   * @param loadUserpics флаг загрузки аватар
   * @return список подготовленных топиков
   */
  public List<PersonalizedPreparedTopic> prepareMessagesForUser(
          List<Topic> messages,
          User user,
          Profile profile,
          boolean loadUserpics
  ) {
    List<PersonalizedPreparedTopic> pm = new ArrayList<>(messages.size());

    Map<Integer,MessageText> textMap = loadTexts(messages);
    ImmutableListMultimap<Integer,TagRef> tags = topicTagService.getTagRefs(messages);

    for (Topic message : messages) {
      PreparedTopic preparedMessage = prepareTopic(
              message,
              tags.get(message.getId()),
              true,
              Optional.empty(),
              user,
              textMap.get(message.getId()),
              Optional.empty()
      );

      TopicMenu topicMenu = getTopicMenu(
              preparedMessage,
              user,
              profile,
              loadUserpics
      );

      pm.add(new PersonalizedPreparedTopic(preparedMessage, topicMenu));
    }

    return pm;
  }

  private Map<Integer, MessageText> loadTexts(List<Topic> messages) {
    return msgbaseDao.getMessageText(messages.stream().map(Topic::getId).collect(Collectors.toList()));
  }

  /**
   * Подготовка ленты топиков, используется в TopicListController например
   * сообщения рендерятся со свернутым cut
   * @param messages список топиков
   * @return список подготовленных топиков
   */
  public List<PreparedTopic> prepareMessages(List<Topic> messages) {
    List<PreparedTopic> pm = new ArrayList<>(messages.size());

    Map<Integer,MessageText> textMap = loadTexts(messages);
    ImmutableListMultimap<Integer,TagRef> tags = topicTagService.getTagRefs(messages);

    for (Topic message : messages) {
      PreparedTopic preparedMessage = prepareTopic(
              message,
              tags.get(message.getId()),
              true,
              Optional.empty(),
              null,
              textMap.get(message.getId()),
              Optional.empty()
      );

      pm.add(preparedMessage);
    }

    return pm;
  }

  public TopicMenu getTopicMenu(
          PreparedTopic message,
          @Nullable User currentUser,
          Profile profile,
          boolean loadUserpics
  ) {
    boolean topicEditable = groupPermissionService.isEditable(message, currentUser);
    boolean tagsEditable = groupPermissionService.isTagsEditable(message, currentUser);
    boolean resolvable;
    boolean deletable;
    boolean undeletable;

    if (currentUser!=null) {
      resolvable = (currentUser.isModerator() || (message.getAuthor().getId()==currentUser.getId())) &&
            message.getGroup().isResolvable();

      deletable = groupPermissionService.isDeletable(message.getMessage(), currentUser);
      undeletable = groupPermissionService.isUndeletable(message.getMessage(), currentUser);
    } else {
      resolvable = false;
      deletable = false;
      undeletable = false;
    }

    Optional<Userpic> userpic;

    if (loadUserpics && profile.isShowPhotos()) {
      userpic = Optional.of(userService.getUserpic(
              message.getAuthor(),
              profile.getAvatarMode(),
              true
      ));
    } else {
      userpic = Optional.empty();
    }

    int postscore = topicPermissionService.getPostscore(message.getGroup(), message.getMessage());
    boolean showComments = postscore != TopicPermissionService.POSTSCORE_HIDE_COMMENTS;

    return new TopicMenu(
            topicEditable,
            tagsEditable,
            resolvable, 
            topicPermissionService.isCommentsAllowed(message.getGroup(), message.getMessage(), currentUser),
            deletable,
            undeletable,
            groupPermissionService.canCommit(currentUser, message.getMessage()),
            userpic.orElse(null),
            showComments
    );
  }
}
