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
package ru.org.linux.section;

import org.junit.Test;
import ru.org.linux.section.stub.TestSectionDaoImpl;

import static org.junit.Assert.*;

public class SectionServiceTest {
  private final SectionService sectionService = new SectionService(new TestSectionDaoImpl());

  @Test
  public void getSectionTest() {
    Section section = sectionService.getSection(3);
    assertNotNull(section);
    assertEquals("Section 3", section.getName());
    assertTrue(section.isPremoderated());
    assertFalse(section.isPollPostAllowed());
    assertTrue(section.isImagepost());

    section = sectionService.getSection(2);
    assertNotNull(section);
    assertEquals("Section 2", section.getName());
    assertTrue(section.isPremoderated());
    assertFalse(section.isPollPostAllowed());
    assertFalse(section.isImagepost());
  }

  @Test(expected = SectionNotFoundException.class)
  public void testBadSection() {
    sectionService.getSection(-1);
  }

  @Test
  public void getSectionListTest() {
    var sectionList = sectionService.sections();
    assertEquals(4, sectionList.size());
  }

  @Test
  public void getScrollModeTest() {
    Section section = sectionService.getSection(1);
    assertEquals(SectionScrollModeEnum.SECTION, section.getScrollMode());

    section = sectionService.getSection(2);
    assertEquals(SectionScrollModeEnum.GROUP, section.getScrollMode());

    section = sectionService.getSection(3);
    assertEquals(SectionScrollModeEnum.SECTION, section.getScrollMode());

    section = sectionService.getSection(5);
    assertEquals(SectionScrollModeEnum.SECTION, section.getScrollMode());
  }
}
