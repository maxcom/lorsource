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

package ru.org.linux.topic

import munit.FunSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.context.WebApplicationContext
import ru.org.linux.PekkoConfiguration
import ru.org.linux.user.UserService
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.{redirectedUrl, status}
import org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup
import ru.org.linux.auth.IpBlockInfo
import ru.org.linux.test.SpringTestSupport

@WebAppConfiguration
@ContextConfiguration(classes = Array(classOf[TopicIntegrationTestConfiguration], classOf[PekkoConfiguration]))
class TopicControllerIntegrationTest extends FunSuite with SpringTestSupport:
  @Autowired
  var wac: WebApplicationContext = scala.compiletime.uninitialized

  @Autowired
  var userService: UserService = scala.compiletime.uninitialized

  private var mockMvc: MockMvc = scala.compiletime.uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    val defaultReq = get("/")
    defaultReq.requestAttr("currentUser", userService.getAnonymous)
    defaultReq.requestAttr("ipBlockInfo", IpBlockInfo("127.0.0.1"))

    val builder = webAppContextSetup(wac)
    builder.defaultRequest(defaultReq)
    mockMvc = builder.build()

  test("jumpToComment"):
    mockMvc
      .perform(get("/forum/talks/1920001?cid=1920019"))
      .andExpect(status.isFound)
      .andExpect(redirectedUrl("/forum/talks/1920001#comment-1920019"))

  test("loadBase"):
    mockMvc.perform(get("/forum/talks/1920001")).andExpect(status.isOk)

  test("loadBaseZeroComments"):
    mockMvc.perform(get("/polls/polls/98075")).andExpect(status.isOk)

  test("wrongPage"):
    mockMvc
      .perform(get("/forum/talks/1920001/page10"))
      .andExpect(status.isFound)
      .andExpect(redirectedUrl("/forum/talks/1920001"))

  test("zeroCommentsWrongPage"):
    mockMvc
      .perform(get("/polls/polls/98075/page10"))
      .andExpect(status.isFound)
      .andExpect(redirectedUrl("/polls/polls/98075"))

end TopicControllerIntegrationTest
