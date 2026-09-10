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

package ru.org.linux.poll

import munit.FunSuite
import org.mockito.Mockito.{mock, when}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.test.TransactionalTestSupport
import ru.org.linux.user.User
import scalikejdbc.*

@ContextConfiguration(classes = Array(classOf[PollDaoIntegrationTestConfiguration]))
class PollDaoIntegrationTest extends FunSuite with TransactionalTestSupport:
  @Autowired
  var pollDao: PollDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  private var pollId: Int = scala.compiletime.uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    pollId = springDB.run:
      sql"SELECT max(polls.id) FROM polls,topics WHERE topics.id=polls.topic AND topics.moderate AND NOT topics.deleted"
        .map(rs => rs.int(1))
        .single
        .apply()
        .get

  test("getMostRecentPoll"):
    val poll = pollDao.getMostRecentPoll()
    assert(poll != null, "Should return a poll")
    assert(poll.variants.nonEmpty, "Poll should have variants")

  test("getPoll"):
    val poll = pollDao.getPoll(pollId)
    assertEquals(poll.id, pollId)
    assert(poll.topic != 0, "Poll should have a topic")
    assert(poll.variants.nonEmpty, "Poll should have variants")

  test("getPollNotFound"):
    intercept[PollNotFoundException] {
      pollDao.getPoll(999999999)
    }

  test("getPollByTopicId"):
    val poll = pollDao.getPoll(pollId)
    val pollByTopic = pollDao.getPollByTopicId(poll.topic)
    assertEquals(pollByTopic.id, poll.id)

  test("getPollByTopicIdNotFound"):
    intercept[PollNotFoundException] {
      pollDao.getPollByTopicId(999999999)
    }

  test("getPollResultsOrderId"):
    val poll = pollDao.getPoll(pollId)
    val results = pollDao.getPollResults(poll)
    assertEquals(results.size, poll.variants.size)
    for r <- results do
      assert(r.label != null, "Each result should have a label")

  test("getPollResultsOrderVotes"):
    val poll = pollDao.getPoll(pollId)
    val results = pollDao.getPollResults(poll, Poll.OrderVotes, None)
    assertEquals(results.size, poll.variants.size)

  test("getCountUsers"):
    val poll = pollDao.getPoll(pollId)
    val count = pollDao.getCountUsers(poll)
    assert(count >= 0, "Count should be non-negative")

  test("getVotersCount"):
    val count = pollDao.getVotersCount(pollId)
    assert(count >= 0, "Voters count should be non-negative")

  test("createPoll"):
    val topicId = springDB.run:
      sql"""INSERT INTO topics (groupid, userid, title, url, moderate, postdate, id, linktext, deleted, ua_id, postip, draft, lastmod, allow_anonymous)
            VALUES (19387, 1, 'Test poll topic', '', 't', CURRENT_TIMESTAMP, nextval('s_msgid'), '', 'f',
                    create_user_agent('Integration test User Agent'), '127.0.0.1'::inet, 'f', CURRENT_TIMESTAMP, 'f')"""
        .update
        .apply()
      sql"SELECT max(id) FROM topics WHERE groupid = 19387 AND title = 'Test poll topic'"
        .map(rs => rs.int(1))
        .single
        .apply()
        .get
    val pollList = Seq("Test Case 1", "Test Case 2", "Test Case 3")
    springDB.localTx {
      pollDao.createPoll(pollList, true, topicId)
    }
    val poll = pollDao.getPollByTopicId(topicId)
    assertEquals(poll.variants.size, 3)
    assert(poll.multiSelect, "Poll should be multiselect")

  test("updatePoll"):
    val topicId = springDB.run:
      sql"""INSERT INTO topics (groupid, userid, title, url, moderate, postdate, id, linktext, deleted, ua_id, postip, draft, lastmod, allow_anonymous)
            VALUES (19387, 1, 'Test update poll topic', '', 't', CURRENT_TIMESTAMP, nextval('s_msgid'), '', 'f',
                    create_user_agent('Integration test User Agent'), '127.0.0.1'::inet, 'f', CURRENT_TIMESTAMP, 'f')"""
        .update
        .apply()
      sql"SELECT max(id) FROM topics WHERE groupid = 19387 AND title = 'Test update poll topic'"
        .map(rs => rs.int(1))
        .single
        .apply()
        .get
    val pollList = Seq("Alpha", "Beta")
    springDB.localTx {
      pollDao.createPoll(pollList, false, topicId)
    }
    val poll = pollDao.getPollByTopicId(topicId)

    val modifiedVariants =
      poll.variants.map(v => PollVariant(v.id, "Modified " + v.label)) :+ PollVariant(0, "New Variant")
    val modified = springDB.localTx {
      pollDao.updatePoll(poll, modifiedVariants, true)
    }
    assert(modified, "Poll should be modified")

    val updatedPoll = pollDao.getPoll(poll.id)
    assert(updatedPoll.multiSelect, "Multiselect should be changed")

  test("updateVotesIncrementsCounts"):
    val topicId = springDB.run:
      sql"""INSERT INTO topics (groupid, userid, title, url, moderate, postdate, id, linktext, deleted, ua_id, postip, draft, lastmod, allow_anonymous)
            VALUES (19387, 1, 'Test vote count topic', '', 't', CURRENT_TIMESTAMP, nextval('s_msgid'), '', 'f',
                    create_user_agent('Integration test User Agent'), '127.0.0.1'::inet, 'f', CURRENT_TIMESTAMP, 'f')"""
        .update
        .apply()
      sql"SELECT max(id) FROM topics WHERE groupid = 19387 AND title = 'Test vote count topic'"
        .map(rs => rs.int(1))
        .single
        .apply()
        .get
    springDB.localTx {
      pollDao.createPoll(Seq("Option A", "Option B"), false, topicId)
    }
    val poll = pollDao.getPollByTopicId(topicId)
    val variantA = poll.variants.find(_.label == "Option A").get

    val votesBefore = springDB.run:
      sql"SELECT sum(votes) FROM polls_variants WHERE vote = ${poll.id}"
        .map(rs => rs.int(1))
        .single
        .apply()
        .getOrElse(0)

    springDB.localTx {
      pollDao.updateVotes(poll.id, Array(variantA.id), mockUser(1))
    }

    val votesAfter = springDB.run:
      sql"SELECT sum(votes) FROM polls_variants WHERE vote = ${poll.id}"
        .map(rs => rs.int(1))
        .single
        .apply()
        .getOrElse(0)

    assertEquals(votesAfter, votesBefore + 1, "Vote count should increase by 1 after voting once")

  test("updateVotesIdempotentOnConflict"):
    val topicId = springDB.run:
      sql"""INSERT INTO topics (groupid, userid, title, url, moderate, postdate, id, linktext, deleted, ua_id, postip, draft, lastmod, allow_anonymous)
            VALUES (19387, 1, 'Test idempotent vote topic', '', 't', CURRENT_TIMESTAMP, nextval('s_msgid'), '', 'f',
                    create_user_agent('Integration test User Agent'), '127.0.0.1'::inet, 'f', CURRENT_TIMESTAMP, 'f')"""
        .update
        .apply()
      sql"SELECT max(id) FROM topics WHERE groupid = 19387 AND title = 'Test idempotent vote topic'"
        .map(rs => rs.int(1))
        .single
        .apply()
        .get
    springDB.localTx {
      pollDao.createPoll(Seq("Option X", "Option Y"), false, topicId)
    }
    val poll = pollDao.getPollByTopicId(topicId)
    val variantX = poll.variants.find(_.label == "Option X").get

    val votesBefore = springDB.run:
      sql"SELECT votes FROM polls_variants WHERE id = ${variantX.id}".map(rs => rs.int("votes")).single.apply().get

    springDB.localTx {
      pollDao.updateVotes(poll.id, Array(variantX.id), mockUser(2))
    }

    val votesAfterFirst = springDB.run:
      sql"SELECT votes FROM polls_variants WHERE id = ${variantX.id}".map(rs => rs.int("votes")).single.apply().get

    assertEquals(votesAfterFirst, votesBefore + 1, "Votes should increase by 1 after first vote")

    springDB.localTx {
      pollDao.updateVotes(poll.id, Array(variantX.id), mockUser(2))
    }

    val votesAfterSecond = springDB.run:
      sql"SELECT votes FROM polls_variants WHERE id = ${variantX.id}".map(rs => rs.int("votes")).single.apply().get

    assertEquals(votesAfterSecond, votesAfterFirst, "Votes should not increase on duplicate vote (idempotent)")

  test("updateVotesInvalidVariantThrowsBadVote"):
    val topicId = springDB.run:
      sql"""INSERT INTO topics (groupid, userid, title, url, moderate, postdate, id, linktext, deleted, ua_id, postip, draft, lastmod, allow_anonymous)
            VALUES (19387, 1, 'Test invalid vote topic', '', 't', CURRENT_TIMESTAMP, nextval('s_msgid'), '', 'f',
                    create_user_agent('Integration test User Agent'), '127.0.0.1'::inet, 'f', CURRENT_TIMESTAMP, 'f')"""
        .update
        .apply()
      sql"SELECT max(id) FROM topics WHERE groupid = 19387 AND title = 'Test invalid vote topic'"
        .map(rs => rs.int(1))
        .single
        .apply()
        .get
    springDB.localTx {
      pollDao.createPoll(Seq("Option M", "Option N"), false, topicId)
    }
    val poll = pollDao.getPollByTopicId(topicId)

    val invalidVariantId = springDB.run:
      sql"SELECT max(id) + 1 FROM polls_variants".map(rs => rs.int(1)).single.apply().get

    try
      springDB.localTx {
        pollDao.updateVotes(poll.id, Array(invalidVariantId), mockUser(3))
      }
      fail("Should throw BadVoteException for invalid poll variant")
    catch
      case _: BadVoteException =>

  private def mockUser(id: Int): User =
    val user = mock(classOf[User])
    when(user.id).thenReturn(id)
    user

end PollDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class PollDaoIntegrationTestConfiguration:

  @Bean
  def pollDao(springDB: SpringDB): PollDao = PollDao(springDB)

end PollDaoIntegrationTestConfiguration
