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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("integration-tests-context.xml")
public class SectionDaoIntegrationTest {
  @Autowired
  SectionDao sectionDao;

  private static Section getSectionById(List<Section> sectionList, int id) {
    for (Section section: sectionList) {
      if (section.getId() == id) {
        return section;
      }
    }
    return null;
  }
  @Test
  public void sectionsTest() {

    List<Section> sectionList = sectionDao.getAllSections();
    Assert.assertEquals(4, sectionList.size());

    String addInfo = sectionDao.getAddInfo(sectionList.get(0).getId());
    Assert.assertNotNull(addInfo);
  }
  @Test
  public void sectionsScrollModeTest() {

    List<Section> sectionList = sectionDao.getAllSections();

    Section section = getSectionById(sectionList, 1);
    Assert.assertNotNull(section);
    Assert.assertEquals(SectionScrollModeEnum.SECTION, section.getScrollMode());

    section = getSectionById(sectionList, 2);
    Assert.assertNotNull(section);
    Assert.assertEquals(SectionScrollModeEnum.GROUP, section.getScrollMode());

    section = getSectionById(sectionList, 3);
    Assert.assertNotNull(section);
    Assert.assertEquals(SectionScrollModeEnum.SECTION, section.getScrollMode());

    section = getSectionById(sectionList, 5);
    Assert.assertNotNull(section);
    Assert.assertEquals(SectionScrollModeEnum.SECTION, section.getScrollMode());

  }

}
