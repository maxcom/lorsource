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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;


@ContextConfiguration("unit-tests-context.xml")
public class SectionServiceTest extends AbstractTestNGSpringContextTests {
  @Autowired
  private SectionService sectionService;

  @Test
  public void getSectionIdByNameTest() {

    Assert.assertEquals(1, sectionService.getSectionIdByName("Section 1"));
    Assert.assertEquals(2, sectionService.getSectionIdByName("Section 2"));
    Assert.assertEquals(3, sectionService.getSectionIdByName("Section 3"));
    Assert.assertEquals(4, sectionService.getSectionIdByName("Section 4"));

    try {
      sectionService.getSectionIdByName("Section XXX");
      Assert.fail();
    } catch (SectionNotFoundException ignored) {
    }
  }
  @Test
  public void getSectionTest() {
    Section section = sectionService.getSection(3);
    Assert.assertNotNull(section);
    Assert.assertEquals("Section 3", section.getName());
    Assert.assertTrue(section.isPremoderated());
    Assert.assertFalse(section.isPollPostAllowed());
    Assert.assertTrue(section.isImagepost());

    section = sectionService.getSection(2);
    Assert.assertNotNull(section);
    Assert.assertEquals("Section 2", section.getName());
    Assert.assertTrue(section.isPremoderated());
    Assert.assertFalse(section.isPollPostAllowed());
    Assert.assertFalse(section.isImagepost());

    try {
      section = sectionService.getSection(-1);
      Assert.fail();
    } catch (SectionNotFoundException ignored) {
    }
  }

  @Test
  public void getSectionListTest() {
    List<Section> sectionList = sectionService.getSectionList();
    Assert.assertEquals(5, sectionList.size());
  }

  @Test
  public void getAddInfoTest() {
    String additionalInfo = sectionService.getAddInfo(1);
    Assert.assertEquals("Extended info for Section 1", additionalInfo);

    additionalInfo = sectionService.getAddInfo(3);
    Assert.assertEquals("Extended info for Section 3", additionalInfo);
  }

  @Test
  public void getScrollModeTest() {

    Section section = sectionService.getSection(1);
    Assert.assertEquals(SectionScrollModeEnum.SECTION, section.getScrollMode());

    section = sectionService.getSection(2);
    Assert.assertEquals(SectionScrollModeEnum.GROUP, section.getScrollMode());

    section = sectionService.getSection(3);
    Assert.assertEquals(SectionScrollModeEnum.SECTION, section.getScrollMode());

    section = sectionService.getSection(4);
    Assert.assertEquals(SectionScrollModeEnum.NO_SCROLL, section.getScrollMode());

    section = sectionService.getSection(5);
    Assert.assertEquals(SectionScrollModeEnum.SECTION, section.getScrollMode());
  }
}
