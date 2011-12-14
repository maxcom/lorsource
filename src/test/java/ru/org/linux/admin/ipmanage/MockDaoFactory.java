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

package ru.org.linux.admin.ipmanage;

import org.springframework.stereotype.Service;
import ru.org.linux.profile.IgnoreListDao;
import ru.org.linux.user.UserDao;

import static org.mockito.Mockito.mock;

@Service
public class MockDaoFactory {
  private static SameIpDao sameIpDao = null;
  private static IpBlockDao ipBlockDao = null;
  private static UserDao userDao = null;
  private static IgnoreListDao ignoreListDao = null;

  public SameIpDao getSameIpDao() {
    if (sameIpDao == null) {
      sameIpDao = mock(SameIpDao.class);
    }
    return sameIpDao;
  }

  public IpBlockDao getIpBlockDao() {
    if (ipBlockDao == null) {
      ipBlockDao = mock(IpBlockDao.class);
    }
    return ipBlockDao;
  }

  public UserDao getUserDao() {
    if (userDao == null) {
      userDao = mock(UserDao.class);
    }
    return userDao;
  }

  public IgnoreListDao getIgnoreListDao() {
    if (ignoreListDao == null) {
      ignoreListDao = mock(IgnoreListDao.class);
    }
    return ignoreListDao;
  }

}
