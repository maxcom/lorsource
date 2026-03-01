/*
 * Copyright 1998-2026 Linux.org.ru
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.junit.Assert.*;

@ContextHierarchy({
        @ContextConfiguration("classpath:database.xml"),
        @ContextConfiguration(classes = SimpleIntegrationTestConfiguration.class)
})
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class UserServiceIntegrationTest {
  public static final int TEST_ID = 7806;

  @Autowired
  private UserService userService;

  private JdbcTemplate jdbcTemplate;

  @Autowired
  private void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  @Before
  @After
  public void fixUser() {
    jdbcTemplate.update("UPDATE users SET blocked='f' WHERE id=?", TEST_ID);
    jdbcTemplate.update("DELETE FROM ban_info WHERE userid=?", TEST_ID);
  }

  @Before
  public void clearCache() {
    userService.idToUserCache().invalidateAll();
  }

  @Test
  public void testUserCached() throws UserNotFoundException {
    User user = userService.getUserCached(TEST_ID);

    jdbcTemplate.update("UPDATE users SET blocked='t' WHERE id=?", TEST_ID);

    User userCached = userService.getUserCached(TEST_ID);

    assertFalse(userCached.isBlocked());

    User userNotCached = userService.getUser(user.nick());

    assertTrue(userNotCached.isBlocked());
  }

  @Test
  public void testCachePutOnGet() throws UserNotFoundException {
    userService.idToUserCache().invalidate(TEST_ID);

    User user = userService.getUserCached(TEST_ID);

    assertNotNull(user);

    assertFalse(user.isBlocked());

    assertNotNull(userService.idToUserCache().get(user.getId()));
  }

  @Test
  public void testBlock() throws UserNotFoundException {
    User user = userService.getUserCached(TEST_ID);

    userService.block(user, user, "");

    User userAfter = userService.getUserCached(TEST_ID);

    assertTrue(userAfter.isBlocked());
  }

  @Test
  public void testCacheResetOnBlock() throws UserNotFoundException {
    User user = userService.getUserCached(TEST_ID);

    userService.block(user, user, "");

    User userAfter = userService.getUserCached(TEST_ID);

    assertTrue(userAfter.isBlocked());
  }
}
