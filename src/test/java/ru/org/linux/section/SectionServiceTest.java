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

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import ru.org.linux.topic.TopicPermissionService;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SectionServiceTest  {
  private SectionService sectionService;
  private SectionDao sectionDao;

  @BeforeMethod
  public void setUp() {
    sectionService = new SectionService();

    sectionDao = mock(SectionDao.class);
    when(sectionDao.getAllSections())
      .thenReturn(createSectionList());

    sectionService.setSectionDao(sectionDao);
    sectionService.initializeSectionList();
  }

  @Test(expectedExceptions = {SectionNotFoundException.class})
  public void getSectionIdByWrongName() {
    // given

    // when
    sectionService.getSectionIdByName("Section XXX");

    // then
  }

  @Test(dataProvider = "getSectionIdByNameDataSource")
  public void getSectionIdByName(
    String inputSectionName,
    int expectedSectionId
  ) {
    // given

    // when
    int actualSectionId =sectionService.getSectionIdByName(inputSectionName);

    // then
    Assert.assertEquals(actualSectionId, expectedSectionId);

    verify(sectionDao).getAllSections();
    verifyNoMoreInteractions(sectionDao);
  }

  @DataProvider(name = "getSectionIdByNameDataSource")
  public Object[][] getSectionIdByNameDataSource() {
    return new Object[][] {
      new Object[] {"Section 1", 1},
      new Object[] {"Section 2", 2},
      new Object[] {"Section 3", 3},
      new Object[] {"Section 4", 4},
    };
  }

  @Test(expectedExceptions = {SectionNotFoundException.class})
  public void getSectionByInvalidId() {
    // given

    // when
    sectionService.getSection(-1);

    // then
  }

  @Test(dataProvider = "getSectionDataSource")
  public void getSection(
    int inputSectionId,
    String expectedSectionName,
    boolean expectedIsPremoderated,
    boolean expectedIsPollPostAllowed,
    boolean expectedIsImagePost,
    SectionScrollModeEnum expectedScrollMode
  ) {
    // given

    // when
    Section section = sectionService.getSection(inputSectionId);

    // then
    Assert.assertNotNull(section);
    Assert.assertEquals(section.getName(), expectedSectionName);
    Assert.assertEquals(section.isPremoderated(), expectedIsPremoderated);
    Assert.assertEquals(section.isPollPostAllowed(), expectedIsPollPostAllowed);
    Assert.assertEquals(section.isImagepost(), expectedIsImagePost);
    Assert.assertEquals(section.getScrollMode(), expectedScrollMode);

    verify(sectionDao).getAllSections();
    verifyNoMoreInteractions(sectionDao);
  }

  @DataProvider(name = "getSectionDataSource")
  public Object[][] getSectionDataSource() {
    return new Object[][] {
      new Object[] {2, "Section 2", true, false, false, SectionScrollModeEnum.GROUP},
      new Object[] {3, "Section 3", true, false, true, SectionScrollModeEnum.SECTION},
      new Object[] {4, "Section 4", false, false, false, SectionScrollModeEnum.NO_SCROLL},
    };
  }

  @Test
  public void countOfSectionInList() {
    // given

    // when
    List<Section> sectionList = sectionService.getSectionList();

    // then
    Assert.assertEquals(5, sectionList.size());
    verify(sectionDao).getAllSections();
    verifyNoMoreInteractions(sectionDao);
  }

  @Test
  public void getAdditionalInformation() {
    // given
    final String additionInfo = "Extended info for Section 1";
    when(sectionDao.getAddInfo(eq(1)))
      .thenReturn(additionInfo);

    // when
    String additionalInfo = sectionService.getAddInfo(1);

    // then
    Assert.assertSame(additionInfo, additionalInfo);

    verify(sectionDao).getAllSections();
    verify(sectionDao).getAddInfo(eq(1));
    verifyNoMoreInteractions(sectionDao);
  }

  private List<Section> createSectionList() {
    List<Section> sectionList = new ArrayList<Section>();

    sectionList.add(new Section("Section 1", false, true, 1, false, "SECTION", TopicPermissionService.POSTSCORE_UNRESTRICTED));
    sectionList.add(new Section("Section 2", false, true, 2, false, "GROUP", TopicPermissionService.POSTSCORE_UNRESTRICTED));
    sectionList.add(new Section("Section 3", true, true, 3, false, "SECTION", TopicPermissionService.POSTSCORE_UNRESTRICTED));
    sectionList.add(new Section("Section 4", false, false, 4, false, "NO_SCROLL", TopicPermissionService.POSTSCORE_UNRESTRICTED));
    sectionList.add(new Section("Section 5", false, false, 5, true, "SECTION", TopicPermissionService.POSTSCORE_UNRESTRICTED));

    return sectionList;
  }
}
