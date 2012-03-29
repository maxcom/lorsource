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

package ru.org.linux.user.stub;

import ru.org.linux.topic.TagDao;
import ru.org.linux.user.UserTagDao;

import static org.mockito.Mockito.mock;

public class BeansProvider {

  public UserTagDao getUserTagDao()
    throws Exception {
    UserTagDao userTagDao = mock(UserTagDao.class);
    return userTagDao;
  }

  public TagDao getTagDao()
    throws Exception {
    TagDao tagDao = mock(TagDao.class);
    return tagDao;
  }
}
