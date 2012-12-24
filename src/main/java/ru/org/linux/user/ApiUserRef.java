package ru.org.linux.user;

import com.google.common.collect.ImmutableList;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import ru.org.linux.site.PublicApi;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
@PublicApi
public class ApiUserRef {
  private final String nick;
  private final boolean blocked;
  private final boolean anonymous;
  private final ImmutableList<Boolean> stars;
  private final Integer score;
  private final Integer maxScore;

  /*
    Constructor with default values for Jackson serializer
   */
  public ApiUserRef() {
    nick = null;
    blocked = false;
    anonymous = false;
    stars = ImmutableList.of();
    score = null;
    maxScore = null;
  }

  @JsonCreator
  public ApiUserRef(
          @JsonProperty("nick") String nick,
          @JsonProperty("blocked") boolean blocked,
          @JsonProperty("anonymous") boolean anonymous,
          @JsonProperty("stars") ImmutableList<Boolean> stars,
          Integer score, Integer maxScore) {
    this.nick = nick;
    this.blocked = blocked;
    this.anonymous = anonymous;
    this.stars = stars;
    this.score = score;
    this.maxScore = maxScore;
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

  public Integer getScore() {
    return score;
  }

  public Integer getMaxScore() {
    return maxScore;
  }
}
