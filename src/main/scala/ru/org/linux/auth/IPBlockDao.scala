/*
 * Copyright 1998-2026 Linux.org.ru
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

package ru.org.linux.auth

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

import java.time.OffsetDateTime
import javax.sql.DataSource
import scala.jdk.CollectionConverters.*

@Repository
class IPBlockDao(ds: DataSource) {
  private val jdbcTemplate = new JdbcTemplate(ds)

  def getBlockInfo(addr: String): IPBlockInfo = {
    val list = jdbcTemplate.query(
      "SELECT ip, reason, ban_date, date, mod_id, allow_posting, captcha_required FROM b_ips WHERE ip = ?::inet",
      (rs, _) => IPBlockInfo.fromResultSet(rs),
      addr
    )

    if (list.isEmpty) {
      IPBlockInfo(addr)
    } else {
      list.getFirst
    }
  }

  def blockIP(ip: String, moderatorId: Int, reason: String, banUntil: Option[OffsetDateTime],
              allowPosting: Boolean, captchaRequired: Boolean): Unit = {
    val blockInfo = getBlockInfo(ip)

    if (!blockInfo.initialized) {
      jdbcTemplate.update(
        "INSERT INTO b_ips (ip, mod_id, date, reason, ban_date, allow_posting, captcha_required)" +
          " VALUES (?::inet, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?)",
        ip,
        Int.box(moderatorId),
        reason,
        banUntil.orNull,
        Boolean.box(allowPosting),
        Boolean.box(captchaRequired))
    } else {
      jdbcTemplate.update(
        "UPDATE b_ips SET mod_id=?,date=CURRENT_TIMESTAMP, reason=?, ban_date=?, allow_posting=?, captcha_required=?" +
          " WHERE ip=?::inet",
        Int.box(moderatorId),
        reason,
        banUntil.orNull,
        Boolean.box(allowPosting),
        Boolean.box(captchaRequired),
        ip)
    }
  }

  def getRecentlyBlocked: scala.collection.Seq[String] =
    jdbcTemplate.queryForList("select ip from b_ips " +
      "where date>CURRENT_TIMESTAMP - interval '3 days' and ban_date > CURRENT_TIMESTAMP and mod_id != 0 order by date", classOf[String]).asScala

  def getRecentlyUnBlocked: scala.collection.Seq[String] =
    jdbcTemplate.queryForList("select ip from b_ips " +
      "where ban_date < CURRENT_TIMESTAMP and ban_date > CURRENT_TIMESTAMP - interval '3 days' and mod_id !=0 order by ban_date", classOf[String]).asScala
}
