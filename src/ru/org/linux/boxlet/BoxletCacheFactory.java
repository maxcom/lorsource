package ru.org.linux.boxlet;

import java.io.IOException;
import java.util.Properties;

import ru.org.linux.cache.Cache;
import ru.org.linux.cache.CacheObject;
import ru.org.linux.util.HTMLFormatter;
import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.UtilException;

public class BoxletCacheFactory extends BoxletFactory {
  private final BoxletFactory factory;
  private final Cache cache;

  private boolean nocache;

  public BoxletCacheFactory(Object Config, BoxletFactory Factory, Cache aCache) {
    super(Config);
    factory = Factory;
    cache = aCache;
    nocache = false;
  }

  public void setCacheMode(boolean noCache) {
    nocache = noCache;
  }

  public String getContent(String name, ProfileHashtable profile) throws IOException, BoxletException, UtilException {
    String variant = factory.getVariantID(name, profile, null);

    String key;
    if (variant != null && !"".equals(variant)) {
      key = name + '?' + variant;
    } else {
      key = name;
    }

    CacheObject cobj = null;

    if (!nocache) {
      cobj = cache.getObject(key);
    }

    long version = factory.getVersionID(name, profile, null);

    String content;
    boolean needupdate = false;
    if (cobj == null) {
      try {
        content = factory.getContent(name, profile);
        cache.putObject(key, content, version);
      } catch (Exception e) {
        if (profile.getBooleanProperty("DebugMode")) {
          content = "<h2>Ошибка: " + e.toString() + "</h2>" + HTMLFormatter.nl2br(StringUtil.getStackTrace(e));
        } else {
          content = "<h2>Ошибка</h2>";
        }
      }
    } else {
      content = cobj.getString();
      synchronized (cache) {
        if (cobj.getVersion() != version) {
          needupdate = true;
          cache.putObject(key, content, version); // Force all others to show old info. until ve update it.
        }
      }
    }

    if (needupdate) {
      try {
        content = factory.getContent(name, profile);
        synchronized (cache) {
          cache.putObject(key, content, version);
        }
      } catch (Exception e) { // show old info instead of error when possible
        if (profile.getBooleanProperty("DebugMode")) {
          content = "Ошибка, using old cache:<i>" + e.toString() + "</i><br>" + cobj.getString();
        } else {
          content = cobj.getString();
        }
      }
    }

    return content;
  }

  public String getMenuContent(String name, ProfileHashtable profile, String addUrl, String removeUrl) throws IOException, BoxletException, UtilException {
    String variant = factory.getVariantID(name, profile, null);

    String key;
    if (variant != null && !"".equals(variant)) {
      key = name + '?' + variant;
    } else {
      key = name;
    }

    key = "edit-" + key;

    CacheObject cobj = cache.getObject(key);

    long version = factory.getVersionID(name, profile, null);

    String content;
    boolean needupdate = false;
    if (cobj == null) {
      content = factory.getMenuContent(name, profile, addUrl, removeUrl);
      cache.putObject(key, content, version);
    } else {
      content = cobj.getString();
      synchronized (cobj) {
        if (cobj.getVersion() != version) {
          needupdate = true;
          cache.putObject(key, content, version); // Make others serve old content while we are fetching new one.
        }
      }
    }
    if (needupdate) {
      content = factory.getMenuContent(name, profile, addUrl, removeUrl);
      synchronized (cobj) {
        cache.putObject(key, content, version);
      }
    }

    return content;
  }


  String getVariantID(String name, ProfileHashtable profile, Properties request) throws BoxletException, UtilException {
    return factory.getVariantID(name, profile, request);
  }

  long getVersionID(String name, ProfileHashtable profile, Properties request) throws BoxletException {
    return factory.getVersionID(name, profile, request);
  }

}
