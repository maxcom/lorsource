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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.org.linux.dto.UserDto;
import ru.org.linux.exception.AccessViolationException;

import java.util.Iterator;
import java.util.Set;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("commonDAO-context.xml")
public class IgnoreListDaoIntegrationTests {
  private static final int USER1_ID = 26182;
  private static final int USER2_ID = 10659;
  private static final int USER3_ID = 3950;
  private static final int USER4_ID = 28589;

  @Autowired
  IgnoreListDao ignoreListDao;

  /**
   * Проверка работы игнор-листа.
   */
  @Test
  public void getGalleryItemsTest()
      throws Exception{

    UserDto user1 = mock(UserDto.class);
    when(user1.getId()).thenReturn(USER1_ID);
    when(user1.isModerator()).thenReturn(false);

    UserDto user2 = mock(UserDto.class);
    when(user2.getId()).thenReturn(USER2_ID);
    when(user2.isModerator()).thenReturn(false);

    UserDto user3 = mock(UserDto.class);
    when(user3.getId()).thenReturn(USER3_ID);
    when(user3.isModerator()).thenReturn(true);

    UserDto user4 = mock(UserDto.class);
    when(user4.getId()).thenReturn(USER4_ID);
    when(user4.isModerator()).thenReturn(false);

    ignoreListDao.addUser(user1, user2);
    try {
      ignoreListDao.addUser(user1, user2);
      Assert.fail("Дубликат в списке игнорирования!");
    } catch (DuplicateKeyException ignored) {
      // всё в порядке
    } finally {
      ignoreListDao.remove(user1, user2);
    }

    try{
      ignoreListDao.addUser(user1, user3);
      Assert.fail("Модератора нельзя игнорировать!");
    } catch (AccessViolationException ignored) {
      // всё в порядке
    } finally {
      ignoreListDao.remove(user1, user2);
      ignoreListDao.remove(user1, user3);
    }

    ignoreListDao.addUser(user1, user2);
    ignoreListDao.addUser(user1, user4);
    ignoreListDao.addUser(user2, user4);
    try {
      // Проверяем количество игнорирующих
      Assert.assertEquals(1, ignoreListDao.getIgnoreStat(user2));
      Assert.assertEquals(2, ignoreListDao.getIgnoreStat(user4));

      // Проверяем список ингорируемых
      Set<Integer> ignoredUsers = ignoreListDao.get(user1);
      Assert.assertEquals(2, ignoredUsers.size());

      Iterator<Integer> iterator = ignoredUsers.iterator();
      int userId = iterator.next().intValue();
      if (userId == USER2_ID) {
        Assert.assertEquals(USER2_ID, userId);
        Assert.assertEquals(USER4_ID, iterator.next().intValue());
      } else {
        Assert.assertEquals(USER4_ID, userId);
        Assert.assertEquals(USER2_ID, iterator.next().intValue());
      }

    } finally {
      ignoreListDao.remove(user1, user2);
      ignoreListDao.remove(user1, user4);
      ignoreListDao.remove(user2, user4);
    }
  }
}
