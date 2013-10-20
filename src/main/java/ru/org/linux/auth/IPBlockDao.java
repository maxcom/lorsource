/*
 * Copyright 1998-2013 Linux.org.ru
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
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.validation.Errors;
import org.xbill.DNS.TextParseException;
import ru.org.linux.user.User;
import ru.org.linux.util.DNSBLClient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
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
            new RowMapper<IPBlockInfo>() {
              @Override
              public IPBlockInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new IPBlockInfo(rs);
              }
            },
            addr
    );

    if (list.isEmpty()) {
      return new IPBlockInfo(addr);
    } else {
      return list.get(0);
    }
  }

  public static boolean getTor(String addr) throws TextParseException, UnknownHostException {
    DNSBLClient dnsbl = new DNSBLClient("tor.ahbl.org");
    return (dnsbl.checkIP(addr));
  }

  public void checkBlockIP(@Nonnull String addr, @Nonnull Errors errors, @Nullable User user)
          throws UnknownHostException, TextParseException {
    checkBlockIP(getBlockInfo(addr), errors, user);
  }

  public static void checkBlockIP(@Nonnull IPBlockInfo block, @Nonnull Errors errors, @Nullable User user)
    throws UnknownHostException, TextParseException {
    
    boolean isAnonymous = (user == null || user.isAnonymousScore()
          || !block.isAllowRegistredPosting());

    if (isAnonymous) {
        if (getTor(block.getIp())) {
             errors.reject(null, "Постинг заблокирован: tor.ahbl.org");
     }

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
