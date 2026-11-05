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

import munit.FunSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.test.TransactionalTestSupport

import java.sql.Timestamp
import java.time.OffsetDateTime

@ContextConfiguration(classes = Array(classOf[IpBlockDaoAutoBlockIntegrationTestConfiguration]))
class IpBlockDaoAutoBlockIntegrationTest extends FunSuite with TransactionalTestSupport:

  @Autowired
  var ipBlockDao: IpBlockDao = scala.compiletime.uninitialized

  test("autoBlockIPs inserts new ips"):
    ipBlockDao.autoBlockIPs(
      Seq("10.1.0.1", "10.1.0.2"),
      "TOR Exit Node",
      OffsetDateTime.now.plusMonths(1),
      allowPosting = true,
      captchaRequired = false)

    for ip <- Seq("10.1.0.1", "10.1.0.2") do
      val info = ipBlockDao.getBlockInfo(ip)
      assert(info.initialized, s"Should be initialized after batch insert: $ip")
      assert(info.isBlocked, s"Should be blocked: $ip")
      assertEquals(info.reason, "TOR Exit Node")
      assertEquals(info.moderator, 0)
      assert(info.isAllowRegisteredPosting)

  test("autoBlockIPs does not refresh auto row while ban date is far away"):
    ipBlockDao.blockIP(
      "10.1.1.1",
      0,
      "TOR Exit Node",
      Some(OffsetDateTime.now.plusDays(20)),
      allowPosting = true,
      captchaRequired = false)

    ipBlockDao.autoBlockIPs(
      Seq("10.1.1.1"),
      "TOR Exit Node",
      OffsetDateTime.now.plusMonths(1),
      allowPosting = false,
      captchaRequired = true)

    val info = ipBlockDao.getBlockInfo("10.1.1.1")
    assertEquals(info.reason, "TOR Exit Node")
    assert(
      info.banDate.after(ts(OffsetDateTime.now.plusDays(19))) &&
        info.banDate.before(ts(OffsetDateTime.now.plusDays(21))),
      "ban_date should stay untouched (~20 days)"
    )
    assert(info.isAllowRegisteredPosting, "allow_posting should stay untouched")

  test("autoBlockIPs refreshes auto row when ban is almost expired"):
    ipBlockDao.blockIP(
      "10.1.2.1",
      0,
      "TOR Exit Node",
      Some(OffsetDateTime.now.plusDays(3)),
      allowPosting = false,
      captchaRequired = false)

    ipBlockDao.autoBlockIPs(
      Seq("10.1.2.1"),
      "TOR Exit Node",
      OffsetDateTime.now.plusMonths(1),
      allowPosting = true,
      captchaRequired = false)

    val info = ipBlockDao.getBlockInfo("10.1.2.1")
    assert(info.banDate.after(ts(OffsetDateTime.now.plusDays(10))), "ban_date should be extended")
    assert(info.isAllowRegisteredPosting, "allow_posting should be refreshed")

  test("autoBlockIPs does not clobber active manual block"):
    ipBlockDao.blockIP(
      "10.1.3.1",
      42,
      "manual block",
      Some(OffsetDateTime.now.plusDays(10)),
      allowPosting = false,
      captchaRequired = true)

    ipBlockDao.autoBlockIPs(
      Seq("10.1.3.1"),
      "TOR Exit Node",
      OffsetDateTime.now.plusMonths(1),
      allowPosting = true,
      captchaRequired = false)

    val info = ipBlockDao.getBlockInfo("10.1.3.1")
    assertEquals(info.moderator, 42)
    assertEquals(info.reason, "manual block")
    assert(info.isBlocked)
    assert(!info.isAllowRegisteredPosting, "manual allow_posting should win")
    assert(info.captchaRequired, "manual captcha_required should win")

  test("autoBlockIPs overwrites expired manual block"):
    ipBlockDao.blockIP(
      "10.1.4.1",
      42,
      "expired manual block",
      Some(OffsetDateTime.now.minusDays(1)),
      allowPosting = false,
      captchaRequired = false)

    ipBlockDao.autoBlockIPs(
      Seq("10.1.4.1"),
      "TOR Exit Node",
      OffsetDateTime.now.plusMonths(1),
      allowPosting = true,
      captchaRequired = false)

    val info = ipBlockDao.getBlockInfo("10.1.4.1")
    assertEquals(info.moderator, 0)
    assertEquals(info.reason, "TOR Exit Node")
    assert(info.isBlocked)
    assert(info.isAllowRegisteredPosting)

  test("deleteExpiredAutoBlocks removes only stale auto rows"):
    // устаревшая авто-блокировка — удаляется
    ipBlockDao.blockIP(
      "10.1.5.1",
      0,
      "TOR Exit Node",
      Some(OffsetDateTime.now.minusDays(30)),
      allowPosting = true,
      captchaRequired = false)
    // недавно истёкшая авто-блокировка — остаётся
    ipBlockDao.blockIP(
      "10.1.5.2",
      0,
      "TOR Exit Node",
      Some(OffsetDateTime.now.minusDays(5)),
      allowPosting = true,
      captchaRequired = false)
    // устаревшая ручная блокировка — остаётся
    ipBlockDao.blockIP(
      "10.1.5.3",
      7,
      "manual block",
      Some(OffsetDateTime.now.minusDays(30)),
      allowPosting = false,
      captchaRequired = false)

    val deleted = ipBlockDao.deleteExpiredAutoBlocks

    assertEquals(deleted, 1)
    assert(!ipBlockDao.getBlockInfo("10.1.5.1").initialized)
    assert(ipBlockDao.getBlockInfo("10.1.5.2").initialized)
    assert(ipBlockDao.getBlockInfo("10.1.5.3").initialized)

  private def ts(dt: OffsetDateTime): Timestamp = new Timestamp(dt.toInstant.toEpochMilli)

end IpBlockDaoAutoBlockIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml"))
class IpBlockDaoAutoBlockIntegrationTestConfiguration:

  @Bean
  def ipBlockDao(springDB: SpringDB) = new IpBlockDao(springDB)

end IpBlockDaoAutoBlockIntegrationTestConfiguration
