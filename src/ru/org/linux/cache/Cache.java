package ru.org.linux.cache;

import java.util.Hashtable;
import java.util.Map;

public class Cache {
  private final Map cache;

  public Cache() {
    cache = new Hashtable();
  }

  public synchronized void putObject(String key, Object obj, long version) {
    cache.put(key, new CacheObject(obj, version));
  }

  public synchronized void putObject(String key, CacheObject cobj) {
    cache.put(key, cobj);
  }

  public synchronized CacheObject getObject(String key) {
    return (CacheObject) cache.get(key);
  }

  public synchronized void clear() {
    cache.clear();
  }
}
