/*
 * Copyright 1998-2012 Linux.org.ru
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

@Repository
public class UserLogDao {
  private static final String OPTION_OLD_USERPIC = "old_userpic";
  private static final String OPTION_NEW_USERPIC = "new_userpic";
  private static final String OPTION_BONUS = "bonus";
  private static final String OPTION_REASON = "reason";

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
    jdbcTemplate.update(
            "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
            user.getId(),
            user.getId(),
            UserLogAction.ACCENT_NEW_EMAIL.toString(),
            ImmutableMap.of(
                    "old_email", user.getEmail(),
                    "new_email", newEmail
            )
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
                    "old_info", userInfo,
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
}
