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
package ru.org.linux.group;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import ru.org.linux.section.Section;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration("integration-tests-context.xml")
public class GroupDaoIntegrationTest extends AbstractTestNGSpringContextTests {
  @Autowired

  GroupDao groupDao;
  private Section sectionDto;

  @BeforeMethod
  public void sectionDtoInit() {
    sectionDto = mock(Section.class);
    when(sectionDto.getId()).thenReturn(Section.SECTION_FORUM);
  }

  @Test
  public void groupsCountInSectionForum() {
    // given

    // when
    List<Group> groupDtoList = groupDao.getGroups(sectionDto);

    // then
    Assert.assertEquals(16, groupDtoList.size());
  }

  @Test(expectedExceptions = {BadGroupException.class})
  public void badGroupName() throws BadGroupException {
    // given

    // when
    groupDao.getGroup(sectionDto, "bad group name");

    // then
  }

  @Test(dataProvider = "sectionForumHaveGroupNamedDataSource")
  public void sectionForumHaveGroupNamed(
    String inputGroupName,
    String expectedGroupTitle
  ) throws BadGroupException {
    // given

    // when
    Group groupDto = groupDao.getGroup(sectionDto, inputGroupName);

    // then
    Assert.assertEquals(groupDto.getTitle(), expectedGroupTitle);
  }

  @DataProvider(name = "sectionForumHaveGroupNamedDataSource")
  public Object[][] sectionForumHaveGroupNamedDataSource() {
    return new Object[][]{
      new Object[]{"admin", "Admin"},
      new Object[]{"desktop", "Desktop"},
      new Object[]{"development", "Development"},
      new Object[]{"games", "Games"},
      new Object[]{"general", "General"},
      new Object[]{"job", "Job"},
      new Object[]{"linux-hardware", "Linux-hardware"},
      new Object[]{"linux-install", "Linux-install"},
      new Object[]{"linux-org-ru", "Linux-org-ru"},
      new Object[]{"lor-source", "Lor-source"},
      new Object[]{"mobile", "Mobile"},
      new Object[]{"multimedia", "Multimedia"},
      new Object[]{"security", "Security"},
      new Object[]{"talks", "Talks"},
      new Object[]{"web-development", "Web-development"},
      new Object[]{"club", "Клуб"}
    };
  }

}
