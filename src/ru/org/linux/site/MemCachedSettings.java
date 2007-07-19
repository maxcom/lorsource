package ru.org.linux.site;

import com.danga.MemCached.MemCachedClient;
import com.danga.MemCached.SockIOPool;

public class MemCachedSettings {
  private final static MemCachedSettings me = new MemCachedSettings();
  private final MemCachedClient mc = new MemCachedClient();

  private MemCachedSettings() {
    SockIOPool pool = SockIOPool.getInstance();
    pool.setServers(new String[] { "localhost:11211" });

    pool.initialize();

    mc.setSanitizeKeys(false);
  }

  public static MemCachedClient getClient() {
    return me.mc;
  }

  public static String getId(Template tmpl, String suffix) {
    return tmpl.getMainUrl()+"/"+suffix;
  }
}
