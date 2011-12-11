/*
 * Copyright 1998-2010 Linux.org.ru
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

package ru.org.linux.message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.site.UserErrorException;
import ru.org.linux.site.UserNotFoundException;
import ru.org.linux.tagcloud.TagCloudDao;
import ru.org.linux.spring.dao.UserDao;
import ru.org.linux.util.bbcode.LorCodeService;

import java.util.ArrayList;
import java.util.List;

@Service
public class EditInfoPrepareService {
  @Autowired
  private MessageDao messageDao;

  @Autowired
  private TagCloudDao tagCloudDao;

  @Autowired
  private UserDao userDao;

  @Autowired
  private LorCodeService lorCodeService;

  public List<PreparedEditInfo> prepareEditInfo(Message message, boolean secure) throws UserNotFoundException, UserErrorException {
    List<EditInfoDto> editInfoDtos = messageDao.loadEditInfo(message.getId());
    List<PreparedEditInfo> editInfos = new ArrayList<PreparedEditInfo>(editInfoDtos.size());

    String currentMessage = message.getMessage();
    String currentTitle = message.getTitle();
    String currentUrl = message.getUrl();
    String currentLinktext = message.getLinktext();
    List<String> currentTags = tagCloudDao.getMessageTags(message.getMessageId());

    for (int i = 0; i< editInfoDtos.size(); i++) {
      EditInfoDto dto = editInfoDtos.get(i);

      editInfos.add(
        new PreparedEditInfo(
          lorCodeService,
          secure,
          userDao,
          dto,
          dto.getOldmessage()!=null ? currentMessage : null,
          dto.getOldtitle()!=null ? currentTitle : null,
          dto.getOldurl()!=null ? currentUrl : null,
          dto.getOldlinktext()!=null ? currentLinktext : null,
          dto.getOldtags()!=null ? currentTags : null,
          i==0,
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
        currentTags = TagCloudDao.parseTags(dto.getOldtags());
      }
    }

    if (!editInfoDtos.isEmpty()) {
      EditInfoDto current = EditInfoDto.createFromMessage(tagCloudDao, message);

      editInfos.add(new PreparedEditInfo(lorCodeService, secure, userDao, current, currentMessage, currentTitle, currentUrl, currentLinktext, currentTags, false, true));
    }

    return editInfos;
  }
}
