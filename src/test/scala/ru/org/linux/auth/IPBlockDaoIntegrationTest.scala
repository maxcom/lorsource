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

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional
import ru.org.linux.scalikejdbc.ScalikeJdbcInitializer

import javax.sql.DataSource

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(classes = Array(classOf[IPBlockDaoIntegrationTestConfiguration])) @Transactional
class IPBlockDaoIntegrationTest:

  @Autowired
  var ipBlockDao: IPBlockDao = scala.compiletime.uninitialized

  @Test
  def testGetBlockInfoNotBlocked(): Unit =
    val info = ipBlockDao.getBlockInfo("192.168.1.1")
    assertFalse("Should not be initialized for unknown IP", info.initialized)
    assertEquals("192.168.1.1", info.ip)
    assertFalse("Should not be blocked for unknown IP", info.isBlocked)

  @Test
  def testBlockIPAndRetrieve(): Unit =
    ipBlockDao.blockIP("10.0.0.1", 1, "test reason", None, allowPosting = false, captchaRequired = true)

    val info = ipBlockDao.getBlockInfo("10.0.0.1")
    assertTrue("Should be initialized after blocking", info.initialized)
    assertEquals("10.0.0.1", info.ip)
    assertEquals("test reason", info.reason)
    assertTrue("Should be blocked", info.isBlocked)
    assertFalse("Should not allow posting", info.isAllowRegisteredPosting)
    assertTrue("Should require captcha", info.captchaRequired)
    assertEquals(1, info.moderator)

  @Test
  def testBlockIPWithBanDate(): Unit =
    val banUntil = java.time.OffsetDateTime.now.plusDays(7)
    ipBlockDao.blockIP("10.0.0.2", 2, "temporary ban", Some(banUntil), allowPosting = true, captchaRequired = false)

    val info = ipBlockDao.getBlockInfo("10.0.0.2")
    assertTrue("Should be initialized", info.initialized)
    assertTrue("Should be blocked with future ban date", info.isBlocked)
    assertTrue("Should allow registered posting", info.isAllowRegisteredPosting)
    assertFalse("Should not require captcha", info.captchaRequired)

  @Test
  def testUpdateBlock(): Unit =
    ipBlockDao.blockIP("10.0.0.3", 1, "initial reason", None, allowPosting = false, captchaRequired = false)
    ipBlockDao.blockIP("10.0.0.3", 2, "updated reason", None, allowPosting = true, captchaRequired = true)

    val info = ipBlockDao.getBlockInfo("10.0.0.3")
    assertTrue("Should be initialized", info.initialized)
    assertEquals("updated reason", info.reason)
    assertEquals(2, info.moderator)
    assertTrue("Should allow registered posting after update", info.isAllowRegisteredPosting)

  @Test
  def testUnblockBySettingPastBanDate(): Unit =
    ipBlockDao.blockIP(
      "10.0.0.4",
      1,
      "will expire",
      Some(java.time.OffsetDateTime.now.minusDays(1)),
      allowPosting = false,
      captchaRequired = false)

    val info = ipBlockDao.getBlockInfo("10.0.0.4")
    assertTrue("Should be initialized", info.initialized)
    assertFalse("Should not be blocked with past ban date", info.isBlocked)

  @Test
  def testGetRecentlyBlocked(): Unit =
    ipBlockDao.blockIP(
      "10.0.0.5",
      1,
      "recent block",
      Some(java.time.OffsetDateTime.now.plusDays(7)),
      allowPosting = false,
      captchaRequired = false)

    val blocked = ipBlockDao.getRecentlyBlocked
    assertTrue("Should contain recently blocked IP", blocked.contains("10.0.0.5"))

  @Test
  def testGetRecentlyUnBlocked(): Unit =
    val expiredBan = java.time.OffsetDateTime.now.minusDays(1)
    ipBlockDao.blockIP("10.0.0.6", 1, "expired block", Some(expiredBan), allowPosting = false, captchaRequired = false)

    val unblocked = ipBlockDao.getRecentlyUnBlocked
    assertTrue("Should contain recently unblocked IP", unblocked.contains("10.0.0.6"))

end IPBlockDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml"))
class IPBlockDaoIntegrationTestConfiguration:

  @Bean
  def ipBlockDao: IPBlockDao = new IPBlockDao

  @Bean
  def scalikeJdbcInitializer(dataSource: DataSource): ScalikeJdbcInitializer = new ScalikeJdbcInitializer(dataSource)

end IPBlockDaoIntegrationTestConfiguration
