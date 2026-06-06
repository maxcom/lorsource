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

import org.junit.Assert.*
import org.junit.{Before, Test}
import org.junit.runner.RunWith
import org.mockito.Mockito.{mock, when}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.user.User
import scalikejdbc.*

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(classes = Array(classOf[PollDaoIntegrationTestConfiguration])) @Transactional
class PollDaoIntegrationTest:
  @Autowired
  var pollDao: PollDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  private var pollId: Int = scala.compiletime.uninitialized

  @Before
  def setUp(): Unit =
    pollId = springDB.run:
      sql"SELECT max(polls.id) FROM polls,topics WHERE topics.id=polls.topic AND topics.moderate AND NOT topics.deleted"
        .map(rs => rs.int(1))
        .single
        .apply()
        .get

  @Test
  def testGetMostRecentPoll(): Unit =
    val poll = pollDao.getMostRecentPoll()
    assertNotNull("Should return a poll", poll)
    assertTrue("Poll should have variants", poll.variants.nonEmpty)

  @Test
  def testGetPoll(): Unit =
    val poll = pollDao.getPoll(pollId)
    assertEquals(pollId, poll.id)
    assertNotNull("Poll should have a topic", poll.topic)
    assertTrue("Poll should have variants", poll.variants.nonEmpty)

  @Test(expected = classOf[PollNotFoundException])
  def testGetPollNotFound(): Unit = pollDao.getPoll(999999999)

  @Test
  def testGetPollByTopicId(): Unit =
    val poll = pollDao.getPoll(pollId)
    val pollByTopic = pollDao.getPollByTopicId(poll.topic)
    assertEquals(poll.id, pollByTopic.id)

  @Test(expected = classOf[PollNotFoundException])
  def testGetPollByTopicIdNotFound(): Unit = pollDao.getPollByTopicId(999999999)

  @Test
  def testGetPollResultsOrderId(): Unit =
    val poll = pollDao.getPoll(pollId)
    val results = pollDao.getPollResults(poll)
    assertEquals(poll.variants.size, results.size)
    for r <- results do
      assertNotNull("Each result should have a label", r.label)

  @Test
  def testGetPollResultsOrderVotes(): Unit =
    val poll = pollDao.getPoll(pollId)
    val results = pollDao.getPollResults(poll, Poll.OrderVotes, None)
    assertEquals(poll.variants.size, results.size)

  @Test
  def testGetCountUsers(): Unit =
    val poll = pollDao.getPoll(pollId)
    val count = pollDao.getCountUsers(poll)
    assertTrue("Count should be non-negative", count >= 0)

  @Test
  def testGetVotersCount(): Unit =
    val count = pollDao.getVotersCount(pollId)
    assertTrue("Voters count should be non-negative", count >= 0)

  @Test
  def testCreatePoll(): Unit =
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
    pollDao.createPoll(pollList, true, topicId)
    val poll = pollDao.getPollByTopicId(topicId)
    assertEquals(3, poll.variants.size)
    assertTrue("Poll should be multiselect", poll.multiSelect)

  @Test
  def testUpdatePoll(): Unit =
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
    pollDao.createPoll(pollList, false, topicId)
    val poll = pollDao.getPollByTopicId(topicId)

    val modifiedVariants =
      poll.variants.map(v => PollVariant(v.id, "Modified " + v.label)) :+ PollVariant(0, "New Variant")
    val modified = pollDao.updatePoll(poll, modifiedVariants, true)
    assertTrue("Poll should be modified", modified)

    val updatedPoll = pollDao.getPoll(poll.id)
    assertTrue("Multiselect should be changed", updatedPoll.multiSelect)

  @Test
  def testUpdateVotesIncrementsCounts(): Unit =
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
    pollDao.createPoll(Seq("Option A", "Option B"), false, topicId)
    val poll = pollDao.getPollByTopicId(topicId)
    val variantA = poll.variants.find(_.label == "Option A").get

    val votesBefore = springDB.run:
      sql"SELECT sum(votes) FROM polls_variants WHERE vote = ${poll.id}"
        .map(rs => rs.int(1))
        .single
        .apply()
        .getOrElse(0)

    pollDao.updateVotes(poll.id, Array(variantA.id), mockUser(1))

    val votesAfter = springDB.run:
      sql"SELECT sum(votes) FROM polls_variants WHERE vote = ${poll.id}"
        .map(rs => rs.int(1))
        .single
        .apply()
        .getOrElse(0)

    assertEquals("Vote count should increase by 1 after voting once", votesBefore + 1, votesAfter)

  @Test
  def testUpdateVotesIdempotentOnConflict(): Unit =
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
    pollDao.createPoll(Seq("Option X", "Option Y"), false, topicId)
    val poll = pollDao.getPollByTopicId(topicId)
    val variantX = poll.variants.find(_.label == "Option X").get

    val votesBefore = springDB.run:
      sql"SELECT votes FROM polls_variants WHERE id = ${variantX.id}".map(rs => rs.int("votes")).single.apply().get

    pollDao.updateVotes(poll.id, Array(variantX.id), mockUser(2))

    val votesAfterFirst = springDB.run:
      sql"SELECT votes FROM polls_variants WHERE id = ${variantX.id}".map(rs => rs.int("votes")).single.apply().get

    assertEquals("Votes should increase by 1 after first vote", votesBefore + 1, votesAfterFirst)

    pollDao.updateVotes(poll.id, Array(variantX.id), mockUser(2))

    val votesAfterSecond = springDB.run:
      sql"SELECT votes FROM polls_variants WHERE id = ${variantX.id}".map(rs => rs.int("votes")).single.apply().get

    assertEquals("Votes should not increase on duplicate vote (idempotent)", votesAfterFirst, votesAfterSecond)

  @Test
  def testUpdateVotesInvalidVariantThrowsBadVote(): Unit =
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
    pollDao.createPoll(Seq("Option M", "Option N"), false, topicId)
    val poll = pollDao.getPollByTopicId(topicId)

    val invalidVariantId = springDB.run:
      sql"SELECT max(id) + 1 FROM polls_variants".map(rs => rs.int(1)).single.apply().get

    try
      pollDao.updateVotes(poll.id, Array(invalidVariantId), mockUser(3))
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
