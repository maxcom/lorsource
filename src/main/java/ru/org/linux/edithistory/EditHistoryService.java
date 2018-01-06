/*
 * Copyright 1998-2018 Linux.org.ru
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.comment.Comment;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.tag.TagName;
import ru.org.linux.tag.TagRef;
import ru.org.linux.tag.TagService;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicTagService;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserNotFoundException;
import ru.org.linux.user.UserService;
import ru.org.linux.util.bbcode.LorCodeService;
import scala.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EditHistoryService {
  @Autowired
  TopicTagService topicTagService;

  @Autowired
  private UserDao userDao;

  @Autowired
  private UserService userService;

  @Autowired
  private LorCodeService lorCodeService;
  
  @Autowired
  private MsgbaseDao msgbaseDao;

  @Autowired
  private EditHistoryDao editHistoryDao;

  /**
   * Получить историю изменений топика
   */
  public List<PreparedEditHistory> prepareEditInfo(
    Topic message,
    boolean secure
  ) throws UserNotFoundException {
    List<EditHistoryRecord> editInfoDTOs = editHistoryDao.getEditInfo(message.getId(), EditHistoryObjectTypeEnum.TOPIC);
    List<PreparedEditHistory> editHistories = new ArrayList<>(editInfoDTOs.size());

    String currentMessage = msgbaseDao.getMessageText(message.getId()).getText();
    String currentTitle = message.getTitle();
    String currentUrl = message.getUrl();
    String currentLinktext = message.getLinktext();
    List<TagRef> currentTags = topicTagService.getTagRefs(message);
    boolean currentMinor = message.isMinor();

    for (int i = 0; i < editInfoDTOs.size(); i++) {
      EditHistoryRecord dto = editInfoDTOs.get(i);

      editHistories.add(
        new PreparedEditHistory(
          lorCodeService,
          secure,
          userDao.getUserCached(dto.getEditor()),
          dto.getEditdate(),
          dto.getOldmessage() != null ? currentMessage : null,
          dto.getOldtitle() != null ? currentTitle : null,
          dto.getOldurl() != null ? currentUrl : null,
          dto.getOldlinktext() != null ? currentLinktext : null,
          dto.getOldtags() != null ? currentTags : null,
          i == 0,
          false,
          dto.getOldminor() != null ? currentMinor : null
        )
      );

      if (dto.getOldmessage() != null) {
        currentMessage = dto.getOldmessage();
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
        currentTags = TagService.namesToRefs(TagName.parseAndSanitizeTags(dto.getOldtags()));
      }

      if (dto.getOldminor() != null) {
        currentMinor = dto.getOldminor();
      }
    }

    if (!editInfoDTOs.isEmpty()) {
      if (currentTags.isEmpty()) {
        currentTags = null;
      }

      editHistories.add(new PreparedEditHistory(
              lorCodeService,
              secure,
              userDao.getUserCached(message.getUid()),
              message.getPostdate(),
              currentMessage,
              currentTitle,
              currentUrl,
              currentLinktext,
              currentTags,
              false,
              true,
              null
      ));
    }

    return editHistories;
  }

  public List<PreparedEditHistory> prepareEditInfo(
    Comment comment,
    boolean secure
  ) throws UserNotFoundException {
    List<EditHistoryRecord> editInfoDTOs = editHistoryDao.getEditInfo(comment.getId(), EditHistoryObjectTypeEnum.COMMENT);
    List<PreparedEditHistory> editHistories = new ArrayList<>(editInfoDTOs.size());

    String currentMessage = msgbaseDao.getMessageText(comment.getId()).getText();
    String currentTitle = comment.getTitle();

    for (int i = 0; i < editInfoDTOs.size(); i++) {
      EditHistoryRecord dto = editInfoDTOs.get(i);

      editHistories.add(
        new PreparedEditHistory(
          lorCodeService,
          secure,
          userDao.getUserCached(dto.getEditor()),
          dto.getEditdate(),
          dto.getOldmessage() != null ? currentMessage : null,
          dto.getOldtitle() != null ? currentTitle : null,
          null,
          null,
          null,
          i == 0,
          false,
          null
        )
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
          lorCodeService,
          secure,
          userDao.getUserCached(comment.getUserid()),
          comment.getPostdate(),
          currentMessage,
          currentTitle,
          null,
          null,
          null,
          false,
          true,
          null
        )
      );
    }

    return editHistories;
  }

  public List<EditHistoryRecord> getEditInfo(int id, EditHistoryObjectTypeEnum objectTypeEnum) {
    return editHistoryDao.getEditInfo(id, objectTypeEnum);
  }

  public List<BriefEditInfo> getBriefEditInfo(int id, EditHistoryObjectTypeEnum objectTypeEnum) {
    return editHistoryDao.getBriefEditInfo(id, objectTypeEnum);
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

    return ImmutableSet.copyOf(userService.getUsersCached(editors));
  }

  public ImmutableSet<Integer> getEditors(final Topic message, List<EditHistoryRecord> editInfoList) {
    return ImmutableSet.copyOf(
            Iterables.transform(
                    editInfoList.stream().filter(input -> input.getEditor() != message.getUid()).collect(Collectors.toList()),
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
}
