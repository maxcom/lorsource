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

package ru.org.linux.tag

import munit.FunSuite
import ru.org.linux.tag.TagName.*

class TagNameTest extends FunSuite {
  test("parseAndSanitizeTags parse one simple tag") {
    assertEquals(parseAndSanitizeTags("linux").toSet, Set("linux"))
  }

  test("parseAndSanitizeTags parse list of simple tags") {
    assertEquals(
      parseAndSanitizeTags("fedora, ubuntu, opensuse").toSet,
      Set("fedora", "ubuntu", "opensuse")
    )
  }

  test("parseTags ignore extra spaces and commas") {
    val (goodTags, badTags) = parseTags("fedora,, ,   ,  ,").partition(isGoodTag)
    assert(badTags.isEmpty)
    assertEquals(goodTags.toSet, Set("fedora"))
  }

  test("isGoodTag reject html in tag") {
    assert(!isGoodTag("<b>"))
  }

  test("isGoodTag reject leading space") {
    assert(!isGoodTag(" test"))
  }

  test("isGoodTag reject trailing space in tag") {
    assert(!isGoodTag("test "))
  }

  test("isGoodTag accept 'linux' tag") {
    assert(isGoodTag("linux"))
  }

  test("isGoodTag accept mixes case tag") {
    assert(isGoodTag("lInux"))
  }

  test("isGoodTag reject dot in end of tag") {
    assert(!isGoodTag("linux."))
  }

  test("isGoodTag accept 'c++' tag") {
    assert(isGoodTag("c++"))
  }

  test("isGoodTag accept '-20' tag") {
    assert(isGoodTag("-20"))
  }

  test("isGoodTag accept 'c' tag") {
    assert(isGoodTag("c"))
  }

  test("isGoodTag reject comma") {
    assert(!isGoodTag("test,test"))
  }

  test("isGoodTag accept 'функциональное программирование' tag") {
    assert(isGoodTag("функциональное программирование"))
  }
}