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

package ru.org.linux.section

import munit.FunSuite
import ru.org.linux.section.stub.TestSectionDaoImpl

class SectionServiceTest extends FunSuite:
  private val sectionService = new SectionService(new TestSectionDaoImpl)

  test("getSection"):
    var section = sectionService.getSection(3)
    assert(section != null)
    assertEquals("Section 3", section.name)
    assert(section.premoderated)
    assert(!section.isPollPostAllowed)
    assert(section.imagepost)

    section = sectionService.getSection(2)
    assert(section != null)
    assertEquals("Section 2", section.name)
    assert(section.premoderated)
    assert(!section.isPollPostAllowed)
    assert(!section.imagepost)

  test("badSection"):
    intercept[SectionNotFoundException]:
      sectionService.getSection(-1)

  test("getSectionList"):
    val sectionList = sectionService.sections
    assertEquals(4, sectionList.size)

  test("getScrollMode"):
    var section = sectionService.getSection(1)
    assertEquals(SectionScrollModeEnum.SECTION, section.scrollMode)

    section = sectionService.getSection(2)
    assertEquals(SectionScrollModeEnum.GROUP, section.scrollMode)

    section = sectionService.getSection(3)
    assertEquals(SectionScrollModeEnum.SECTION, section.scrollMode)

    section = sectionService.getSection(5)
    assertEquals(SectionScrollModeEnum.SECTION, section.scrollMode)
