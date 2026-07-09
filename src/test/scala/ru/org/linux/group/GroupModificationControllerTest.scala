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
package ru.org.linux.group

import ru.org.linux.group.GroupModificationController.validateUrlName

class GroupModificationControllerTest extends munit.FunSuite:
  test("valid ASCII urlName accepted") {
    assertEquals(validateUrlName("general"), None)
  }

  test("valid urlName with hyphen, underscore and digits accepted") {
    assertEquals(validateUrlName("general-talk_2"), None)
  }

  test("valid single-char urlName accepted") {
    assertEquals(validateUrlName("a"), None)
  }

  test("empty urlName rejected") {
    assert(validateUrlName("").isDefined)
  }

  test("null urlName rejected") {
    assert(validateUrlName(null).isDefined)
  }

  test("slash in urlName rejected") {
    val err = validateUrlName("a/b")
    assert(err.isDefined)
    assert(err.get.contains("/"))
  }

  test("cyrillic urlName rejected") {
    assert(validateUrlName("раздел").isDefined)
  }

  test("unicode latin-extended urlName rejected") {
    assert(validateUrlName("tëst").isDefined)
  }

  test("space in urlName rejected") {
    assert(validateUrlName("a b").isDefined)
  }

  test("dot in urlName rejected") {
    assert(validateUrlName("a.b").isDefined)
  }

  test("plus in urlName rejected") {
    assert(validateUrlName("a+b").isDefined)
  }
