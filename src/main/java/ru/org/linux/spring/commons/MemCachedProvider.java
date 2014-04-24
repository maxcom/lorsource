/*
 * Copyright 1998-2013 Linux.org.ru
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

import net.spy.memcached.OperationTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.org.linux.site.MemCachedSettings;

public class MemCachedProvider implements CacheProvider {
  private static final Logger logger = LoggerFactory.getLogger(MemCachedProvider.class);

  @Override
  public Object getFromCache(String key) {
    String s = MemCachedSettings.getId(key);
    try {
      if (MemCachedSettings.getMemCachedClient().getAvailableServers().isEmpty()) {
        return null;
      }

      return MemCachedSettings.getMemCachedClient().get(s);
    } catch (RuntimeException ex) {
      logger.info("Memcached GET failed", ex);
      return null;
    }
  }

  @Override
  public <T> void storeToCache(String key, T value) {
    String s = MemCachedSettings.getId(key);
    try {
      if (MemCachedSettings.getMemCachedClient().getAvailableServers().isEmpty()) {
        return;
      }

      MemCachedSettings.getMemCachedClient().set(s, 0, value);
    } catch (IllegalArgumentException | IllegalStateException | OperationTimeoutException ex) {
      logger.info("Memcached SET failed", ex);
    }
  }

  public void destroy() {
    logger.debug("Shutting down memcached");
    MemCachedSettings.getMemCachedClient().shutdown();
  }
}
