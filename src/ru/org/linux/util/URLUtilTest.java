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

package ru.org.linux.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class URLUtilTest {
  @Test
  public void testSimpleURL() {
    assertTrue(URLUtil.isUrl("http://www.linux.org.ru/"));
  }

  @Test
  public void testURL() {
    assertTrue(URLUtil.isUrl("http://www.linux.org.ru:80/dfrer.jsp?msgid=3412345"));
  }

  @Test
  public void testBadURL() {
    assertFalse(URLUtil.isUrl("xttp://www.linux.org.ru/"));
  }

  @Test
  public void testIPUrl() {
    assertTrue(URLUtil.isUrl("http://127.0.0.1/"));
  }

  @Test
  public void testCyrillicURL() {
    assertTrue(URLUtil.isUrl("http://президент.рф/"));
  }
}
