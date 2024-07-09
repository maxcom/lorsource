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

package ru.org.linux.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.validation.Errors;
import ru.org.linux.user.User;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class IPBlockDao {
  private final JdbcTemplate jdbcTemplate;

  public IPBlockDao(DataSource ds) {
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
      return list.getFirst();
    }
  }

  public void checkBlockIP(String addr, Errors errors, @Nullable User user) {
    checkBlockIP(getBlockInfo(addr), errors, user);
  }

  public static void checkBlockIP(IPBlockInfo block, Errors errors, @Nullable User user) {
    if (block.isBlocked() && (user == null || user.isAnonymousScore() || !block.isAllowRegistredPosting())) {
      errors.reject(null, "Постинг заблокирован: " + block.getReason());
    }
  }

  public void blockIP(String ip, int moderatorId, String reason, @Nullable Timestamp banUntil,
                      boolean allowPosting, boolean captchaRequired) {
    IPBlockInfo blockInfo = getBlockInfo(ip);

    if (!blockInfo.isInitialized()) {
      jdbcTemplate.update(
              "INSERT INTO b_ips (ip, mod_id, date, reason, ban_date, allow_posting, captcha_required)"+
                " VALUES (?::inet, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?)",
              ip,
              moderatorId,
              reason,
              banUntil,
              allowPosting,
              captchaRequired);
    } else {
      jdbcTemplate.update(
              "UPDATE b_ips SET mod_id=?,date=CURRENT_TIMESTAMP, reason=?, ban_date=?, allow_posting=?, captcha_required=?"+
                " WHERE ip=?::inet",
              moderatorId,
              reason,
              banUntil,
              allowPosting,
              captchaRequired,
              ip);
    }
  }

  public List<String> getRecentlyBlocked() {
    return jdbcTemplate.queryForList("select ip from b_ips " +
            "where date>CURRENT_TIMESTAMP - interval '3 days' and ban_date > CURRENT_TIMESTAMP and mod_id != 0 order by date", String.class);
  }

  public List<String> getRecentlyUnBlocked() {
    return jdbcTemplate.queryForList("select ip from b_ips " +
            "where ban_date < CURRENT_TIMESTAMP and ban_date > CURRENT_TIMESTAMP - interval '3 days' and mod_id !=0 order by ban_date", String.class);
  }
}
