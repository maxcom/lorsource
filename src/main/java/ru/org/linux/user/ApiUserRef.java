package ru.org.linux.user;

import com.google.common.collect.ImmutableList;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

public class ApiUserRef {
  private final String nick;
  private final boolean blocked;
  private final boolean anonymous;
  private final ImmutableList<Boolean> stars;

  @JsonCreator
  public ApiUserRef(
          @JsonProperty("nick") String nick,
          @JsonProperty("blocked") boolean blocked,
          @JsonProperty("anonymous") boolean anonymous,
          @JsonProperty("stars") ImmutableList<Boolean> stars
  ) {
    this.nick = nick;
    this.blocked = blocked;
    this.anonymous = anonymous;
    this.stars = stars;
  }

  public String getNick() {
    return nick;
  }

  public boolean isBlocked() {
    return blocked;
  }

  public boolean isAnonymous() {
    return anonymous;
  }

  public ImmutableList<Boolean> getStars() {
    return stars;
  }
}
