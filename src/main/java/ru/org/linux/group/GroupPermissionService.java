package ru.org.linux.group;

import org.springframework.stereotype.Service;
import ru.org.linux.topic.TopicPermissionService;
import ru.org.linux.user.User;

@Service
public class GroupPermissionService {
  public boolean isTopicPostingAllowed(Group group, User currentUser) {
    int groupRestriction = group.getTopicRestriction();

    if (groupRestriction == TopicPermissionService.POSTSCORE_UNRESTRICTED) {
      return true;
    }

    if (currentUser==null) {
      return false;
    }

    if (currentUser.isBlocked()) {
      return false;
    }

    if (groupRestriction==TopicPermissionService.POSTSCORE_MODERATORS_ONLY) {
      return currentUser.isModerator();
    } else {
      return currentUser.getScore() >= groupRestriction;
    }
  }
}
