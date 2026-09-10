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

@ContextConfiguration(classes = Array(classOf[IPBlockDaoIntegrationTestConfiguration]))
class IpBlockDaoIntegrationTest extends FunSuite with TransactionalTestSupport:

  @Autowired
  var ipBlockDao: IpBlockDao = scala.compiletime.uninitialized

  test("getBlockInfoNotBlocked"):
    val info = ipBlockDao.getBlockInfo("192.168.1.1")
    assert(!info.initialized, "Should not be initialized for unknown IP")
    assertEquals(info.ip, "192.168.1.1")
    assert(!info.isBlocked, "Should not be blocked for unknown IP")

  test("blockIPAndRetrieve"):
    ipBlockDao.blockIP("10.0.0.1", 1, "test reason", None, allowPosting = false, captchaRequired = true)

    val info = ipBlockDao.getBlockInfo("10.0.0.1")
    assert(info.initialized, "Should be initialized after blocking")
    assertEquals(info.ip, "10.0.0.1")
    assertEquals(info.reason, "test reason")
    assert(info.isBlocked, "Should be blocked")
    assert(!info.isAllowRegisteredPosting, "Should not allow posting")
    assert(info.captchaRequired, "Should require captcha")
    assertEquals(info.moderator, 1)

  test("blockIPWithBanDate"):
    val banUntil = java.time.OffsetDateTime.now.plusDays(7)
    ipBlockDao.blockIP("10.0.0.2", 2, "temporary ban", Some(banUntil), allowPosting = true, captchaRequired = false)

    val info = ipBlockDao.getBlockInfo("10.0.0.2")
    assert(info.initialized, "Should be initialized")
    assert(info.isBlocked, "Should be blocked with future ban date")
    assert(info.isAllowRegisteredPosting, "Should allow registered posting")
    assert(!info.captchaRequired, "Should not require captcha")

  test("updateBlock"):
    ipBlockDao.blockIP("10.0.0.3", 1, "initial reason", None, allowPosting = false, captchaRequired = false)
    ipBlockDao.blockIP("10.0.0.3", 2, "updated reason", None, allowPosting = true, captchaRequired = true)

    val info = ipBlockDao.getBlockInfo("10.0.0.3")
    assert(info.initialized, "Should be initialized")
    assertEquals(info.reason, "updated reason")
    assertEquals(info.moderator, 2)
    assert(info.isAllowRegisteredPosting, "Should allow registered posting after update")

  test("unblockBySettingPastBanDate"):
    ipBlockDao.blockIP(
      "10.0.0.4",
      1,
      "will expire",
      Some(java.time.OffsetDateTime.now.minusDays(1)),
      allowPosting = false,
      captchaRequired = false)

    val info = ipBlockDao.getBlockInfo("10.0.0.4")
    assert(info.initialized, "Should be initialized")
    assert(!info.isBlocked, "Should not be blocked with past ban date")

  test("getRecentlyBlocked"):
    ipBlockDao.blockIP(
      "10.0.0.5",
      1,
      "recent block",
      Some(java.time.OffsetDateTime.now.plusDays(7)),
      allowPosting = false,
      captchaRequired = false)

    val blocked = ipBlockDao.getRecentlyBlocked
    assert(blocked.contains("10.0.0.5"), "Should contain recently blocked IP")

  test("getRecentlyUnBlocked"):
    val expiredBan = java.time.OffsetDateTime.now.minusDays(1)
    ipBlockDao.blockIP("10.0.0.6", 1, "expired block", Some(expiredBan), allowPosting = false, captchaRequired = false)

    val unblocked = ipBlockDao.getRecentlyUnBlocked
    assert(unblocked.contains("10.0.0.6"), "Should contain recently unblocked IP")

end IpBlockDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml"))
class IPBlockDaoIntegrationTestConfiguration:

  @Bean
  def ipBlockDao(springDB: SpringDB) = new IpBlockDao(springDB)

end IPBlockDaoIntegrationTestConfiguration
