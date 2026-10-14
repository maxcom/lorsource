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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.{get, post}
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.{redirectedUrl, status}
import org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup
import org.springframework.web.context.WebApplicationContext
import ru.org.linux.PekkoConfiguration
import ru.org.linux.auth.{IpBlockInfo, UserDetailsImpl}
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.test.TransactionalTestSupport
import ru.org.linux.topic.TopicDao
import ru.org.linux.topic.TopicIntegrationTestConfiguration
import ru.org.linux.user.{Profile, User, UserService}
import scalikejdbc.*

@WebAppConfiguration
@ContextConfiguration(classes = Array(
  classOf[TopicIntegrationTestConfiguration],
  classOf[PekkoConfiguration],
  classOf[VoteControllerIntegrationTestConfiguration]))
class VoteControllerIntegrationTest extends FunSuite with TransactionalTestSupport:
  @Autowired
  var wac: WebApplicationContext = scala.compiletime.uninitialized

  @Autowired
  var userService: UserService = scala.compiletime.uninitialized

  @Autowired
  var pollDao: PollDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  private var mockMvc: MockMvc = scala.compiletime.uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)

    val defaultReq = get("/")
    defaultReq.requestAttr("currentUser", userService.getAnonymous)
    defaultReq.requestAttr("ipBlockInfo", IpBlockInfo("127.0.0.1"))

    val builder = webAppContextSetup(wac)
    builder.defaultRequest(defaultReq)
    mockMvc = builder.build()

  override def afterEach(context: AfterEach): Unit =
    SecurityContextHolder.clearContext()
    super.afterEach(context)

  private def loginAs(user: User): Unit =
    val authorities = java.util.List.of[GrantedAuthority](new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
    val details = new UserDetailsImpl(user, authorities, Profile.DEFAULT)

    SecurityContextHolder
      .getContext
      .setAuthentication(new UsernamePasswordAuthenticationToken(details, null, authorities))

  private def createPollTopic(title: String): (Int, Int, Int) =
    val topicId = springDB.run:
      sql"""INSERT INTO topics (groupid, userid, title, url, moderate, postdate, id, linktext, deleted, ua_id, postip, draft, lastmod, allow_anonymous)
            VALUES (19387, 1, $title, '', 't', CURRENT_TIMESTAMP, nextval('s_msgid'), '', 'f',
                    create_user_agent('Integration test User Agent'), '127.0.0.1'::inet, 'f', CURRENT_TIMESTAMP, 'f')"""
        .update
        .apply()
      sql"SELECT max(id) FROM topics WHERE groupid = 19387 AND title = $title".map(rs => rs.int(1)).single.apply().get

    springDB.localTx {
      pollDao.createPoll(Seq("Да", "Нет"), false, topicId)
    }

    val poll = pollDao.getPollByTopicId(topicId)

    (topicId, poll.id, poll.variants.head.id)

  test("voteOnActivePollRedirectsToTopic"):
    loginAs(userService.getUser("edo"))
    val (topicId, pollId, variantId) = createPollTopic("VoteController active poll test")

    mockMvc
      .perform(post("/vote.jsp").param("voteid", pollId.toString).param("vote", variantId.toString))
      .andExpect(status.isFound)
      .andExpect(redirectedUrl(s"/polls/polls/$topicId"))

  test("voteOnDeletedTopicFails"):
    loginAs(userService.getUser("edo"))
    val (topicId, pollId, variantId) = createPollTopic("VoteController deleted poll test")

    springDB.run:
      sql"UPDATE topics SET deleted='t' WHERE id = $topicId".update.apply()

    try
      mockMvc.perform(post("/vote.jsp").param("voteid", pollId.toString).param("vote", variantId.toString))
      fail("Голосование в удалённой теме должно быть отклонено")
    catch
      case e: Exception =>
        var cause: Throwable = e
        while cause.getCause != null do
          cause = cause.getCause

        assert(cause.isInstanceOf[BadVoteException], s"expected BadVoteException, got $cause")
        assertEquals(cause.getMessage, "Опрос удален")

end VoteControllerIntegrationTest

@Configuration
class VoteControllerIntegrationTestConfiguration:
  @Bean
  def voteController(pollDao: PollDao, topicDao: TopicDao, springDB: SpringDB): VoteController =
    VoteController(pollDao, topicDao, springDB)
