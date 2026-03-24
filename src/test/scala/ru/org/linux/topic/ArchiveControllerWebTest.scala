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
import sttp.client3.{UriContext, basicRequest}
import sttp.model.StatusCode

class ArchiveControllerWebTest extends FunSuite with WebHelper:
  test("archive controller opens without slash"):
    val response = basicRequest
      .get(uri"${MainUrl}news/archive")
      .send(backend)
    assertEquals(response.code, StatusCode.Ok, "status code")

  test("archive controller opens with slash"):
    val response = basicRequest
      .get(uri"${MainUrl}news/archive/")
      .send(backend)
    assertEquals(response.code, StatusCode.Ok, "status code")
