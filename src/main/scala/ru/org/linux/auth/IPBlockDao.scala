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

import org.springframework.stereotype.Repository
import ru.org.linux.scalikejdbc.SpringDB
import scalikejdbc.*

import java.time.OffsetDateTime

@Repository
class IPBlockDao(springDB: SpringDB):

  def getBlockInfo(addr: String): IPBlockInfo = springDB.run(getBlockInfoInternal(addr))

  def blockIP(
      ip: String,
      moderatorId: Int,
      reason: String,
      banUntil: Option[OffsetDateTime],
      allowPosting: Boolean,
      captchaRequired: Boolean): Unit =
    springDB.run:
      sql"""INSERT INTO b_ips (ip, mod_id, date, reason, ban_date, allow_posting, captcha_required)
            VALUES ($ip::inet, $moderatorId, CURRENT_TIMESTAMP, $reason, $banUntil, $allowPosting, $captchaRequired)
            ON CONFLICT (ip) DO UPDATE SET
              mod_id = EXCLUDED.mod_id,
              date = CURRENT_TIMESTAMP,
              reason = EXCLUDED.reason,
              ban_date = EXCLUDED.ban_date,
              allow_posting = EXCLUDED.allow_posting,
              captcha_required = EXCLUDED.captcha_required"""
        .update
        .apply()

  def getRecentlyBlocked: Seq[String] = springDB.run(getRecentlyBlockedInternal)

  def getRecentlyUnBlocked: Seq[String] = springDB.run(getRecentlyUnBlockedInternal)

  private def getBlockInfoInternal(addr: String)(using DBSession): IPBlockInfo =
    sql"""SELECT ip, reason, ban_date, date, mod_id, allow_posting, captcha_required
          FROM b_ips WHERE ip = $addr::inet"""
      .map(IPBlockInfo.fromWrappedResultSet)
      .single
      .apply()
      .getOrElse(IPBlockInfo(addr))

  private def getRecentlyBlockedInternal(using DBSession): Seq[String] =
    sql"""SELECT ip FROM b_ips
          WHERE date > CURRENT_TIMESTAMP - interval '3 days'
          AND ban_date > CURRENT_TIMESTAMP AND mod_id != 0
          ORDER BY date""".map(rs => rs.string("ip")).list.apply()

  private def getRecentlyUnBlockedInternal(using DBSession): Seq[String] =
    sql"""SELECT ip FROM b_ips
          WHERE ban_date < CURRENT_TIMESTAMP
          AND ban_date > CURRENT_TIMESTAMP - interval '3 days' AND mod_id != 0
          ORDER BY ban_date""".map(rs => rs.string("ip")).list.apply()

end IPBlockDao
