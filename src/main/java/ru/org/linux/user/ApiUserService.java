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

  public ApiUserRef ref(User user) {
    return new ApiUserRef(
            user.getNick(),
            user.isBlocked(),
            user.isAnonymous(),
            getStars(user)
    );
  }
}
