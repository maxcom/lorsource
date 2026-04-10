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
import ru.org.linux.test.WebHelper
import sttp.client4.*
import sttp.model.StatusCode

class TopicListControllerWebTest extends FunSuite with WebHelper:
  test("TopicListController loads archive with 200 code"):
    val response = basicRequest
      .get(MainUrl.addPath("news", "archive", "2007", "5"))
      .send(backend)
    assertEquals(response.code, StatusCode.Ok, "status code")

  test("polls section loads with 200 code"):
    val response = basicRequest
      .get(uri"${MainUrl}polls/")
      .send(backend)
    assertEquals(response.code, StatusCode.Ok, "status code")
