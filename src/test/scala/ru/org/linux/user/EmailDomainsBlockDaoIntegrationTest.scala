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

import org.junit.Assert.*
import org.junit.{After, Before, Test}
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional
import ru.org.linux.scalikejdbc.SpringDB
import scalikejdbc.*

object EmailDomainsBlockDaoIntegrationTest:
  private val ManualDomain = "manual-example-test.example"
  private val AutoDomain = "auto-example-test.example"
  private val ModeratorId = 2

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(classes = Array(classOf[EmailDomainsBlockDaoIntegrationTestConfiguration])) @Transactional
class EmailDomainsBlockDaoIntegrationTest:

  @Autowired
  var dao: EmailDomainsBlockDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  @Before @After
  def cleanup(): Unit =
    springDB.run:
      sql"delete from email_domains_block where domain in (${EmailDomainsBlockDaoIntegrationTest
          .ManualDomain}, ${EmailDomainsBlockDaoIntegrationTest.AutoDomain})".update.apply()

  @Test
  def testIsBlockedEmpty(): Unit = assertFalse(dao.isBlocked(EmailDomainsBlockDaoIntegrationTest.ManualDomain))

  @Test
  def testManualBlock(): Unit =
    val until = java.time.OffsetDateTime.now.plusYears(3)
    dao.blockDomainManual(
      EmailDomainsBlockDaoIntegrationTest.ManualDomain,
      until,
      EmailDomainsBlockDaoIntegrationTest.ModeratorId)

    assertTrue(dao.isBlocked(EmailDomainsBlockDaoIntegrationTest.ManualDomain))

    val blocks = dao.getManualDomains(0, 50)
    val block = blocks.find(_.domain == EmailDomainsBlockDaoIntegrationTest.ManualDomain)
    assertTrue("Manual block should be in list", block.isDefined)
    assertFalse("Manual block should have auto=false", block.get.auto)
    assertEquals(
      "Manual block should have moderator",
      Some(EmailDomainsBlockDaoIntegrationTest.ModeratorId),
      block.get.moderatorId)

  @Test
  def testManualBlockExtends(): Unit =
    dao.blockDomainManual(
      EmailDomainsBlockDaoIntegrationTest.ManualDomain,
      java.time.OffsetDateTime.now.plusDays(10),
      EmailDomainsBlockDaoIntegrationTest.ModeratorId)
    val firstUntil =
      dao.getManualDomains(0, 50).find(_.domain == EmailDomainsBlockDaoIntegrationTest.ManualDomain).get.blockUntil

    dao.blockDomainManual(
      EmailDomainsBlockDaoIntegrationTest.ManualDomain,
      java.time.OffsetDateTime.now.plusYears(3),
      EmailDomainsBlockDaoIntegrationTest.ModeratorId)
    val secondUntil =
      dao.getManualDomains(0, 50).find(_.domain == EmailDomainsBlockDaoIntegrationTest.ManualDomain).get.blockUntil

    assertTrue("Block should be extended", secondUntil.isAfter(firstUntil))
    assertEquals(1L, dao.manualCount)

  @Test
  def testAutoDoesNotOverrideManual(): Unit =
    dao.blockDomainManual(
      EmailDomainsBlockDaoIntegrationTest.ManualDomain,
      java.time.OffsetDateTime.now.plusYears(3),
      EmailDomainsBlockDaoIntegrationTest.ModeratorId)

    val before = dao.getManualDomains(0, 50).find(_.domain == EmailDomainsBlockDaoIntegrationTest.ManualDomain).get

    dao.blockDomains(Seq(EmailDomainsBlockDaoIntegrationTest.ManualDomain), java.time.OffsetDateTime.now.plusDays(7))

    val after = dao.getManualDomains(0, 50).find(_.domain == EmailDomainsBlockDaoIntegrationTest.ManualDomain).get

    assertFalse("After auto-update block should still be manual", after.auto)
    assertEquals(
      "Moderator should be preserved",
      Some(EmailDomainsBlockDaoIntegrationTest.ModeratorId),
      after.moderatorId)
    assertEquals("block_until should not change", before.blockUntil, after.blockUntil)

  @Test
  def testAutoBlockDoesNotAppearInManualList(): Unit =
    dao.blockDomains(Seq(EmailDomainsBlockDaoIntegrationTest.AutoDomain), java.time.OffsetDateTime.now.plusDays(7))

    assertTrue(dao.isBlocked(EmailDomainsBlockDaoIntegrationTest.AutoDomain))
    val blocks = dao.getManualDomains(0, 50)
    assertTrue(
      "Auto block should not appear in manual list",
      blocks.forall(_.domain != EmailDomainsBlockDaoIntegrationTest.AutoDomain))

  @Test
  def testManualOverridesAuto(): Unit =
    dao.blockDomains(Seq(EmailDomainsBlockDaoIntegrationTest.AutoDomain), java.time.OffsetDateTime.now.plusDays(7))

    dao.blockDomainManual(
      EmailDomainsBlockDaoIntegrationTest.AutoDomain,
      java.time.OffsetDateTime.now.plusYears(3),
      EmailDomainsBlockDaoIntegrationTest.ModeratorId)

    val block = dao.getManualDomains(0, 50).find(_.domain == EmailDomainsBlockDaoIntegrationTest.AutoDomain).get
    assertFalse("After manual override block should be manual", block.auto)

  @Test
  def testAutoBlockBlockedAtIsCreationTime(): Unit =
    val blockUntil = java.time.OffsetDateTime.now.plusDays(7)
      .truncatedTo(java.time.temporal.ChronoUnit.MICROS)
    val beforeInsert = java.time.OffsetDateTime.now

    dao.blockDomains(Seq(EmailDomainsBlockDaoIntegrationTest.AutoDomain), blockUntil)

    val row = springDB.run:
      sql"select blocked_at, block_until from email_domains_block where domain = ${EmailDomainsBlockDaoIntegrationTest
          .AutoDomain}"
        .map(rs => (rs.offsetDateTime("blocked_at"), rs.offsetDateTime("block_until")))
        .single
        .apply()
        .get

    assertTrue("blocked_at should be the creation time, not the expiry", row._1.isBefore(blockUntil))
    assertTrue("blocked_at should be at or after the insert moment", !row._1.isBefore(beforeInsert.minusSeconds(2)))
    assertEquals("block_until should match the requested expiry", blockUntil, row._2)

  @Test
  def testUnblock(): Unit =
    dao.blockDomainManual(
      EmailDomainsBlockDaoIntegrationTest.ManualDomain,
      java.time.OffsetDateTime.now.plusYears(3),
      EmailDomainsBlockDaoIntegrationTest.ModeratorId)

    dao.unblockDomain(EmailDomainsBlockDaoIntegrationTest.ManualDomain)
    assertFalse(dao.isBlocked(EmailDomainsBlockDaoIntegrationTest.ManualDomain))

end EmailDomainsBlockDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml"))
class EmailDomainsBlockDaoIntegrationTestConfiguration:
  @Bean
  def emailDomainsBlockDao(springDB: SpringDB) = new EmailDomainsBlockDao(springDB)
end EmailDomainsBlockDaoIntegrationTestConfiguration
