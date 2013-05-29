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
package ru.org.linux.user;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

/**
 */
@ContextConfiguration(classes=MemoriesDaoIntegrationTestConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class MemoriesDaoIntegrationTest {

  @Autowired
  private UserDao userDao;

  @Autowired
  private MemoriesDao memoriesDao;


  @Test
  public void test1() {
    User maxcom = userDao.getUser(1);
    assertTrue(memoriesDao.getWatchCountForUser(maxcom) > 0);
    assertTrue(memoriesDao.isWatchPresetForUser(maxcom));
    User anonymous = userDao.getUser(2);
    assertTrue(memoriesDao.getWatchCountForUser(anonymous) == 0);
    assertFalse(memoriesDao.isFavPresetForUser(anonymous));
  }

}
