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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("commonDAO-context.xml")
public class UserAgentDaoIntegrationTests {
  @Autowired
  UserAgentDao userAgentDao;

  @Test
  public void userAgentTest() {
    String userAgent = userAgentDao.getUserAgentById(10);
    Assert.assertTrue("Mozilla/5.0 (X11; U; Linux i686; ru; rv:1.9.1b4) Gecko/20090427 Fedora/3.5-0.20.beta4.fc11 Firefox/3.5b4".equals(userAgent));
  }
}
