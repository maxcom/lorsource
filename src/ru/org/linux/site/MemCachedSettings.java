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

import com.danga.MemCached.Logger;
import com.danga.MemCached.MemCachedClient;
import com.danga.MemCached.SockIOPool;

public class MemCachedSettings {
  private static final MemCachedSettings me = new MemCachedSettings();
  private final MemCachedClient mc = new MemCachedClient();

  private static String mainUrl = "uninitialized/";

  private MemCachedSettings() {
    Logger logger = Logger.getLogger("com.danga.MemCached.MemCachedClient");
    logger.setLevel(Logger.LEVEL_WARN);

    SockIOPool pool = SockIOPool.getInstance();
    pool.setServers(new String[] { "localhost:11211" });

    pool.setNagle(false);

    pool.initialize();

    mc.setSanitizeKeys(false);
  }

  public static MemCachedClient getClient() {
    return me.mc;
  }

  public static String getId(String suffix) {
    return mainUrl+ '/' +suffix;
  }

  public static void setMainUrl(String mainUrl) {
    MemCachedSettings.mainUrl = mainUrl;
  }
}
