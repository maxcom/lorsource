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
import ru.org.linux.test.WebHelper
import sttp.client4.*
import sttp.model.StatusCode

object WhoisDeletedLinksWebTest:
  private val TestGroup = 4068
  private val CleanUser = "edo"

class WhoisDeletedLinksWebTest extends FunSuite with WebHelper:
  import WhoisDeletedLinksWebTest.*

  private def getProfile(auth: String, nick: String): String =
    val response = basicRequest.cookie(AuthCookie, auth).get(MainUrl.addPath("people", nick, "profile")).send(backend)

    assertEquals(response.code, StatusCode.Ok, "status code")
    response.body.merge

  authorized().test("deleted topics link appears on own profile only after deletion"): auth =>
    val topicId = createTopic(auth, TestGroup, "Whois deleted links web test").fold(v => fail(v), identity)

    deleteTopic(auth, topicId)

    val body = getProfile(auth, TestUser)

    assert(body.contains(s"/people/$TestUser/deleted-topics"), "deleted topics link should be shown")

  authorized().test("deleted topics link is not shown without deleted topics"): auth =>
    val body = getProfile(auth, CleanUser)

    assert(!body.contains("deleted-topics"), "deleted topics link should not be shown")

  authorized("maxcom").test("deleted comments link depends on deleted comments presence"): auth =>
    val cleanBody = getProfile(auth, CleanUser)

    assert(!cleanBody.contains("deleted-comments"), "deleted comments link should not be shown")
    assert(!cleanBody.contains("deleted-topics"), "deleted topics link should not be shown")

    val body = getProfile(auth, TestUser)

    assert(body.contains(s"/people/$TestUser/deleted-comments"), "deleted comments link should be shown")
