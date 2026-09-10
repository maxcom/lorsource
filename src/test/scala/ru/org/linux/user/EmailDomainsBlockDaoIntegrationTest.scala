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

import munit.FunSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.test.TransactionalTestSupport
import scalikejdbc.*

object EmailDomainsBlockDaoIntegrationTest:
  private val ManualDomain = "manual-example-test.example"
  private val AutoDomain = "auto-example-test.example"
  private val ModeratorId = 2

@ContextConfiguration(classes = Array(classOf[EmailDomainsBlockDaoIntegrationTestConfiguration]))
class EmailDomainsBlockDaoIntegrationTest extends FunSuite with TransactionalTestSupport:

  @Autowired
  var dao: EmailDomainsBlockDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    cleanup()

  override def afterEach(context: AfterEach): Unit =
    cleanup()
    super.afterEach(context)

  private def cleanup(): Unit =
    springDB.run:
      sql"delete from email_domains_block where domain in (${EmailDomainsBlockDaoIntegrationTest
          .ManualDomain}, ${EmailDomainsBlockDaoIntegrationTest.AutoDomain})".update.apply()

  test("isBlockedEmpty"):
    assert(!dao.isBlocked(EmailDomainsBlockDaoIntegrationTest.ManualDomain))

  test("manualBlock"):
    val until = java.time.OffsetDateTime.now.plusYears(3)
    dao.blockDomainManual(
      EmailDomainsBlockDaoIntegrationTest.ManualDomain,
      until,
      EmailDomainsBlockDaoIntegrationTest.ModeratorId)

    assert(dao.isBlocked(EmailDomainsBlockDaoIntegrationTest.ManualDomain))

    val blocks = dao.getManualDomains(0, 50)
    val block = blocks.find(_.domain == EmailDomainsBlockDaoIntegrationTest.ManualDomain)
    assert(block.isDefined, "Manual block should be in list")
    assert(!block.get.auto, "Manual block should have auto=false")
    assertEquals(
      block.get.moderatorId,
      Some(EmailDomainsBlockDaoIntegrationTest.ModeratorId),
      "Manual block should have moderator")

  test("manualBlockExtends"):
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

    assert(secondUntil.isAfter(firstUntil), "Block should be extended")
    assertEquals(dao.manualCount, 1L)

  test("autoDoesNotOverrideManual"):
    dao.blockDomainManual(
      EmailDomainsBlockDaoIntegrationTest.ManualDomain,
      java.time.OffsetDateTime.now.plusYears(3),
      EmailDomainsBlockDaoIntegrationTest.ModeratorId)

    val before = dao.getManualDomains(0, 50).find(_.domain == EmailDomainsBlockDaoIntegrationTest.ManualDomain).get

    dao.blockDomains(Seq(EmailDomainsBlockDaoIntegrationTest.ManualDomain), java.time.OffsetDateTime.now.plusDays(7))

    val after = dao.getManualDomains(0, 50).find(_.domain == EmailDomainsBlockDaoIntegrationTest.ManualDomain).get

    assert(!after.auto, "After auto-update block should still be manual")
    assertEquals(
      after.moderatorId,
      Some(EmailDomainsBlockDaoIntegrationTest.ModeratorId),
      "Moderator should be preserved")
    assertEquals(after.blockUntil, before.blockUntil, "block_until should not change")

  test("autoBlockDoesNotAppearInManualList"):
    dao.blockDomains(Seq(EmailDomainsBlockDaoIntegrationTest.AutoDomain), java.time.OffsetDateTime.now.plusDays(7))

    assert(dao.isBlocked(EmailDomainsBlockDaoIntegrationTest.AutoDomain))
    val blocks = dao.getManualDomains(0, 50)
    assert(
      blocks.forall(_.domain != EmailDomainsBlockDaoIntegrationTest.AutoDomain),
      "Auto block should not appear in manual list")

  test("manualOverridesAuto"):
    dao.blockDomains(Seq(EmailDomainsBlockDaoIntegrationTest.AutoDomain), java.time.OffsetDateTime.now.plusDays(7))

    dao.blockDomainManual(
      EmailDomainsBlockDaoIntegrationTest.AutoDomain,
      java.time.OffsetDateTime.now.plusYears(3),
      EmailDomainsBlockDaoIntegrationTest.ModeratorId)

    val block = dao.getManualDomains(0, 50).find(_.domain == EmailDomainsBlockDaoIntegrationTest.AutoDomain).get
    assert(!block.auto, "After manual override block should be manual")

  test("autoBlockBlockedAtIsCreationTime"):
    val blockUntil = java.time.OffsetDateTime.now.plusDays(7).truncatedTo(java.time.temporal.ChronoUnit.MICROS)
    val beforeInsert = java.time.OffsetDateTime.now

    dao.blockDomains(Seq(EmailDomainsBlockDaoIntegrationTest.AutoDomain), blockUntil)

    val row = springDB.run:
      sql"select blocked_at, block_until from email_domains_block where domain = ${EmailDomainsBlockDaoIntegrationTest
          .AutoDomain}"
        .map(rs => (rs.offsetDateTime("blocked_at"), rs.offsetDateTime("block_until")))
        .single
        .apply()
        .get

    assert(row._1.isBefore(blockUntil), "blocked_at should be the creation time, not the expiry")
    assert(!row._1.isBefore(beforeInsert.minusSeconds(2)), "blocked_at should be at or after the insert moment")
    assertEquals(row._2, blockUntil, "block_until should match the requested expiry")

  test("unblock"):
    dao.blockDomainManual(
      EmailDomainsBlockDaoIntegrationTest.ManualDomain,
      java.time.OffsetDateTime.now.plusYears(3),
      EmailDomainsBlockDaoIntegrationTest.ModeratorId)

    dao.unblockDomain(EmailDomainsBlockDaoIntegrationTest.ManualDomain)
    assert(!dao.isBlocked(EmailDomainsBlockDaoIntegrationTest.ManualDomain))

end EmailDomainsBlockDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml"))
class EmailDomainsBlockDaoIntegrationTestConfiguration:
  @Bean
  def emailDomainsBlockDao(springDB: SpringDB) = new EmailDomainsBlockDao(springDB)
end EmailDomainsBlockDaoIntegrationTestConfiguration
