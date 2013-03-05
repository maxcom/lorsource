/*
 * Copyright 1998-2012 Linux.org.ru
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
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("unit-tests-context.xml")
public class SectionServiceTest {
  @Autowired
  private SectionService sectionService;

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

    try {
      section = sectionService.getSection(-1);
      fail();
    } catch (SectionNotFoundException ignored) {
    }
  }

  @Test
  public void getSectionListTest() {
    List<Section> sectionList = sectionService.getSectionList();
    assertEquals(5, sectionList.size());
  }

  @Test
  public void getAddInfoTest() {
    String additionalInfo = sectionService.getAddInfo(1);
    assertEquals("Extended info for Section 1", additionalInfo);

    additionalInfo = sectionService.getAddInfo(3);
    assertEquals("Extended info for Section 3", additionalInfo);
  }

  @Test
  public void getScrollModeTest() {

    Section section = sectionService.getSection(1);
    assertEquals(SectionScrollModeEnum.SECTION, section.getScrollMode());

    section = sectionService.getSection(2);
    assertEquals(SectionScrollModeEnum.GROUP, section.getScrollMode());

    section = sectionService.getSection(3);
    assertEquals(SectionScrollModeEnum.SECTION, section.getScrollMode());

    section = sectionService.getSection(4);
    assertEquals(SectionScrollModeEnum.NO_SCROLL, section.getScrollMode());

    section = sectionService.getSection(5);
    assertEquals(SectionScrollModeEnum.SECTION, section.getScrollMode());
  }
}
