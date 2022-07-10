/*
 * Copyright 1998-2022 Linux.org.ru
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

import org.junit.{Assert, Test}
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("integration-tests-context.xml")) class SectionDaoIntegrationTest {
  @Autowired
  private var sectionDao: SectionDao = _

  private def getSectionById(sections: Seq[Section], id: Int) = sections.find(_.getId == id).orNull

  @Test
  def sectionsScrollModeTest() = {
    val sectionList = sectionDao.getAllSections

    var section = getSectionById(sectionList, 1)
    Assert.assertNotNull(section)
    Assert.assertEquals(SectionScrollModeEnum.SECTION, section.getScrollMode)

    section = getSectionById(sectionList, 2)
    Assert.assertNotNull(section)
    Assert.assertEquals(SectionScrollModeEnum.GROUP, section.getScrollMode)

    section = getSectionById(sectionList, 3)
    Assert.assertNotNull(section)
    Assert.assertEquals(SectionScrollModeEnum.SECTION, section.getScrollMode)

    section = getSectionById(sectionList, 5)
    Assert.assertNotNull(section)
    Assert.assertEquals(SectionScrollModeEnum.SECTION, section.getScrollMode)
  }
}