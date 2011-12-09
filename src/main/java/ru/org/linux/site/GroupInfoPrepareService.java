package ru.org.linux.site;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.util.bbcode.LorCodeService;

@Service
public class GroupInfoPrepareService {
  @Autowired
  private LorCodeService lorCodeService;

  public PreparedGroupInfo prepareGroupInfo(Group group, boolean secure) {
    String longInfo;

    if (group.getLongInfo()!=null) {
      longInfo = lorCodeService.parseComment(group.getLongInfo(), secure);
    } else {
      longInfo = null;
    }

    return new PreparedGroupInfo(group, longInfo);
  }
}
