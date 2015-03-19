/*
 * Copyright 1998-2015 Linux.org.ru
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

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.org.linux.tag.TagName._

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class TagNameTest extends FunSuite {
  test("test sanitize one") {
    assert(List("linux") === parseAndSanitizeTags("linux").toList)
  }

  test("test sanitize list") {
    assert(List("fedora", "ubuntu", "opensuse") === parseAndSanitizeTags("fedora, ubuntu, opensuse").toList)
  }

  test("ignore extra spaces and commas") {
    val (goodTags, badTags) = parseTags("fedora,, ,   ,  ,").partition(isGoodTag)

    assert (badTags === Set())
    assert (goodTags === Set("fedora"))
  }

  test("html tag is invalid") {
    assert(isGoodTag("<b>") === false)
  }

  test("leading space in tag is invalid") {
    assert(isGoodTag(" test") === false)
  }

  test("trailing space in tag is invalid") {
    assert(isGoodTag("test ") === false)
  }

  test("linux is valid tag") {
    assert(isGoodTag("linux") === true)
  }

  test("linux in mixed case is valid tag") {
    assert(isGoodTag("lInux") === true)
  }

  test("dot in end is not valid") {
    assert(isGoodTag("linux.") === false)
  }

  test("c++ is valid tag") {
    assert(isGoodTag("c++") === true)
  }

  test("-20 is valid tag") {
    assert(isGoodTag("-20") === true)
  }

  test("c is valid tag") {
    assert(isGoodTag("c") === true)
  }

  test("tag can't contain comma") {
    assert(isGoodTag("test,test") === false)
  }

  test("функциональное программирование is valid") {
    assert(isGoodTag("функциональное программирование"))
  }
}

