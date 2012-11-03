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
import org.testng.annotations.Test;
import ru.org.linux.section.Section;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration("integration-tests-context.xml")
public class GroupDaoIntegrationTest extends AbstractTestNGSpringContextTests {
  @Autowired

  GroupDao groupDao;

  @Test
  public void groupsTest()
      throws Exception {
    Section sectionDto = mock(Section.class);
    when(sectionDto.getId()).thenReturn(Section.SECTION_FORUM);

    List<Group> groupDtoList = groupDao.getGroups(sectionDto);
    Assert.assertEquals(16, groupDtoList.size());

    Group groupDto = groupDao.getGroup(sectionDto, "general");
    Assert.assertEquals("General", groupDto.getTitle());
  }
}
