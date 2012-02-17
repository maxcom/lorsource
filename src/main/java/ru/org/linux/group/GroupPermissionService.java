package ru.org.linux.group;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionNotFoundException;
import ru.org.linux.section.SectionService;
import ru.org.linux.topic.TopicPermissionService;
import ru.org.linux.user.User;

@Service
public class GroupPermissionService {
  @Autowired
  private SectionService sectionService;

  private int getEffectivePostscore(Group group) {
    Section section;

    try {
      section = sectionService.getSection(group.getSectionId());
    } catch (SectionNotFoundException e) {
      throw new RuntimeException("bad section", e);
    }

    return Math.max(group.getTopicRestriction(), section.getTopicsRestriction());
  }

  public boolean isTopicPostingAllowed(Group group, User currentUser) {
    int restriction = getEffectivePostscore(group);

    if (restriction == TopicPermissionService.POSTSCORE_UNRESTRICTED) {
      return true;
    }

    if (currentUser==null || currentUser.isAnonymous()) {
      return false;
    }

    if (currentUser.isBlocked()) {
      return false;
    }

    if (restriction==TopicPermissionService.POSTSCORE_MODERATORS_ONLY) {
      return currentUser.isModerator();
    } else {
      return currentUser.getScore() >= restriction;
    }
  }

  public String getPostScoreInfo(Group group) {
    int postscore = getEffectivePostscore(group);

    switch (postscore) {
      case TopicPermissionService.POSTSCORE_UNRESTRICTED:
        return "";
      case 100:
        return "<b>Ограничение на добавление сообщений</b>: " + User.getStars(100, 100);
      case 200:
        return "<b>Ограничение на добавление сообщений</b>: " + User.getStars(200, 200);
      case 300:
        return "<b>Ограничение на добавление сообщений</b>: " + User.getStars(300, 300);
      case 400:
        return "<b>Ограничение на добавление сообщений</b>: " + User.getStars(400, 400);
      case 500:
        return "<b>Ограничение на добавление сообщений</b>: " + User.getStars(500, 500);
      case TopicPermissionService.POSTSCORE_MODERATORS_ONLY:
        return "<b>Ограничение на добавление сообщений</b>: только для модераторов";
      case TopicPermissionService.POSTSCORE_REGISTERED_ONLY:
        return "<b>Ограничение на добавление сообщений</b>: только для зарегистрированных пользователей";
      default:
        return "<b>Ограничение на добавление сообщений</b>: только для зарегистрированных пользователей, score>=" + postscore;
    }
  }
}
