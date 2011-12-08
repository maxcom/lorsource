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
import ru.org.linux.dto.IPBlockInfoDto;
import ru.org.linux.dto.UserDto;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("commonDAO-context.xml")
public class IPBlockDaoIntegrationTests {

  @Autowired
  IPBlockDao ipBlockDao;

  private static final String TEST_IP = "4.5.6.7";

  @Test
  public void ipBlockTest() {
    UserDto moderator = mock(UserDto.class);
    when(moderator.getId()).thenReturn(1);

    ipBlockDao.blockIP(TEST_IP, moderator, "test", null);

    try {
      IPBlockInfoDto ipBlockInfoDto = ipBlockDao.getBlockInfo(TEST_IP);
      Assert.assertTrue(ipBlockInfoDto != null);
      String reason = ipBlockInfoDto.getReason();
      Assert.assertTrue("test".equals(reason));
      Assert.assertEquals(1, ipBlockInfoDto.getModerator());
    } finally {
      ipBlockDao.delete(TEST_IP);
    }
  }
}
