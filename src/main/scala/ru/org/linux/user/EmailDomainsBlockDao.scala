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

@Service
class EmailDomainsBlockDao(springDB: SpringDB):
  def isBlocked(domain: String): Boolean =
    springDB.run:
      sql"select exists (select block_until from email_domains_block where domain = $domain and block_until > CURRENT_TIMESTAMP)"
        .map(rs => rs.boolean(1))
        .single
        .apply()
        .getOrElse(false)

  def blockDomains(domains: Seq[String], blockUntil: OffsetDateTime): Unit =
    springDB.run:
      sql"insert into email_domains_block (domain, block_until) values ({domain}, {blockUntil}) on conflict (domain) do update set block_until = excluded.block_until"
        .batchByName(domains.map(d => Seq("domain" -> d, "blockUntil" -> blockUntil))*)
        .apply()
