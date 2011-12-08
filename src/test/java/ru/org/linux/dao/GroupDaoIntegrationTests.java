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
package ru.org.linux.dao;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.org.linux.dto.GroupDto;
import ru.org.linux.dto.SectionDto;

import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("commonDAO-context.xml")
public class GroupDaoIntegrationTests {
  @Autowired
  GroupDao groupDao;

  @Test
  public void groupsTest()
      throws Exception {
    SectionDto sectionDto = mock(SectionDto.class);
    when(sectionDto.getId()).thenReturn(SectionDto.SECTION_FORUM);

    List<GroupDto> groupDtoList = groupDao.getGroups(sectionDto);
    Assert.assertEquals(16, groupDtoList.size());

    GroupDto groupDto = groupDao.getGroup(sectionDto, "general");
    Assert.assertTrue("General".equals(groupDto.getTitle()));
  }
}
