/*
 * Copyright 1998-2024 Linux.org.ru
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

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import ru.org.linux.test.WebHelper
import sttp.client3.{UriContext, basicRequest}
import sttp.model.StatusCode

@RunWith(classOf[JUnitRunner])
class ArchiveControllerWebTest extends Specification {
  "archive controller" should {
    "open without slash" in {
      val response = basicRequest
        .get(uri"${WebHelper.MainUrl}news/archive")
        .send(WebHelper.backend)

      response.code must be equalTo StatusCode.Ok
    }

    "open with slash" in {
      val response = basicRequest
        .get(uri"${WebHelper.MainUrl}news/archive/")
        .send(WebHelper.backend)

      response.code must be equalTo StatusCode.Ok
    }
  }
}
