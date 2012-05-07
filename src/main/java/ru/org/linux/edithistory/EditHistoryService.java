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

package ru.org.linux.edithistory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.tag.TagService;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicTagService;
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

  public List<PreparedEditHistory> prepareEditInfo(Topic message, boolean secure) throws UserNotFoundException, UserErrorException {
    List<EditHistoryDto> editInfoDTOs = editHistoryDao.getEditInfo(message.getId());
    List<PreparedEditHistory> editHistories = new ArrayList<PreparedEditHistory>(editInfoDTOs.size());

    String baseText = msgbaseDao.getMessageText(message.getId()).getText();

    String currentMessage = baseText;
    String currentTitle = message.getTitle();
    String currentUrl = message.getUrl();
    String currentLinktext = message.getLinktext();
    List<String> currentTags = topicTagService.getMessageTags(message.getMessageId());

    for (int i = 0; i<editInfoDTOs.size(); i++) {
      EditHistoryDto dto = editInfoDTOs.get(i);

      editHistories.add(
        new PreparedEditHistory(
          lorCodeService,
          secure,
          userDao,
          dto,
          dto.getOldmessage() != null ? currentMessage : null,
          dto.getOldtitle() != null ? currentTitle : null,
          dto.getOldurl() != null ? currentUrl : null,
          dto.getOldlinktext() != null ? currentLinktext : null,
          dto.getOldtags() != null ? currentTags : null,
          i == 0,
          false
        )
      );

      if (dto.getOldmessage() !=null) {
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

      if (dto.getOldtags()!=null) {
        currentTags = tagService.parseSanitizeTags(dto.getOldtags());
      }
    }

    if (!editInfoDTOs.isEmpty()) {
      EditHistoryDto current = getEditInfoDto(topicTagService, message, baseText);

      if (currentTags.isEmpty()) {
        currentTags = null;
      }

      editHistories.add(new PreparedEditHistory(lorCodeService, secure, userDao, current, currentMessage, currentTitle, currentUrl, currentLinktext, currentTags, false, true));
    }

    return editHistories;
  }

  /**
   *
   *
   * @param id
   * @return
   */
  public List<EditHistoryDto> getEditInfo(int id) {
    return editHistoryDao.getEditInfo(id);
  }

  /**
   *
   * @param editHistoryDto
   */
  public void insert(EditHistoryDto editHistoryDto) {
    editHistoryDao.insert(editHistoryDto);
  }

  /**
   *
   * @param topicTagService
   * @param message
   * @param text
   * @return
   */
  private EditHistoryDto getEditInfoDto(TopicTagService topicTagService, Topic message, String text) {
    EditHistoryDto editInfoDto = new EditHistoryDto();

    editInfoDto.setOldmessage(text);
    editInfoDto.setEditdate(message.getPostdate());
    editInfoDto.setEditor(message.getUid());
    editInfoDto.setMsgid(message.getMessageId());
    editInfoDto.setOldtags(TagService.toString(topicTagService.getMessageTags(message.getMessageId())));
    editInfoDto.setOldlinktext(message.getLinktext());
    editInfoDto.setOldurl(message.getUrl());
    editInfoDto.setObjectType(EditHistoryObjectTypeEnum.TOPIC);

    return editInfoDto;
  }
}
