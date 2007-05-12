package ru.org.linux.cache;

public class CacheObject {
  private final Object obj;
  private final long version;

  public CacheObject(Object object, long ver) {
    obj = object;
    version = ver;
  }

  public Object getObject() {
    return obj;
  }

  public String getString() {
    return (String) obj;
  }

  public long getVersion() {
    return version;
  }
}
