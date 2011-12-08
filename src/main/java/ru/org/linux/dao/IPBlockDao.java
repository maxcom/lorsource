/*
 * Copyright 1998-2011 Linux.org.ru
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

package ru.org.linux.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.validation.Errors;
import org.xbill.DNS.TextParseException;
import ru.org.linux.dto.IPBlockInfoDto;
import ru.org.linux.dto.UserDto;
import ru.org.linux.util.DNSBLClient;

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

  /**
   *
   * @param addr
   * @return
   */
  public IPBlockInfoDto getBlockInfo(String addr) {
    List<IPBlockInfoDto> list = jdbcTemplate.query(
            "SELECT reason, ban_date, date, mod_id FROM b_ips WHERE ip = ?::inet",
            new RowMapper<IPBlockInfoDto>() {
              @Override
              public IPBlockInfoDto mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new IPBlockInfoDto(rs);
              }
            },
            addr
    );

    if (list.isEmpty()) {
      return null;
    } else {
      return list.get(0);
    }
  }

  /**
   *
   * @param addr
   * @return
   * @throws TextParseException
   * @throws UnknownHostException
   */
//TODO: это должно быть в сервисе
  public static boolean getTor(String addr) throws TextParseException, UnknownHostException {
    DNSBLClient dnsbl = new DNSBLClient("tor.ahbl.org");
    return (dnsbl.checkIP(addr));
  }

  /**
   *
   * @param addr
   * @param errors
   * @throws UnknownHostException
   * @throws TextParseException
   */
//TODO: это должно быть в сервисе
  public void checkBlockIP(String addr, Errors errors) throws UnknownHostException, TextParseException {
    if (getTor(addr)) {
      errors.reject(null, "Постинг заблокирован: tor.ahbl.org");
    }

    IPBlockInfoDto blockDto = getBlockInfo(addr);

    if (blockDto == null) {
      return;
    }

    if (blockDto.isBlocked()) {
      errors.reject(null, "Постинг заблокирован: "+ blockDto.getReason());
    }
  }

  /**
   *
   * @param ip
   * @param moderator
   * @param reason
   * @param ts
   */
  public void blockIP(String ip, UserDto moderator, String reason, Timestamp ts) {
    IPBlockInfoDto blockInfoDto = getBlockInfo(ip);

    if (blockInfoDto == null) {
      jdbcTemplate.update(
              "INSERT INTO b_ips (ip, mod_id, date, reason, ban_date) VALUES (?::inet, ?, CURRENT_TIMESTAMP, ?, ?)",
              ip,
              moderator.getId(),
              reason,
              ts
      );
    } else {
      jdbcTemplate.update(
              "UPDATE b_ips SET mod_id=?,date=CURRENT_TIMESTAMP, reason=?, ban_date=? WHERE ip=?::inet",
              moderator.getId(),
              reason,
              ts,
              ip
      );
    }
  }

  /**
   * Удаление информации о забаненном адресе.
   *
   * @param ip IP-адрес
   */
  public void delete(String ip) {
    jdbcTemplate.update( "DELETE FROM b_ips WHERE ip =  ?::inet", ip );
  }
}
