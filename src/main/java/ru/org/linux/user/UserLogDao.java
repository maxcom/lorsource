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

import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Repository
public class UserLogDao {
  public static final String OPTION_OLD_USERPIC = "old_userpic";
  public static final String OPTION_NEW_USERPIC = "new_userpic";
  public static final String OPTION_BONUS = "bonus";
  public static final String OPTION_REASON = "reason";
  public static final String OPTION_OLD_EMAIL = "old_email";
  public static final String OPTION_NEW_EMAIL = "new_email";
  public static final String OPTION_OLD_INFO = "old_info";
  public static final String OPTION_IP = "ip";

  private JdbcTemplate jdbcTemplate;

  @Autowired
  private void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public void logResetUserpic(@Nonnull User user, @Nonnull User actionUser, int bonus) {
    ImmutableMap<String, Object> options;

    if (bonus!=0) {
      options = ImmutableMap.<String, Object>of(OPTION_BONUS, bonus, OPTION_OLD_USERPIC, user.getPhoto());
    } else {
      options = ImmutableMap.<String, Object>of(OPTION_OLD_USERPIC, user.getPhoto());
    }

    jdbcTemplate.update(
            "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
            user.getId(),
            actionUser.getId(),
            UserLogAction.RESET_USERPIC.toString(),
            options
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

  @Nonnull
  public List<UserLogItem> getLogItems(@Nonnull User user, boolean includeSelf) {
    String sql =
            includeSelf ?
            "SELECT id, userid, action_userid, action_date, action, info FROM user_log WHERE userid=? ORDER BY id DESC"
            :
            "SELECT id, userid, action_userid, action_date, action, info FROM user_log WHERE userid=? AND userid!=action_userid ORDER BY id DESC";

    return jdbcTemplate.query(
            sql,
            new RowMapper<UserLogItem>() {
              @Override
              public UserLogItem mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new UserLogItem(
                        rs.getInt("id"),
                        rs.getInt("userid"),
                        rs.getInt("action_userid"),
                        new DateTime(rs.getTimestamp("action_date")),
                        UserLogAction.valueOf(rs.getString("action").toUpperCase()),
                        (Map<String, String>) rs.getObject("info")
                );
              }
            },
            user.getId()
    );
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public void logRegister(int userid, @Nonnull String ip) {
    jdbcTemplate.update(
            "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
            userid,
            userid,
            UserLogAction.REGISTER.toString(),
            ImmutableMap.of(OPTION_IP, ip)
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
