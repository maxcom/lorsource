/*
 * Copyright 1998-2014 Linux.org.ru
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

package ru.org.linux.gallery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.edithistory.EditHistoryDto;
import ru.org.linux.edithistory.EditHistoryObjectTypeEnum;
import ru.org.linux.edithistory.EditHistoryService;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.user.User;

@Service
public class ImageService {
  @Autowired
  private ImageDao imageDao;

  @Autowired
  private EditHistoryService editHistoryService;

  @Autowired
  private TopicDao topicDao;

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void deleteImage(User editor, Image image) {
    EditHistoryDto info = new EditHistoryDto();

    info.setEditor(editor.getId());
    info.setMsgid(image.getTopicId());
    info.setOldimage(image.getId());
    info.setObjectType(EditHistoryObjectTypeEnum.TOPIC);

    imageDao.deleteImage(image);

    editHistoryService.insert(info);
    topicDao.updateLastmod(image.getTopicId(), false);
  }
}
