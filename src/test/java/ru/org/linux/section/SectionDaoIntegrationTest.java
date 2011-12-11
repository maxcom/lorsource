/*
 * Copyright 1998-2011 Linux.org.ru
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

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:commonDAO-context.xml")
public class SectionDaoIntegrationTest {
  @Autowired
  SectionDao sectionDao;

  @Test
  public void sectionsTest()
      throws Exception {
    ImmutableList<Section> sectionDtoImmutableList = sectionDao.getSectionsList();
    Assert.assertEquals(4, sectionDtoImmutableList.size());
    Section sectionDto = sectionDao.getSection(Section.SECTION_FORUM);
    String addInfo = sectionDao.getAddInfo(sectionDto.getId());
    Assert.assertNotNull(addInfo);
    Assert.assertTrue("Форум".equals(sectionDto.getName()));
    Assert.assertEquals(2, sectionDto.getId());

  }
}
