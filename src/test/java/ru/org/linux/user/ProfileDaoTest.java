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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes=ProfileDaoTestConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class ProfileDaoTest {
  @Autowired
  private ProfileDao profileDao;

  private User testUser;

  @Before
  public void initTestUser() {
    testUser = mock(User.class);
    when(testUser.getNick()).thenReturn("test-user");
  }

  @Test
  public void testDefaultProfile() {
    Profile profile = new Profile();

    assertTrue(profile.isDefault());
  }

  @Test
  public void testDefaultProfileSave() throws Exception {
    Profile profile = new Profile();

    profileDao.writeProfile(testUser, profile);

    Profile profile1 = profileDao.readProfile(testUser);

    assertFalse(profile1.isDefault());
  }

  @Test
  public void testModification() throws Exception {
    Profile profile = new Profile();

    assertNotSame(125, profile.getProperties().getMessages());

    profile.getProperties().setMessages(125);

    profileDao.writeProfile(testUser, profile);

    Profile profile1 = profileDao.readProfile(testUser);

    assertEquals(125, profile1.getProperties().getMessages());
  }
}
