/*
 * Copyright 1998-2015 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import ru.org.linux.site.PublicApi;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
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
