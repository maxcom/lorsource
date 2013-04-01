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

import javax.annotation.Nonnull;
import javax.sql.DataSource;

@Repository
public class UserLogDao {
  private static final String OPTION_OLD_USERPIC = "old_userpic";
  private static final String OPTION_NEW_USERPIC = "new_userpic";

  private JdbcTemplate jdbcTemplate;

  @Autowired
  private void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  public void logResetUserpic(@Nonnull User user, @Nonnull User actionUser, int bonus) {
    ImmutableMap<String, Object> options;

    if (bonus!=0) {
      options = ImmutableMap.<String, Object>of("bonus", bonus, OPTION_OLD_USERPIC, user.getPhoto());
    } else {
      options = ImmutableMap.<String, Object>of("old_userpic", user.getPhoto());
    }

    jdbcTemplate.update(
            "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
            user.getId(),
            actionUser.getId(),
            UserLogAction.RESET_USERPIC.toString(),
            options
    );
  }

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
}
