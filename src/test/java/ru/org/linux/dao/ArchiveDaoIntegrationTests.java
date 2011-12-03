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
import ru.org.linux.dto.ArchiveDto;
import ru.org.linux.site.Section;

import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("commonDAO-context.xml")
public class ArchiveDaoIntegrationTests {

  @Autowired
  ArchiveDao archiveDao;

  @Test
  public void archiveTest() {
    Section section = mock(Section.class);
    when(section.getId()).thenReturn(Section.SECTION_FORUM);

    List<ArchiveDto> archiveDtoList = archiveDao.getArchiveDTO(section, 3);

    // TODO: для чистоты тестов необходимо создать отдельную СУБД, в которую вносить только те данные, которые потом и проверять.
    // Сейчас нет возможности точно проверить количество (и содержимое) архива, не удалив все архивы из существующей базы.
    // разве что создать свои секции и группы, но лучше создать тестовую СУБД.

    Assert.assertEquals(3, archiveDtoList.size());

  }
}
