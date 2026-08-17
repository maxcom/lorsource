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

package ru.org.linux.util.bbcode

import munit.FunSuite
import org.apache.commons.httpclient.URI
import org.mockito.Mockito.{mock, when}
import ru.org.linux.spring.SiteConfig
import ru.org.linux.user.{User, UserNotFoundException, UserService}
import ru.org.linux.util.formatter.ToHtmlFormatter

import scala.compiletime.uninitialized

class MemberTagTest extends FunSuite:
  private var maxcom: User = uninitialized
  private var JB: User = uninitialized
  private var isden: User = uninitialized
  private var lorCodeService: LorCodeService = uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    val userService = mock(classOf[UserService])
    val splinter = User(
      nick = "splinter", id = 2, canmod = false, candel = false, anonymous = false,
      corrector = false, blocked = false, password = "", score = 0, maxScore = 0,
      photo = null, email = null, fullName = null, unreadEvents = 0, frozenUntil = null,
      activated = true)

    maxcom = User(nick = "maxcom", id = 1, canmod = true, candel = true, anonymous = false,
      corrector = false, blocked = false, password = "", score = 0, maxScore = 0,
      photo = null, email = null, fullName = null, unreadEvents = 0, frozenUntil = null,
      activated = true)
    JB = User(nick = "JB", id = 3, canmod = true, candel = false, anonymous = false,
      corrector = false, blocked = false, password = "", score = 0, maxScore = 0,
      photo = null, email = null, fullName = null, unreadEvents = 0, frozenUntil = null,
      activated = true)
    isden = User(nick = "isden", id = 4, canmod = false, candel = false, anonymous = false,
      corrector = false, blocked = true, password = "", score = 0, maxScore = 0,
      photo = null, email = null, fullName = null, unreadEvents = 0, frozenUntil = null,
      activated = true)

    when(userService.getUserCached("splinter")).thenReturn(splinter)
    when(userService.getUserCached("maxcom")).thenReturn(maxcom)
    when(userService.getUserCached("JB")).thenReturn(JB)
    when(userService.getUserCached("isden")).thenReturn(isden)
    when(userService.getUserCached("hizel")).thenThrow(UserNotFoundException("hizel"))

    val mainUrl = "http://127.0.0.1:8080/"
    val mainURI = new URI(mainUrl, true, "UTF-8")

    val siteConfig = mock(classOf[SiteConfig])
    when(siteConfig.getMainURI).thenReturn(mainURI)
    when(siteConfig.getSecureURI).thenReturn(mainURI)

    val toHtmlFormatter = new ToHtmlFormatter
    toHtmlFormatter.setSiteConfig(siteConfig)

    lorCodeService = new LorCodeService(userService, toHtmlFormatter)

  test("testExtraLines"):
    assertEquals(
      lorCodeService.parseComment("[user]splinter[/user]", false, LorCodeService.Plain),
      lorCodeService.parseComment("\n\n[user]\n\nsplinter\n\n[/user]\n\n", false, LorCodeService.Plain))

  // http://www.linux.org.ru/forum/linux-org-ru/6448266
  test("splinterTest1"):
    assertEquals(
      "<p><a href=\"http://www.fishing.org/\">http://www.fishing.org/</a> <span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><a style=\"text-decoration: none\" href=\"http://127.0.0.1:8080/people/splinter/profile\">splinter</a></span></p>",
      lorCodeService.parseComment("[url=http://www.fishing.org/][user]splinter[/user][/url]", false, LorCodeService.Plain))

  test("userTest"):
    assertEquals(
      "<p> <span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><a style=\"text-decoration: none\" href=\"http://127.0.0.1:8080/people/maxcom/profile\">maxcom</a></span></p>",
      lorCodeService.parseComment("[user]maxcom[/user]", false, LorCodeService.Plain))
    assertEquals(
      "<p> <span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><s><a style=\"text-decoration: none\" href=\"http://127.0.0.1:8080/people/isden/profile\">isden</a></s></span></p>",
      lorCodeService.parseComment("[user]isden[/user]", false, LorCodeService.Plain))
    assertEquals(
      "<p> <s>hizel</s></p>",
      lorCodeService.parseComment("[user]hizel[/user]", false, LorCodeService.Plain))

  test("parserResultTest"):
    val msg = "[user]hizel[/user][user]JB[/user][user]maxcom[/user]"
    val replier = lorCodeService.getMentions(msg)

    assert(replier.contains(maxcom))
    assert(replier.contains(JB))
    assert(!replier.contains(isden))

  test("userTest2"):
    assertEquals(
      "<p> <span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><a style=\"text-decoration: none\" href=\"http://127.0.0.1:8080/people/maxcom/profile\">maxcom</a></span></p>",
      lorCodeService.parseComment("[user]maxcom[/USER]", false, LorCodeService.Plain))
    assertEquals(
      "<p> <span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><s><a style=\"text-decoration: none\" href=\"http://127.0.0.1:8080/people/isden/profile\">isden</a></s></span></p>",
      lorCodeService.parseComment("[USER]isden[/USER]", false, LorCodeService.Plain))
    assertEquals(
      "<p> <s>hizel</s></p>",
      lorCodeService.parseComment("[user]hizel[/USER]", false, LorCodeService.Plain))

  test("parserResultTest2"):
    val msg = "[user]hizel[/user][USER]JB[/user][user]maxcom[/USER]"
    val replier = lorCodeService.getMentions(msg)

    assert(replier.contains(maxcom))
    assert(replier.contains(JB))
    assert(!replier.contains(isden))