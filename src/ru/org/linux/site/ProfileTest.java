/*
 * Copyright 1998-2009 Linux.org.ru
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

package ru.org.linux.site;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.TestCase;

public class ProfileTest extends TestCase {
  public void testDefaultProfile() {
    new Profile(null);
  }

  public void testDefaultProfileSave() throws IOException, ClassNotFoundException {
    Profile profile = new Profile(null);
    ByteArrayOutputStream os = new ByteArrayOutputStream();

    profile.write(os);

    new Profile(new ByteArrayInputStream(os.toByteArray()) , "test");
  }

  public void testModification() throws IOException, ClassNotFoundException {
    Profile profile = new Profile(null);

    profile.getHashtable().setInt("messages", new Integer(125));

    ByteArrayOutputStream os = new ByteArrayOutputStream();

    profile.write(os);

    new Profile(new ByteArrayInputStream(os.toByteArray()) , "test");
  }

}
