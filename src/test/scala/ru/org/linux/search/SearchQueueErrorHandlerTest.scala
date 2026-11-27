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
package ru.org.linux.search

import munit.FunSuite
import ru.org.linux.search.SearchQueueErrorHandler.{LogDecision, Suppressor}

import java.net.ConnectException

class SearchQueueErrorHandlerTest extends FunSuite:
  private val Interval = 5L * 60 * 1000

  test("first error of a signature is logged with full trace"):
    val suppressor = Suppressor(Interval, () => 0L)

    assertEquals(suppressor.decide("sig"), LogDecision.LogFull(0))

  test("repeats of the same signature are suppressed"):
    var now = 0L
    val suppressor = Suppressor(Interval, () => now)
    suppressor.decide("sig")

    now += 1000

    assertEquals(suppressor.decide("sig"), LogDecision.Suppress)

    now += 1000

    assertEquals(suppressor.decide("sig"), LogDecision.Suppress)

  test("after the interval the error is logged again with the suppressed repeat count"):
    var now = 0L
    val suppressor = Suppressor(Interval, () => now)
    suppressor.decide("sig")

    now += 1000
    for _ <- 1 to 3 do
      suppressor.decide("sig")

    now += Interval

    assertEquals(suppressor.decide("sig"), LogDecision.LogFull(3))

  test("during a continuing failure the full trace repeats once per interval"):
    var now = 0L
    val suppressor = Suppressor(Interval, () => now)
    suppressor.decide("sig")

    now = Interval - 1000

    assertEquals(suppressor.decide("sig"), LogDecision.Suppress)

    now = Interval

    assertEquals(suppressor.decide("sig"), LogDecision.LogFull(1))

  test("a different signature is logged immediately"):
    var now = 0L
    val suppressor = Suppressor(Interval, () => now)
    suppressor.decide("sig1")

    now += 1

    assertEquals(suppressor.decide("sig2"), LogDecision.LogFull(0))

  test("signature combines exception class with its most specific cause"):
    val sig = SearchQueueErrorHandler.signature(RuntimeException("wrapped", ConnectException("Connection refused")))

    assert(sig.contains("RuntimeException"))
    assert(sig.contains("ConnectException"))
