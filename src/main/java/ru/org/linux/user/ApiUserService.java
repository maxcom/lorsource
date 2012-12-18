package ru.org.linux.user;

import com.google.common.collect.ImmutableList;
import org.springframework.stereotype.Service;

@Service
public class ApiUserService {
  private ImmutableList<Boolean> getStars(User user) {
    ImmutableList.Builder<Boolean> builder = ImmutableList.builder();

    for (int i=0; i<user.getGreenStars(); i++) {
      builder.add(true);
    }

    for (int i=0; i<user.getGreyStars(); i++) {
      builder.add(false);
    }

    return builder.build();
  }

  public ApiUserRef ref(User user, User requestUser) {
    Integer score = null;
    Integer maxScore = null;

    if (requestUser!=null && requestUser.isModerator() && !user.isAnonymous()) {
      score = user.getScore();
      maxScore = user.getMaxScore();
    }

    return new ApiUserRef(
            user.getNick(),
            user.isBlocked(),
            user.isAnonymous(),
            getStars(user),
            score,
            maxScore
    );
  }
}
