package ru.org.linux.gallery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.edithistory.EditHistoryDto;
import ru.org.linux.edithistory.EditHistoryObjectTypeEnum;
import ru.org.linux.edithistory.EditHistoryService;
import ru.org.linux.user.User;

@Service
public class ImageService {
  @Autowired
  private ImageDao imageDao;

  @Autowired
  private EditHistoryService editHistoryService;

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void deleteImage(User editor, Image image) {
    EditHistoryDto info = new EditHistoryDto();

    info.setEditor(editor.getId());
    info.setMsgid(image.getTopicId());
    info.setOldimage(image.getId());
    info.setObjectType(EditHistoryObjectTypeEnum.TOPIC);

    imageDao.deleteImage(image);

    editHistoryService.insert(info);
  }
}
