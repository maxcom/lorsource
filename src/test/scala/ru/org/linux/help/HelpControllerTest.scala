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

package ru.org.linux.help

import munit.FunSuite
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders.*
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import ru.org.linux.test.SpringTestSupport
import ru.org.linux.util.markdown.MarkdownFormatter

@WebAppConfiguration @ContextConfiguration(classes = Array(classOf[HelpControllerTestConfig]))
class HelpControllerTest extends FunSuite with SpringTestSupport:
  @Autowired
  var wac: WebApplicationContext = scala.compiletime.uninitialized

  var mockMvc: MockMvc = scala.compiletime.uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    this.mockMvc = webAppContextSetup(this.wac).build()

  test("ok"):
    mockMvc.perform(get("/help/lorcode.md")).andExpect(status.is(200))

  test("404"):
    mockMvc.perform(get("/help/wrong.md")).andExpect(status.is(404))

@Configuration @EnableWebMvc
class HelpControllerTestConfig:
  @Bean
  def controller =
    val markdown: MarkdownFormatter = mock(classOf[MarkdownFormatter])

    when(markdown.renderToHtml(anyString(), anyBoolean())).thenReturn("ok")

    new HelpController(markdown)
