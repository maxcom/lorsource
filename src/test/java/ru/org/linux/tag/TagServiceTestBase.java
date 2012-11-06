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

package ru.org.linux.tag;

import org.testng.annotations.BeforeMethod;

import static org.mockito.Mockito.mock;

public abstract class TagServiceTestBase {
  protected TagService tagService;

  protected TagDao tagDao;

  @BeforeMethod
  public void resetTagDaoMock() {
    tagDao = mock(TagDao.class);

    tagService = new TagService();
    tagService.setTagDao(tagDao);
  }

}
