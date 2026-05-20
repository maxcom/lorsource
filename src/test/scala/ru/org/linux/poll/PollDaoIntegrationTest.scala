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

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.{ContextConfiguration, ContextHierarchy}
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional

object PollDaoIntegrationTest {
  private val TestTopicId = 1937504
}

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextHierarchy(Array(
  new ContextConfiguration(value = Array("classpath:database.xml")),
  new ContextConfiguration(classes = Array(classOf[PollDaoIntegrationTestConfiguration]))
))
class PollDaoIntegrationTest {
  @Autowired
  private var pollDao: PollDao = scala.compiletime.uninitialized

  @Test
  def voteGetCurrentPollTest(): Unit = {
    val currentPollId = pollDao.getMostRecentPollId
    val poll = pollDao.getMostRecentPoll()
    assertEquals(currentPollId, poll.id)
  }

  @Test
  @Transactional
  def voteCreateAndRunningPollTest(): Unit = {
    val pollList = Seq("Case 1", "Case 2", "Case 3")

    pollDao.createPoll(pollList, true, PollDaoIntegrationTest.TestTopicId)
    val poll = pollDao.getPollByTopicId(PollDaoIntegrationTest.TestTopicId)

    /* Проверяем правильность сохранения вариантов голосования */
    var pollVariants = pollDao.getPollResults(poll)
    assertEquals(3, pollVariants.size)

    /* Проверяем изменения по вариантам голосования */
    pollDao.addNewVariant(poll, "Case 4")
    pollVariants = pollDao.getPollResults(poll)
    assertEquals(4, pollVariants.size)

    val next = pollVariants.head
    pollDao.removeVariant(PollVariant(next.id, next.label))
    pollVariants = pollDao.getPollResults(poll)
    assertEquals(3, pollVariants.size)
  }
}