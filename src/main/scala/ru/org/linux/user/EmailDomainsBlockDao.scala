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

package ru.org.linux.user

import org.springframework.stereotype.Service
import ru.org.linux.scalikejdbc.SpringDB
import scalikejdbc.*

import java.time.OffsetDateTime

object EmailDomainsBlockDao:
  case class EmailDomainBlock(
      domain: String,
      blockUntil: OffsetDateTime,
      moderatorId: Option[Int],
      blockedAt: OffsetDateTime,
      auto: Boolean)

@Service
class EmailDomainsBlockDao(springDB: SpringDB):
  def isBlocked(domain: String): Boolean =
    springDB.run:
      sql"select exists (select block_until from email_domains_block where domain = $domain and block_until > CURRENT_TIMESTAMP)"
        .map(rs => rs.boolean(1))
        .single
        .apply()
        .getOrElse(false)

  /** Автоматическая блокировка списком из disposable-email-domains. Не перекрывает ручную блокировку: для существующих
    * ручных записей (auto=false) UPDATE не выполняется (фильтр WHERE email_domains_block.auto).
    */
  def blockDomains(domains: Seq[String], blockUntil: OffsetDateTime): Unit =
    springDB.run:
      sql"""insert into email_domains_block (domain, block_until, auto, moderator_id, blocked_at)
            values ({domain}, {blockUntil}, true, null, CURRENT_TIMESTAMP)
            on conflict (domain) do update
              set block_until = excluded.block_until,
                  blocked_at = excluded.blocked_at,
                  auto = true
            where email_domains_block.auto"""
        .batchByName(domains.map(d => Seq("domain" -> d, "blockUntil" -> blockUntil))*)
        .apply()

  /** Ручная блокировка модератором. Перекрывает автоматическую и продлевает срок. */
  def blockDomainManual(domain: String, blockUntil: OffsetDateTime, moderatorId: Int): Unit =
    springDB.run:
      sql"""insert into email_domains_block (domain, block_until, auto, moderator_id, blocked_at)
            values ($domain, $blockUntil, false, $moderatorId, CURRENT_TIMESTAMP)
            on conflict (domain) do update
              set block_until = excluded.block_until,
                  auto = false,
                  moderator_id = excluded.moderator_id,
                  blocked_at = CURRENT_TIMESTAMP""".update.apply()

  def unblockDomain(domain: String): Unit =
    springDB.run:
      sql"delete from email_domains_block where domain = $domain".update.apply()

  def getManualDomains(offset: Int, limit: Int): Seq[EmailDomainsBlockDao.EmailDomainBlock] =
    springDB.run:
      sql"""select domain, block_until, moderator_id, blocked_at, auto
            from email_domains_block
            where auto = false
            order by domain
            limit $limit offset $offset"""
        .map(rs =>
          EmailDomainsBlockDao.EmailDomainBlock(
            domain = rs.string("domain"),
            blockUntil = rs.offsetDateTime("block_until"),
            moderatorId = rs.intOpt("moderator_id"),
            blockedAt = rs.offsetDateTime("blocked_at"),
            auto = rs.boolean("auto")
          ))
        .list
        .apply()

  def manualCount: Long =
    springDB.run:
      sql"select count(*) from email_domains_block where auto = false"
        .map(rs => rs.long(1))
        .single
        .apply()
        .getOrElse(0L)
end EmailDomainsBlockDao
