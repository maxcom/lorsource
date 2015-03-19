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

package ru.org.linux.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.validation.Errors;
import ru.org.linux.user.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class IPBlockDao {
  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  public IPBlockInfo getBlockInfo(String addr) {
    List<IPBlockInfo> list = jdbcTemplate.query(
            "SELECT ip, reason, ban_date, date, mod_id, allow_posting, captcha_required FROM b_ips WHERE ip = ?::inet",
            (rs, rowNum) -> new IPBlockInfo(rs),
            addr
    );

    if (list.isEmpty()) {
      return new IPBlockInfo(addr);
    } else {
      return list.get(0);
    }
  }

  public void checkBlockIP(@Nonnull String addr, @Nonnull Errors errors, @Nullable User user) {
    checkBlockIP(getBlockInfo(addr), errors, user);
  }

  public static void checkBlockIP(@Nonnull IPBlockInfo block, @Nonnull Errors errors, @Nullable User user) {

    if (block.isBlocked() && (user == null || user.isAnonymousScore() || !block.isAllowRegistredPosting())) {
      errors.reject(null, "Постинг заблокирован: " + block.getReason());
    }
  }

  public void blockIP(String ip, User moderator, String reason, Timestamp ts,
                      boolean allow_posting, boolean captcha_required) {
    IPBlockInfo blockInfo = getBlockInfo(ip);

    if (!blockInfo.isInitialized()) {
      jdbcTemplate.update(
              "INSERT INTO b_ips (ip, mod_id, date, reason, ban_date, allow_posting, captcha_required)"+
                " VALUES (?::inet, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?)",
              ip,
              moderator.getId(),
              reason,
              ts,
              allow_posting,
              captcha_required
      );
    } else {
      jdbcTemplate.update(
              "UPDATE b_ips SET mod_id=?,date=CURRENT_TIMESTAMP, reason=?, ban_date=?, allow_posting=?, captcha_required=?"+
                " WHERE ip=?::inet",
              moderator.getId(),
              reason,
              ts,
              allow_posting,
              captcha_required,
              ip
      );
    }
  }
}
