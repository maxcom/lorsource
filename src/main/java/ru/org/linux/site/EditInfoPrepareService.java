package ru.org.linux.site;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.spring.dao.MessageDao;
import ru.org.linux.spring.dao.TagDao;
import ru.org.linux.spring.dao.UserDao;
import ru.org.linux.util.bbcode.LorCodeService;

import java.util.ArrayList;
import java.util.List;

@Service
public class EditInfoPrepareService {
  @Autowired
  private MessageDao messageDao;

  @Autowired
  private TagDao tagDao;

  @Autowired
  private UserDao userDao;

  @Autowired
  private LorCodeService lorCodeService;

  public List<PreparedEditInfo> prepareEditInfo(Message message, boolean secure) throws UserNotFoundException, UserErrorException {
    List<EditInfoDTO> editInfoDTOs = messageDao.loadEditInfo(message.getId());
    List<PreparedEditInfo> editInfos = new ArrayList<PreparedEditInfo>(editInfoDTOs.size());

    String currentMessage = message.getMessage();
    String currentTitle = message.getTitle();
    String currentUrl = message.getUrl();
    String currentLinktext = message.getLinktext();
    List<String> currentTags = tagDao.getMessageTags(message.getMessageId());

    for (int i = 0; i<editInfoDTOs.size(); i++) {
      EditInfoDTO dto = editInfoDTOs.get(i);

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
        currentTags = TagDao.parseTags(dto.getOldtags());
      }
    }

    if (!editInfoDTOs.isEmpty()) {
      EditInfoDTO current = EditInfoDTO.createFromMessage(tagDao, message);

      editInfos.add(new PreparedEditInfo(lorCodeService, secure, userDao, current, currentMessage, currentTitle, currentUrl, currentLinktext, currentTags, false, true));
    }

    return editInfos;
  }
}
