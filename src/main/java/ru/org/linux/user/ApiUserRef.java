package ru.org.linux.user;

import com.google.common.collect.ImmutableList;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class ApiUserRef {
  private final String nick;
  private final boolean blocked;
  private final boolean anonymous;
  private final ImmutableList<Boolean> stars;

  /*
    Constructor with default values for Jackson serializer
   */
  public ApiUserRef() {
    nick = null;
    blocked = false;
    anonymous = false;
    stars = ImmutableList.of();
  }

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
