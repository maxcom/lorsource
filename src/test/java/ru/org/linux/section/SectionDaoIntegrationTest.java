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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

@ContextConfiguration("integration-tests-context.xml")
public class SectionDaoIntegrationTest extends AbstractTestNGSpringContextTests {
  @Autowired
  private SectionDao sectionDao;

  private List<Section> sectionList = null;

  @BeforeClass
  public void initSectionList() {
    sectionList = sectionDao.getAllSections();
  }

  @Test
  public void sectionsCount() {

    // given

    // when

    // then
    Assert.assertEquals(4, sectionList.size());
  }

  @Test(dataProvider = "additionInfoShouldBeFilledDataSource")
  public void additionInfoShouldBeFilled(
    int inputSectionId
  ) {
    // given

    // when
    String additionInfo = sectionDao.getAddInfo(inputSectionId);

    // then
    Assert.assertNotNull(additionInfo);

  }
  @DataProvider(name = "additionInfoShouldBeFilledDataSource")
  public Object[][] additionInfoShouldBeFilledDataSource() {
    return new Object[][] {
      new Object[] {1},
      new Object[] {2},
    };
  }


  @Test(dataProvider = "sectionNamesDataSource")
  public void sectionValues(
    int inputSectionId,
    String expectedSessionName,
    SectionScrollModeEnum expectedScrollMode
  ) {

    // given

    // when
    Section section = getSectionById(inputSectionId);

    // then
    Assert.assertEquals(section.getName(), expectedSessionName);
    Assert.assertEquals(section.getScrollMode(), expectedScrollMode);
  }

  @DataProvider(name = "sectionNamesDataSource")
  public Object[][] sectionNamesDataSource() {
    return new Object[][] {
      new Object[] {1, "news",SectionScrollModeEnum.SECTION},
      new Object[] {2, "forum", SectionScrollModeEnum.GROUP},
      new Object[] {3, "gallery", SectionScrollModeEnum.SECTION},
      new Object[] {5, "polls", SectionScrollModeEnum.SECTION}
    };
  }

  private Section getSectionById(int id) {
    for (Section section: sectionList) {
      if (section.getId() == id) {
        return section;
      }
    }
    return null;
  }
}
