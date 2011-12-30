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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:commonDAO-context.xml")
public class SectionDaoIntegrationTest {
  @Autowired
  SectionDao sectionDao;

  private SectionDto  getSectionById(List<SectionDto> sectionList, int id) {
    for (SectionDto section: sectionList) {
      if (section.getId() == id) {
        return section;
      }
    }
    return null;
  }

  @Test
  public void sectionsTest()
      throws Exception {

    List<SectionDto> sectionList = sectionDao.getAllSections();
    Assert.assertEquals(4, sectionList.size());

    String addInfo = sectionDao.getAddInfo(sectionList.get(0).getId());
    Assert.assertNotNull(addInfo);
  }

  @Test
  public void sectionsScrollModeTest()
    throws Exception {

    List<SectionDto> sectionList = sectionDao.getAllSections();

    SectionDto section;
    section = getSectionById(sectionList, 1);
    Assert.assertNotNull(section);
    Assert.assertEquals(SectionScrollModeEnum.SECTION.toString(), section.getScrollMode());

    section = getSectionById(sectionList, 2);
    Assert.assertNotNull(section);
    Assert.assertEquals(SectionScrollModeEnum.GROUP.toString(), section.getScrollMode());

    section = getSectionById(sectionList, 3);
    Assert.assertNotNull(section);
    Assert.assertEquals(SectionScrollModeEnum.SECTION.toString(), section.getScrollMode());

    section = getSectionById(sectionList, 5);
    Assert.assertNotNull(section);
    Assert.assertEquals(SectionScrollModeEnum.SECTION.toString(), section.getScrollMode());
  }

  @Test
  public void extendedSectionsTest()
    throws Exception {

    List<SectionDto> sectionList = sectionDao.getAllSections();

    SectionDto section;
    section = getSectionById(sectionList, 1);
    Assert.assertEquals("Новости", section.getTitle());
    Assert.assertEquals("news", section.getName());
    Assert.assertEquals("/news/", section.getLink());
    Assert.assertEquals("/news/", section.getFeedLink());
    Assert.assertEquals(-9999, section.getMinCommentScore());

    section = getSectionById(sectionList, 2);
    Assert.assertEquals("Форум", section.getTitle());
    Assert.assertEquals("forum", section.getName());
    Assert.assertEquals("/forum/", section.getLink());
    Assert.assertEquals("/forum/lenta/", section.getFeedLink());
    Assert.assertEquals(-9999, section.getMinCommentScore());

    section = getSectionById(sectionList, 3);
    Assert.assertEquals("Галерея", section.getTitle());
    Assert.assertEquals("gallery", section.getName());
    Assert.assertEquals("/gallery/", section.getLink());
    Assert.assertEquals("/gallery/", section.getFeedLink());
    Assert.assertEquals(50, section.getMinCommentScore());

    section = getSectionById(sectionList, 5);
    Assert.assertEquals("Голосования", section.getTitle());
    Assert.assertEquals("polls", section.getName());
    Assert.assertEquals("/polls/", section.getLink());
    Assert.assertEquals("/polls/", section.getFeedLink());
    Assert.assertEquals(50, section.getMinCommentScore());
  }
}
