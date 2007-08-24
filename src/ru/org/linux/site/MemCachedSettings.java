package ru.org.linux.site;

import com.danga.MemCached.MemCachedClient;
import com.danga.MemCached.SockIOPool;

public class MemCachedSettings {
  private static final MemCachedSettings me = new MemCachedSettings();
  private final MemCachedClient mc = new MemCachedClient();

  private static String mainUrl = "uninitialized/";

  private MemCachedSettings() {
    SockIOPool pool = SockIOPool.getInstance();
    pool.setServers(new String[] { "localhost:11211" });

    pool.initialize();

    mc.setSanitizeKeys(false);
  }

  public static MemCachedClient getClient() {
    return me.mc;
  }

  public static String getId(String suffix) {
    return mainUrl+"/"+suffix;
  }

  public static void setMainUrl(String mainUrl) {
    MemCachedSettings.mainUrl = mainUrl;
  }
}
