/*
 * Copyright 1998-2024 Linux.org.ru
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

import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.util.password.BasicPasswordEncryptor;
import org.jasypt.util.password.PasswordEncryptor;
import org.springframework.validation.Errors;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.auth.BadPasswordException;
import ru.org.linux.util.StringUtil;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class User implements Serializable {
  private static final int ANONYMOUS_LEVEL_SCORE = 50;
  public static final int ANONYMOUS_ID = 2;

  private final String nick;
  private final int id;
  private final boolean canmod;
  private final boolean candel;
  private final boolean anonymous;
  private final boolean corrector;
  private final boolean blocked;
  private final String password;
  private final int score;
  private final int maxScore;
  private final String photo;
  private final String email;
  private final String fullName;
  private final int unreadEvents;
  private final String style;
  private final Timestamp frozenUntil;
  private final int frozenBy;
  private final String freezingReason;

  private final boolean activated;

  public static final int MAX_NICK_LENGTH = 19; // check only on new user registration, do not check existing users!

  private static final long serialVersionUID = 69986652856916540L;

  public User(ResultSet rs) throws SQLException {
    id = rs.getInt("id");
    nick = rs.getString("nick");
    canmod = rs.getBoolean("canmod");
    candel = rs.getBoolean("candel");
    corrector = rs.getBoolean("corrector");
    activated = rs.getBoolean("activated");
    blocked = rs.getBoolean("blocked");
    score = rs.getInt("score");
    maxScore = rs.getInt("max_score");
    fullName = rs.getString("name");
    String pwd = rs.getString("passwd");
    if (pwd == null) {
      pwd = "";
    }
    anonymous = pwd.isEmpty();
    password = pwd;
    photo=rs.getString("photo");
    email = rs.getString("email");
    unreadEvents = rs.getInt("unread_events");
    style = rs.getString("style");
    // freezing
    frozenUntil = rs.getTimestamp("frozen_until");
    frozenBy = rs.getInt("frozen_by");
    freezingReason = rs.getString("freezing_reason");
  }

  public int getId() {
    return id;
  }

  public String getNick() {
    return nick;
  }

  public String getPassword() {
    return password;
  }

  public void checkPassword(String password) throws BadPasswordException {
    if (blocked) {
      throw new BadPasswordException(nick);
    }

    if (password==null) {
      throw new BadPasswordException(nick);
    }

    if (anonymous && password.isEmpty()) {
      return;
    }

    if (!matchPassword(password)) {
      throw new BadPasswordException(nick);
    }
  }

  public boolean matchPassword(String password) {
    PasswordEncryptor encryptor = new BasicPasswordEncryptor();

    try {
      return encryptor.checkPassword(password, this.password);
    } catch (EncryptionOperationNotPossibleException ex) {
      return false;
    }
  }

  public void checkBlocked() throws AccessViolationException {
    if (blocked) {
      throw new AccessViolationException("Пользователь заблокирован");
    }

    if (!activated) {
      throw new AccessViolationException("Пользователь не активирован");
    }
  }

  public void checkBlocked(Errors errors) {
    if (blocked) {
      errors.reject(null, "Пользователь заблокирован");
    }

    if (!activated) {
      errors.reject(null, "Пользователь не активирован");
    }
  }

  public void checkFrozen() throws AccessViolationException {
    if (isFrozen()) {
      throw new AccessViolationException("Пользователь временно заморожен");
    }
  }

  public void checkFrozen(Errors errors) {
    if (isFrozen()) {
      errors.reject(null, "Пользователь временно заморожен");
    }
  }

  public void checkCommit() throws AccessViolationException {
    if (anonymous || blocked) {
      throw new AccessViolationException("Commit access denied for anonymous user");
    }
    if (!canmod) {
      throw new AccessViolationException("Commit access denied for user " + nick + " (" + id + ") ");
    }
  }

  public boolean isFrozen() {
    if (frozenUntil == null) {
      return false;
    }
    return frozenUntil.after(new Timestamp(System.currentTimeMillis()));
  }

  public boolean isBlocked() {
    return blocked;
  }

  public int getFrozenBy() {
    return frozenBy;
  }

  public Timestamp getFrozenUntil() {
    return frozenUntil;
  }

  public String getFreezingReason() {
    return freezingReason;
  }

  /**
   * Check if use is super-moderator
   *
   * @throws AccessViolationException if use is not super-moderator
   */
  public void checkDelete() throws AccessViolationException {
    if (anonymous || blocked) {
      throw new AccessViolationException("Delete access denied for anonymous user");
    }
    if (!candel) {
      throw new AccessViolationException("Delete access denied for user " + nick + " (" + id + ") ");
    }
  }

  public boolean isModerator() {
    return canmod;
  }

  public boolean isAdministrator() {
    return candel;
  }

  public boolean canCorrect() {
    return corrector && !isFrozen();
  }

  public boolean isCorrector() {
    return corrector;
  }

  public boolean isAnonymous() {
    return anonymous;
  }

  public String getActivationCode(String base) {
    return getActivationCode(base, nick, email);
  }

  public String getActivationCode(String base, String email) {
    return StringUtil.md5hash(base + ':' + nick + ':' + email);
  }

  public static String getActivationCode(String base, String nick, String email) {
    return StringUtil.md5hash(base + ':' + nick + ':' + email);
  }

  public int getScore() {
    if (anonymous) {
      return 0;
    } else {
      return score;
    }
  }

  public int getMaxScore() {
    if (anonymous) {
      return 0;
    } else {
      return maxScore;
    }
  }

  @Deprecated
  public String getStars() {
    return getStars(score, maxScore, true);
  }

  private static int getGreenStars(int score) {
    if (score < 0) {
      score = 0;
    }
    if (score >= 600) {
      score = 599;
    }

    return (int) Math.floor(score / 100.0);
  }

  private static int getGreyStars(int score, int maxScore) {
    if (maxScore < 0) {
      maxScore = 0;
    }
    if (maxScore < score) {
      maxScore = score;
    }
    if (maxScore >= 600) {
      maxScore = 599;
    }

    int stars = getGreenStars(score);
    return (int) Math.floor(maxScore / 100.0) - stars;
  }

  public static String getStars(int score, int maxScore, boolean html) {
    StringBuilder out = new StringBuilder();

    int stars = getGreenStars(score);
    int greyStars = getGreyStars(score, maxScore);

    if (html) {
      out.append("<span class=\"stars\">");
    }

    out.append("★".repeat(Math.max(0, stars)));

    out.append("☆".repeat(Math.max(0, greyStars)));

    if (html) {
      out.append("</span>");
    }

    return out.toString();
  }

  public String getStatus() {
    String text;

    if (score < ANONYMOUS_LEVEL_SCORE) {
      text = "анонимный";
    } else if (score < 100 && maxScore < 100) {
      text = "новый пользователь";
    } else {
      text = "";
    }

    if (maxScore>=100 && text.isEmpty()) {
      return getStars(score, maxScore, true);
    } else if (maxScore>=100 && !text.isEmpty()) {
      return text + " " + getStars(score, maxScore, true);
    } else {
      return text;
    }
  }

  public boolean isActivated() {
    return activated;
  }

  @Nullable
  public String getPhoto() {
    return photo;
  }

  public boolean isAnonymousScore() {
    return anonymous || blocked || score<ANONYMOUS_LEVEL_SCORE;
  }

  public String getEmail() {
    return email;
  }

  public boolean hasEmail() {
    return email!=null;
  }

  public String getName() {
    return fullName;
  }

  public int getUnreadEvents() {
    return unreadEvents;
  }

  /**
   * Стиль\тема пользователя
   * @return название стиля\темы
   */
  public String getStyle() {
    return style;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    User user = (User) o;

    return id == user.id;
  }

  @Override
  public int hashCode() {
    return id;
  }
}
