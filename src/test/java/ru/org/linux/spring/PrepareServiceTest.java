/*
 * Copyright 1998-2010 Linux.org.ru
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

package ru.org.linux.spring;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ru.org.linux.dao.GroupDao;
import ru.org.linux.dao.PollDao;
import ru.org.linux.dao.SectionDao;
import ru.org.linux.dao.UserDao;
import ru.org.linux.site.Message;
import ru.org.linux.spring.dao.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PrepareServiceTest {

  /**
   * TODO дописать, написать общий тестовый клас со слепком данных для тестирования, как то
   * TODO пару тройку ResultSet для разных классов и сами классы
   */
  @Test
  public void prepareMessageTest() {
    ImmutableList<String> tags = ImmutableList.of("one", "two");
    Message message = mock(Message.class);

    PollDao pollDao = mock(PollDao.class);
    GroupDao groupDao = mock(GroupDao.class);
    UserDao userDao = mock(UserDao.class);
    SectionDao sectionDao = mock(SectionDao.class);
    DeleteInfoDao deleteInfoDao = mock(DeleteInfoDao.class);
    MessageDao messageDao = mock(MessageDao.class);
    UserAgentDao userAgentDao = mock(UserAgentDao.class);

    when(message.getGroupId()).thenReturn(13); // group id 13

  }

}
