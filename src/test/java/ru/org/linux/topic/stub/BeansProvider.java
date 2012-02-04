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

package ru.org.linux.topic.stub;

import ru.org.linux.spring.commons.CacheProvider;
import ru.org.linux.topic.TagDao;

import static org.mockito.Mockito.*;

public class BeansProvider {
  
  public TagDao getTagDao ()
    throws Exception {
    TagDao tagDao = mock(TagDao.class);
    when(tagDao.getTagId("LOR")).thenReturn(123);
    return tagDao;
  }

  public CacheProvider getCacheProvider() {
    CacheProvider cacheProvider = new CacheProvider() {
      @Override
      public Object getFromCache(String key) {
        return null;
      }

      @Override
      public <T> void storeToCache(String key, T value, int expire) {
      }

      @Override
      public <T> void storeToCache(String key, T value) {
      }
    };
    return cacheProvider;
  }
}
