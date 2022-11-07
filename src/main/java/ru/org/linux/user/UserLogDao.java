/*
 * Copyright 1998-2022 Linux.org.ru
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

import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.sql.Timestamp;
import java.util.Optional;

@Repository
public class UserLogDao {
  public static final String OPTION_OLD_USERPIC = "old_userpic";
  public static final String OPTION_NEW_USERPIC = "new_userpic";
  public static final String OPTION_BONUS = "bonus";
  public static final String OPTION_REASON = "reason";
  public static final String OPTION_OLD_EMAIL = "old_email";
  public static final String OPTION_NEW_EMAIL = "new_email";
  public static final String OPTION_OLD_INFO = "old_info";
  public static final String OPTION_OLD_TOWN = "old_town";
  public static final String OPTION_OLD_URL = "old_url";
  public static final String OPTION_IP = "ip";
  public static final String OPTION_USET_AGENT = "user_agent";
  public static final String OPTION_INVITED_BY = "invited_by";

  private JdbcTemplate jdbcTemplate;

  @Autowired
  private void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public void logResetUserpic(@Nonnull User user, @Nonnull User actionUser, int bonus) {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

    if (bonus!=0) {
      builder.put(OPTION_BONUS, bonus);
    }

    if (user.getPhoto()!=null) {
      builder.put(OPTION_OLD_USERPIC, user.getPhoto());
    }

    jdbcTemplate.update(
            "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
            user.getId(),
            actionUser.getId(),
            UserLogAction.RESET_USERPIC.toString(),
            builder.build()
    );
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public void logSetUserpic(@Nonnull User user, @Nonnull String userpic) {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

    if (user.getPhoto()!=null) {
      builder.put(OPTION_OLD_USERPIC, user.getPhoto());
    }

    builder.put(OPTION_NEW_USERPIC, userpic);

    jdbcTemplate.update(
            "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
            user.getId(),
            user.getId(),
            UserLogAction.SET_USERPIC.toString(),
            builder.build()
    );
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public void logBlockUser(@Nonnull User user, @Nonnull User moderator, @Nonnull String reason) {
    jdbcTemplate.update(
            "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
            user.getId(),
            moderator.getId(),
            UserLogAction.BLOCK_USER.toString(),
            ImmutableMap.of(OPTION_REASON, reason)
    );
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public void logFreezeUser(@Nonnull User user, @Nonnull User moderator,  
    @Nonnull String reason, @Nonnull Timestamp until) {

    Timestamp     now = new Timestamp(System.currentTimeMillis());
    UserLogAction action = UserLogAction.FROZEN;

    // the action may be not consistent with database (e.g. with real action)
    // if the 'until' is close to the now, but we don't have to worry about it,
    // since, it's not about real use cases
    if (until.before(now)) {
        action = UserLogAction.DEFROSTED;
    }

    jdbcTemplate.update(
            "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
            user.getId(),
            moderator.getId(),
            action.toString(),
            ImmutableMap.of(OPTION_REASON, reason)
    );
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public void logScore50(@Nonnull User user, @Nonnull User moderator) {
    jdbcTemplate.update(
            "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, '')",
            user.getId(),
            moderator.getId(),
            UserLogAction.SCORE50.toString()
    );
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public void logUnblockUser(@Nonnull User user, @Nonnull User moderator) {
    jdbcTemplate.update(
            "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
            user.getId(),
            moderator.getId(),
            UserLogAction.UNBLOCK_USER.toString(),
            ImmutableMap.of()
    );
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public void logAcceptNewEmail(@Nonnull User user, @Nonnull String newEmail) {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

    builder.put(OPTION_NEW_EMAIL, newEmail);

    if (user.getEmail()!=null) {
      builder.put(OPTION_OLD_EMAIL, user.getEmail());
    }

    jdbcTemplate.update(
            "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
            user.getId(),
            user.getId(),
            UserLogAction.ACCEPT_NEW_EMAIL.toString(),
            builder.build()
    );
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public void logResetInfo(@Nonnull User user, @Nonnull User moderator, @Nonnull String userInfo, int bonus) {
    jdbcTemplate.update(
            "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
            user.getId(),
            moderator.getId(),
            UserLogAction.RESET_INFO.toString(),
            ImmutableMap.of(
                    OPTION_OLD_INFO, userInfo,
                    OPTION_BONUS, bonus
            )
    );
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public void logResetUrl(@Nonnull User user, @Nonnull User moderator, @Nonnull String url, int bonus) {
    jdbcTemplate.update(
            "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
            user.getId(),
            moderator.getId(),
            UserLogAction.RESET_URL.toString(),
            ImmutableMap.of(
                    OPTION_OLD_URL, url,
                    OPTION_BONUS, bonus
            )
    );
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public void logResetTown(@Nonnull User user, @Nonnull User moderator, @Nonnull String town, int bonus) {
    jdbcTemplate.update(
            "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
            user.getId(),
            moderator.getId(),
            UserLogAction.RESET_TOWN.toString(),
            ImmutableMap.of(
                    OPTION_OLD_TOWN, town,
                    OPTION_BONUS, bonus
            )
    );
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public void logResetPassword(@Nonnull User user, @Nonnull User moderator) {
    jdbcTemplate.update(
            "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
            user.getId(),
            moderator.getId(),
            UserLogAction.RESET_PASSWORD.toString(),
            ImmutableMap.of()
    );
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public void logSetPassword(@Nonnull User user) {
    jdbcTemplate.update(
            "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
            user.getId(),
            user.getId(),
            UserLogAction.SET_PASSWORD.toString(),
            ImmutableMap.of()
    );
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public void logSetUserInfo(@Nonnull User user, Map<String, String> info) {
    jdbcTemplate.update(
            "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
            user.getId(),
            user.getId(),
            UserLogAction.SET_INFO.toString(),
            info
    );
  }

  @Nonnull
  public List<UserLogItem> getLogItems(@Nonnull User user, boolean includeSelf) {
    String sql =
            includeSelf ?
            "SELECT id, userid, action_userid, action_date, action, info FROM user_log WHERE userid=? ORDER BY id DESC"
            :
            "SELECT id, userid, action_userid, action_date, action, info FROM user_log WHERE userid=? AND userid!=action_userid ORDER BY id DESC";

    return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new UserLogItem(
                    rs.getInt("id"),
                    rs.getInt("userid"),
                    rs.getInt("action_userid"),
                    new DateTime(rs.getTimestamp("action_date")),
                    UserLogAction.valueOf(rs.getString("action").toUpperCase()),
                    (Map<String, String>) rs.getObject("info")
            ),
            user.getId()
    );
  }

  public int getUserpicSetCount(User user, Duration duration) {
    return jdbcTemplate.queryForObject(
            "SELECT count(*) FROM user_log WHERE userid=? AND action=?::user_log_action AND action_date>?",
            Integer.class,
            user.getId(),
            UserLogAction.SET_USERPIC.toString(),
            OffsetDateTime.now().minus(duration));
  }

  public boolean hasRecentModerationEvent(User user, Duration duration, UserLogAction action) {
    return jdbcTemplate.queryForObject(
            "SELECT EXISTS (SELECT * FROM user_log WHERE userid=? AND action=?::user_log_action AND action_date>? AND userid!=action_userid)",
            Boolean.class,
            user.getId(),
            action.toString(),
            OffsetDateTime.now().minus(duration));
  }

  public List<Integer> getRecentlyHasEvent(UserLogAction action) {
    return jdbcTemplate.queryForList(
            "SELECT userid FROM user_log WHERE action=?::user_log_action AND action_date>CURRENT_TIMESTAMP - interval '3 days' ORDER BY action_date",
            Integer.class,
            action.toString());
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public void logRegister(int userid, @Nonnull String ip, Optional<Integer> invitedBy, int userAgent) {
    ImmutableMap<String, String> params;

    params = invitedBy
            .map(integer -> ImmutableMap.of(OPTION_IP, ip, OPTION_USET_AGENT, Integer.toString(userAgent), OPTION_INVITED_BY, integer.toString()))
            .orElseGet(() -> ImmutableMap.of(OPTION_IP, ip, OPTION_USET_AGENT, Integer.toString(userAgent)));

    jdbcTemplate.update(
            "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
            userid,
            userid,
            UserLogAction.REGISTER.toString(),
            params
    );
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public void setCorrector(@Nonnull User user, @Nonnull User moderator) {
    jdbcTemplate.update(
            "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
            user.getId(),
            moderator.getId(),
            UserLogAction.SET_CORRECTOR.toString(),
            ImmutableMap.of()
    );
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public void unsetCorrector(@Nonnull User user, @Nonnull User moderator) {
    jdbcTemplate.update(
            "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
            user.getId(),
            moderator.getId(),
            UserLogAction.UNSET_CORRECTOR.toString(),
            ImmutableMap.of()
    );
  }

}
