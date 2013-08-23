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

package ru.org.linux.site;

import java.io.IOException;
import java.net.InetSocketAddress;

import net.spy.memcached.MemcachedClient;

import ru.org.linux.spring.commons.CacheProvider;
import ru.org.linux.spring.commons.MemCachedProvider;

public class MemCachedSettings {
  private static final MemCachedSettings me;static {
    try {
      me = new MemCachedSettings();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private final MemcachedClient mc;
  private final MemCachedProvider provider = new MemCachedProvider();

  private static String mainUrl = "uninitialized/";

  private MemCachedSettings() throws IOException {
    mc = new MemcachedClient(new InetSocketAddress("127.0.0.1", 11211));
  }

  public static MemcachedClient getMemCachedClient() {
    return me.mc;
  }

  public static CacheProvider getCache() {
    return me.provider;
  }

  public static String getId(String suffix) {
    return mainUrl+ '/' +suffix;
  }

  public static void setMainUrl(String mainUrl) {
    MemCachedSettings.mainUrl = mainUrl;
  }
}
