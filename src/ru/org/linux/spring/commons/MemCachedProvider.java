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

package ru.org.linux.spring.commons;

import java.util.Date;

import ru.org.linux.site.MemCachedSettings;

public class MemCachedProvider implements CacheProvider {
  public Object getFromCache(String key) {
    String s = MemCachedSettings.getId(key);
    return MemCachedSettings.getClient().get(s);
  }

  public <T> boolean storeToCache(String key, T value, long expire) {
    String s = MemCachedSettings.getId(key);
    Date now = new Date();
    return MemCachedSettings.getClient().add(s, value, new Date(now.getTime() + expire));
  }
}
