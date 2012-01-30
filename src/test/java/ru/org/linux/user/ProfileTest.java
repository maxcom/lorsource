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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProfileTest {
  @Test
  public void testDefaultProfile() {
    Profile profile = new Profile();

    assertTrue(profile.isDefault());
  }

  @Test
  public void testDefaultProfileSave() throws Exception {
    Profile profile = new Profile();
    ByteArrayOutputStream os = new ByteArrayOutputStream();

    profile.write(os);

    Profile profile1 = new Profile(new ByteArrayInputStream(os.toByteArray()));

    assertFalse(profile1.isDefault());
  }

  @Test
  public void testModification() throws Exception {
    Profile profile = new Profile();

    assertNotSame(125, profile.getProperties().getMessages());

    profile.getProperties().setMessages(125);

    ByteArrayOutputStream os = new ByteArrayOutputStream();

    profile.write(os);

    Profile profile1 = new Profile(new ByteArrayInputStream(os.toByteArray()));

    assertEquals(125, profile1.getProperties().getMessages());
  }

}
