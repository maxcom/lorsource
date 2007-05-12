package ru.org.linux.boxlet;

import java.io.IOException;
import java.util.Properties;

import ru.org.linux.cache.Cache;
import ru.org.linux.util.HTMLFormatter;
import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.UtilException;

public class BoxletRunner {
  private final String name;
  private final Cache cache;
  private boolean nocache;

  public BoxletRunner(String n, Cache aCache) {
    name = n;
    cache = aCache;
    nocache = false;
  }

  public void setCacheMode(boolean noCache) {
    nocache = noCache;
  }

  public String getContent(Object config, ProfileHashtable profile) throws IOException, UtilException {
    StringBuffer out = new StringBuffer();
    BoxletFactory loader = new BoxletLoaderFactory(config);
    BoxletCacheFactory bx = new BoxletCacheFactory(config, loader, cache);
    bx.setCacheMode(nocache);

    try {
      out.append(bx.getContent(name, profile));
    } catch (Exception e) {
      if (profile.getBooleanProperty("DebugMode")) {
        out.append("<b>Ошибка получения " + name + ": " + e.toString() + "</b><p>" + HTMLFormatter.nl2br(StringUtil.getStackTrace(e)));
      } else {
        out.append("<b>Ошибка</b>");
      }
    }

    return out.toString();
  }

  public String getEditContent(Properties config, ProfileHashtable profile, String tag) {
    return null;
  }
}
