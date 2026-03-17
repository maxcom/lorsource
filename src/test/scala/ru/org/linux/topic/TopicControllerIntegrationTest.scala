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

import org.junit.{Before, Test}
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.context.WebApplicationContext
import ru.org.linux.PekkoConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.{redirectedUrl, status}
import org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup

@RunWith(classOf[SpringJUnit4ClassRunner])
@WebAppConfiguration
@ContextConfiguration(
  classes = Array(
    classOf[TopicIntegrationTestConfiguration],
    classOf[PekkoConfiguration]
  )
)
class TopicControllerIntegrationTest {
  @Autowired
  var wac: WebApplicationContext = scala.compiletime.uninitialized

  private var mockMvc: MockMvc = scala.compiletime.uninitialized

  @Before
  def setup(): Unit = {
    mockMvc = webAppContextSetup(wac).build()
  }

  @Test
  def testJumpToComment(): Unit = {
    mockMvc
      .perform(get("/forum/talks/1920001?cid=1920019"))
      .andExpect(status.isFound)
      .andExpect(redirectedUrl("/forum/talks/1920001#comment-1920019"))
  }

  @Test
  def testLoadBase(): Unit = {
    mockMvc
      .perform(get("/forum/talks/1920001"))
      .andExpect(status.isOk)
  }

  @Test
  def testLoadBaseZeroComments(): Unit = {
    mockMvc
      .perform(get("/polls/polls/98075"))
      .andExpect(status.isOk)
  }

  @Test
  def testWrongPage(): Unit = {
    mockMvc
      .perform(get("/forum/talks/1920001/page10"))
      .andExpect(status.isFound)
      .andExpect(redirectedUrl("/forum/talks/1920001"))
  }

  @Test
  def testZeroCommentsWrongPage(): Unit = {
    mockMvc
      .perform(get("/polls/polls/98075/page10"))
      .andExpect(status.isFound)
      .andExpect(redirectedUrl("/polls/polls/98075"))
  }
}
