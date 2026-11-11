/*
 * Copyright 1998-2026 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.msgbase

import munit.FunSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.test.TransactionalTestSupport
import scalikejdbc.*

import java.util as ju
import scala.jdk.CollectionConverters.MapHasAsJava

/** Интеграционные тесты методов очистки словаря user_agents и новой версии create_user_agent. */
@ContextConfiguration(classes = Array(classOf[UserAgentDaoIntegrationTestConfiguration]))
class UserAgentDaoIntegrationTest extends FunSuite with TransactionalTestSupport:

  @Autowired
  var userAgentDao: UserAgentDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  private var testUserId: Int = scala.compiletime.uninitialized
  private var testGroupId: Int = scala.compiletime.uninitialized
  private var testTopicId: Int = scala.compiletime.uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    testUserId = springDB.run:
      sql"select min(id) from users".map(rs => rs.int(1)).single.apply().get
    testGroupId = springDB.run:
      sql"select min(id) from groups".map(rs => rs.int(1)).single.apply().get
    testTopicId = springDB.run:
      sql"select min(id) from topics where not deleted".map(rs => rs.int(1)).single.apply().get

  private def insertUserAgent(name: String): Int =
    springDB.run:
      sql"INSERT INTO user_agents (name) VALUES ($name) RETURNING id".map(rs => rs.int("id")).single.apply().get

  private def insertComment(uaId: Int): Unit =
    springDB.run:
      val commentId = sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
      sql"""INSERT INTO comments (id, userid, title, postdate, replyto, deleted, topic, postip, ua_id)
            VALUES ($commentId, $testUserId, 'UserAgentDaoIntegrationTest', CURRENT_TIMESTAMP,
                    null, 'f', $testTopicId, '127.0.0.1'::inet, $uaId)""".update.apply()
      sql"INSERT INTO msgbase (id, message) VALUES ($commentId, 'UserAgentDaoIntegrationTest body')".update.apply()

  private def insertTopic(uaId: Int): Unit =
    springDB.run:
      val topicId = sql"select nextval('s_msgid') as msgid".map(rs => rs.int("msgid")).single.apply().get
      sql"""INSERT INTO topics (groupid, userid, title, url, moderate, postdate, id, linktext, deleted, ua_id, postip, draft, lastmod, allow_anonymous)
            VALUES ($testGroupId, $testUserId, 'UserAgentDaoIntegrationTest', '', 'f', CURRENT_TIMESTAMP,
                    $topicId, '', 'f', $uaId, '127.0.0.1'::inet, 'f', CURRENT_TIMESTAMP, 'f')""".update.apply()

  private def insertUserLogMention(uaId: Int): Unit =
    springDB.run:
      val info: ju.Map[String, String] = Map("user_agent" -> uaId.toString).asJava
      sql"""INSERT INTO user_log (userid, action_userid, action_date, action, info)
            VALUES ($testUserId, $testUserId, CURRENT_TIMESTAMP, 'register'::user_log_action, $info)""".update.apply()

  test("unreferencedUserAgentIsCandidate"):
    val id = insertUserAgent("UserAgentDaoIntegrationTest unreferenced")

    assert(userAgentDao.getUnreferencedUserAgentIds.contains(id))

  test("referencedByCommentIsNotCandidate"):
    val id = insertUserAgent("UserAgentDaoIntegrationTest comment")
    insertComment(id)

    assert(!userAgentDao.getUnreferencedUserAgentIds.contains(id))

  test("referencedByTopicIsNotCandidate"):
    val id = insertUserAgent("UserAgentDaoIntegrationTest topic")
    insertTopic(id)

    assert(!userAgentDao.getUnreferencedUserAgentIds.contains(id))

  test("referencedByUserLogIsNotCandidate"):
    val id = insertUserAgent("UserAgentDaoIntegrationTest user_log")
    insertUserLogMention(id)

    assert(!userAgentDao.getUnreferencedUserAgentIds.contains(id))

  test("purgeDeletesOnlyUnreferenced"):
    val unreferencedId = insertUserAgent("UserAgentDaoIntegrationTest purge unreferenced")
    val referencedId = insertUserAgent("UserAgentDaoIntegrationTest purge referenced")
    insertComment(referencedId)

    val purged = userAgentDao.purgeUnreferencedUserAgents(Seq(unreferencedId, referencedId))

    assertEquals(purged, 1)
    assert(userAgentDao.getUserAgentById(unreferencedId).isEmpty)
    assert(userAgentDao.getUserAgentById(referencedId).isPresent)

  test("purgeEmptyIsNoop"):
    assertEquals(userAgentDao.purgeUnreferencedUserAgents(Seq.empty), 0)

  test("createOrGetIdReturnsExistingIdForSameName"):
    val name = "UserAgentDaoIntegrationTest createOrGetId same"
    val id1 = springDB.localTx {
      userAgentDao.createOrGetId(name)
    }
    val id2 = springDB.localTx {
      userAgentDao.createOrGetId(name)
    }

    assertEquals(id1, id2)
    assert(userAgentDao.getUserAgentById(id1).isPresent)

  test("createOrGetIdCreatesNewUserAgent"):
    val name = "UserAgentDaoIntegrationTest createOrGetId new"
    val id = springDB.localTx {
      userAgentDao.createOrGetId(name)
    }
    val directId = springDB.run:
      sql"SELECT id FROM user_agents WHERE name = $name".map(rs => rs.int("id")).single.apply().get

    assertEquals(id, directId)

  test("createOrGetIdTruncatesLongUserAgent"):
    val longPrefix = "UserAgentDaoIntegrationTest long " + "x" * 600

    val id = springDB.localTx {
      userAgentDao.createOrGetId(longPrefix + "-different-tail")
    }

    val nameOpt = userAgentDao.getUserAgentById(id)
    assert(nameOpt.isPresent, "user agent not found")
    assertEquals(nameOpt.get.length, UserAgentDao.MaxNameLength)

    // одинаковый префикс длиннее лимита — та же запись словаря
    val samePrefixId = springDB.localTx {
      userAgentDao.createOrGetId(longPrefix + "-another-tail")
    }
    assertEquals(samePrefixId, id)

end UserAgentDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class UserAgentDaoIntegrationTestConfiguration:

  @Bean
  def userAgentDao(springDB: SpringDB): UserAgentDao = new UserAgentDao(springDB)

end UserAgentDaoIntegrationTestConfiguration
