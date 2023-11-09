/*
 * Copyright 1998-2023 Linux.org.ru
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

package ru.org.linux.edithistory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.springframework.stereotype.Service;
import ru.org.linux.comment.Comment;
import ru.org.linux.gallery.Image;
import ru.org.linux.gallery.ImageDao;
import ru.org.linux.gallery.ImageService;
import ru.org.linux.markup.MarkupType;
import ru.org.linux.markup.MessageTextService;
import ru.org.linux.poll.Poll;
import ru.org.linux.poll.PollDao;
import ru.org.linux.poll.PollNotFoundException;
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.tag.TagName;
import ru.org.linux.tag.TagRef;
import ru.org.linux.tag.TagService;
import ru.org.linux.topic.PreparedImage;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicTagService;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserNotFoundException;
import ru.org.linux.user.UserService;
import scala.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EditHistoryService {
  private final TopicTagService topicTagService;

  private final UserDao userDao;

  private final UserService userService;

  private final MessageTextService textService;
  
  private final MsgbaseDao msgbaseDao;

  private final EditHistoryDao editHistoryDao;

  private final ImageDao imageDao;

  private final ImageService imageService;

  private final PollDao pollDao;

  public EditHistoryService(TopicTagService topicTagService, UserDao userDao, UserService userService,
                            MessageTextService textService, MsgbaseDao msgbaseDao, EditHistoryDao editHistoryDao,
                            ImageDao imageDao, ImageService imageService, PollDao pollDao) {
    this.topicTagService = topicTagService;
    this.userDao = userDao;
    this.userService = userService;
    this.textService = textService;
    this.msgbaseDao = msgbaseDao;
    this.editHistoryDao = editHistoryDao;
    this.imageDao = imageDao;
    this.imageService = imageService;
    this.pollDao = pollDao;
  }

  /**
   * Получить историю изменений топика
   */
  public List<PreparedEditHistory> prepareEditInfo(
    Topic topic,User currentUser
  ) throws UserNotFoundException {
    List<EditHistoryRecord> editInfoDTOs = editHistoryDao.getEditInfo(topic.getId(), EditHistoryObjectTypeEnum.TOPIC);
    List<PreparedEditHistory> editHistories = new ArrayList<>(editInfoDTOs.size());

    MessageText messageText = msgbaseDao.getMessageText(topic.getId());
    String currentMessage = messageText.text();
    MarkupType markup = messageText.markup();
    String currentTitle = topic.getTitle();
    String currentUrl = topic.getUrl();
    String currentLinktext = topic.getLinktext();
    List<TagRef> currentTags = topicTagService.getTagRefs(topic);
    boolean currentMinor = topic.isMinor();
    Image maybeImage = imageDao.imageForTopic(topic);
    PreparedImage currentImage = maybeImage !=null ? imageService.prepareImageJava(maybeImage).orElse(null) : null;
    Poll maybePoll;
    Integer lastId = null;

    try {
      maybePoll = pollDao.getPollByTopicId(topic.getId(),currentUser!=null ? currentUser.getId() : 0);
    } catch (PollNotFoundException ex) {
      maybePoll = null;
    }

    for (int i = 0; i < editInfoDTOs.size(); i++) {
      EditHistoryRecord dto = editInfoDTOs.get(i);

      editHistories.add(
        new PreparedEditHistory(
                textService,
          userDao.getUserCached(dto.getEditor()),
          dto.getEditdate(),
          dto.getOldmessage() != null ? currentMessage : null,
          dto.getOldtitle() != null ? currentTitle : null,
          dto.getOldurl() != null ? currentUrl : null,
          dto.getOldlinktext() != null ? currentLinktext : null,
          dto.getOldtags() != null ? currentTags : null,
          i == 0,
          false,
          dto.getOldminor() != null ? currentMinor : null,
          dto.getOldimage() != null && currentImage !=null ? currentImage : null,
          currentImage == null && dto.getOldimage()!=null,
          markup,
          dto.getOldPoll() !=null ? maybePoll : null,
          lastId
        )
      );

      if (dto.getOldimage() != null) {
        if (dto.getOldimage() == 0) {
          currentImage = null;
        } else {
          currentImage = imageService.prepareImageJava(imageDao.getImage(dto.getOldimage())).orElse(null);
        }
      }

      if (dto.getOldmessage() != null) {
        currentMessage = dto.getOldmessage();
        lastId = dto.getId();
      }

      if (dto.getOldtitle() != null) {
        currentTitle = dto.getOldtitle();
      }

      if (dto.getOldurl() != null) {
        currentUrl = dto.getOldurl();
      }

      if (dto.getOldlinktext() != null) {
        currentLinktext = dto.getOldlinktext();
      }

      if (dto.getOldtags() != null) {
        currentTags = TagService.namesToRefs(TagName.parseAndSanitizeTagsJava(dto.getOldtags()));
      }

      if (dto.getOldminor() != null) {
        currentMinor = dto.getOldminor();
      }

      if (dto.getOldPoll() != null) {
        maybePoll = dto.getOldPoll();
      }
    }

    if (!editInfoDTOs.isEmpty()) {
      if (currentTags.isEmpty()) {
        currentTags = null;
      }

      editHistories.add(new PreparedEditHistory(
              textService,
              userDao.getUserCached(topic.getAuthorUserId()),
              topic.getPostdate(),
              currentMessage,
              currentTitle,
              currentUrl,
              currentLinktext,
              currentTags,
              false,
              true,
              null,
              currentImage,
              false,
              markup,
              maybePoll,
              lastId));
    }

    return editHistories;
  }

  public List<PreparedEditHistory> prepareEditInfo(Comment comment) throws UserNotFoundException {
    List<EditHistoryRecord> editInfoDTOs = editHistoryDao.getEditInfo(comment.getId(), EditHistoryObjectTypeEnum.COMMENT);
    List<PreparedEditHistory> editHistories = new ArrayList<>(editInfoDTOs.size());

    MessageText messageText = msgbaseDao.getMessageText(comment.getId());
    MarkupType markup = messageText.markup();
    String currentMessage = messageText.text();
    String currentTitle = comment.getTitle();

    for (int i = 0; i < editInfoDTOs.size(); i++) {
      EditHistoryRecord dto = editInfoDTOs.get(i);

      editHistories.add(
        new PreparedEditHistory(
                textService,
          userDao.getUserCached(dto.getEditor()),
          dto.getEditdate(),
          dto.getOldmessage() != null ? currentMessage : null,
          dto.getOldtitle() != null ? currentTitle : null,
          null,
          null,
          null,
          i == 0,
          false,
          null,
                null, false, markup, null, null)
      );

      if (dto.getOldmessage() != null) {
        currentMessage = dto.getOldmessage();
      }

      if (dto.getOldtitle() != null) {
        currentTitle = dto.getOldtitle();
      }
    }

    if (!editInfoDTOs.isEmpty()) {
      editHistories.add(
        new PreparedEditHistory(
                textService,
          userDao.getUserCached(comment.getUserid()),
          comment.getPostdate(),
          currentMessage,
          currentTitle,
          null,
          null,
          null,
          false,
          true,
          null,
                null, false, markup, null, null)
      );
    }

    return editHistories;
  }

  public List<EditHistoryRecord> getEditInfo(int id, EditHistoryObjectTypeEnum objectTypeEnum) {
    return editHistoryDao.getEditInfo(id, objectTypeEnum);
  }

  public int editCount(int id, EditHistoryObjectTypeEnum objectTypeEnum) {
    // TODO replace with count() SQL query
    return editHistoryDao.getEditInfo(id, objectTypeEnum).size();
  }

  public void insert(EditHistoryRecord editHistoryRecord) {
    editHistoryDao.insert(editHistoryRecord);
  }

  public ImmutableSet<User> getEditorUsers(final Topic message, List<EditHistoryRecord> editInfoList) {
    ImmutableSet<Integer> editors = getEditors(message, editInfoList);

    return ImmutableSet.copyOf(userService.getUsersCachedJava(editors));
  }

  public ImmutableSet<Integer> getEditors(final Topic message, List<EditHistoryRecord> editInfoList) {
    return ImmutableSet.copyOf(
            Iterables.transform(
                    editInfoList.stream().filter(input -> input.getEditor() != message.getAuthorUserId()).collect(Collectors.toList()),
                    EditHistoryRecord::getEditor)
    );
  }

  public Option<EditInfoSummary> editInfoSummary(int id, EditHistoryObjectTypeEnum objectTypeEnum) {
    List<BriefEditInfo> history = editHistoryDao.getBriefEditInfo(id, objectTypeEnum);

    if (history.isEmpty()) {
      return Option.empty();
    } else {
      return Option.apply(EditInfoSummary.apply(history.size(), history.get(0)));
    }
  }

  public EditHistoryRecord getEditHistoryRecord(Topic topic, int recordId) {
    return editHistoryDao.getEditRecord(topic.getId(), recordId, EditHistoryObjectTypeEnum.TOPIC);
  }
}
