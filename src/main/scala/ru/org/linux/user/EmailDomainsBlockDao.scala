/*
 * Copyright 1998-2025 Linux.org.ru
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

import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

import java.time.OffsetDateTime
import javax.sql.DataSource

@Service
class EmailDomainsBlockDao(ds: DataSource) {
  private val jdbcTemplate = new JdbcTemplate(ds)

  def isBlocked(domain: String): Boolean =
    jdbcTemplate.queryForObject[Boolean]("select exists (select block_until from email_domains_block where domain = ? and block_until > CURRENT_TIMESTAMP)", domain).get

  def blockDomains(domains: Seq[String], blockUntil: OffsetDateTime): Unit = {
    jdbcTemplate.batchUpdate(
      "insert into email_domains_block (domain, block_until) values (?, ?) " +
        "on conflict (domain) do update set block_until = excluded.block_until",
      domains.map(domain => Seq(domain, blockUntil)))
  }
}
