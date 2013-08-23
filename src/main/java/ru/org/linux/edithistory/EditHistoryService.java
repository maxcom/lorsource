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

package ru.org.linux.edithistory;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.comment.Comment;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.tag.TagService;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicTagService;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserErrorException;
import ru.org.linux.user.UserNotFoundException;
import ru.org.linux.util.bbcode.LorCodeService;

import java.util.ArrayList;
import java.util.List;

@Service
public class EditHistoryService {
  @Autowired
  private TagService tagService;

  @Autowired
  TopicTagService topicTagService;

  @Autowired
  private UserDao userDao;

  @Autowired
  private LorCodeService lorCodeService;
  
  @Autowired
  private MsgbaseDao msgbaseDao;

  @Autowired
  private EditHistoryDao editHistoryDao;

  /**
   * Получить историю изменений топика
   *
   * @param message
   * @param secure
   * @return
   * @throws UserNotFoundException
   * @throws UserErrorException
   */
  public List<PreparedEditHistory> prepareEditInfo(
    Topic message,
    boolean secure
  ) throws UserNotFoundException {
    List<EditHistoryDto> editInfoDTOs = editHistoryDao.getEditInfo(message.getId(), EditHistoryObjectTypeEnum.TOPIC);
    List<PreparedEditHistory> editHistories = new ArrayList<>(editInfoDTOs.size());

    String currentMessage = msgbaseDao.getMessageText(message.getId()).getText();
    String currentTitle = message.getTitle();
    String currentUrl = message.getUrl();
    String currentLinktext = message.getLinktext();
    List<String> currentTags = topicTagService.getMessageTags(message.getId());
    boolean currentMinor = message.isMinor();

    for (int i = 0; i < editInfoDTOs.size(); i++) {
      EditHistoryDto dto = editInfoDTOs.get(i);

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
        currentTags = tagService.parseSanitizeTags(dto.getOldtags());
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
    List<EditHistoryDto> editInfoDTOs = editHistoryDao.getEditInfo(comment.getId(), EditHistoryObjectTypeEnum.COMMENT);
    List<PreparedEditHistory> editHistories = new ArrayList<>(editInfoDTOs.size());

    String currentMessage = msgbaseDao.getMessageText(comment.getId()).getText();
    String currentTitle = comment.getTitle();

    for (int i = 0; i < editInfoDTOs.size(); i++) {
      EditHistoryDto dto = editInfoDTOs.get(i);

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

  public List<EditHistoryDto> getEditInfo(int id, EditHistoryObjectTypeEnum objectTypeEnum) {
    return editHistoryDao.getEditInfo(id, objectTypeEnum);
  }

  public void insert(EditHistoryDto editHistoryDto) {
    editHistoryDao.insert(editHistoryDto);
  }

  public ImmutableSet<User> getEditors(final Topic message, List<EditHistoryDto> editInfoList) {
    return ImmutableSet.copyOf(
            Iterables.transform(
                    Iterables.filter(editInfoList, new Predicate<EditHistoryDto>() {
                      @Override
                      public boolean apply(EditHistoryDto input) {
                        return input.getEditor() != message.getUid();
                      }
                    }),
                    new Function<EditHistoryDto, User>() {
                      @Override
                      public User apply(EditHistoryDto input) {
                        return userDao.getUserCached(input.getEditor());
                      }
                    })
    );
  }
}
