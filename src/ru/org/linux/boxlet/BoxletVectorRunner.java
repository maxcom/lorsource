package ru.org.linux.boxlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;

import ru.org.linux.cache.Cache;
import ru.org.linux.util.HTMLFormatter;
import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.UtilException;

public class BoxletVectorRunner {
  final List boxes;
  final Cache cache;
  private boolean nocache;

  public BoxletVectorRunner(List v, Cache aCache) {
    boxes = v;
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

    for (Iterator i = boxes.iterator(); i.hasNext(); ) {
      String name = (String) i.next();

      try {
        out.append("<div class=boxlet>");
        out.append(bx.getContent(name, profile));
        out.append("</div>");
      } catch (Exception e) {
        if (profile.getBoolean("DebugMode")) {
          out.append("<b>Ошибка получения " + name + ": " + e.toString() + "</b><p>" + HTMLFormatter.nl2br(StringUtil.getStackTrace(e)));
        } else {
          out.append("<b>Ошибка</b>");
        }
      }

    }
    return out.toString();
  }

  public String getEditContent(Object config, ProfileHashtable profile, String tag) throws IOException, UtilException {
    BoxletFactory bx = new BoxletLoaderFactory(config);
    StringBuffer out = new StringBuffer();

    for (int i = 0; i < boxes.size(); i++) {
      try {
        out.append("<div class=boxlet>");
        out.append(bx.getMenuContent((String) boxes.get(i), profile, "edit-boxes.jsp?tag=" + URLEncoder.encode(tag) + "&mode=add&id=" + i, "edit-boxes.jsp?tag=" + URLEncoder.encode(tag) + "&mode=remove&id=" + i));
        out.append("</div>");
      } catch (Exception e) {
        if (profile.getBoolean("DebugMode")) {
          out.append("<b>Ошибка получения " + boxes.get(i) + ": " + e.toString() + "</b><p>" + HTMLFormatter.nl2br(StringUtil.getStackTrace(e)));
        } else {
          out.append("<b>Ошибка</b>");
        }
      }
    }

    out.append("[<a href=\"edit-boxes.jsp?tag=" + URLEncoder.encode(tag) + "&mode=add\">Добавить</a>]<p>");

    return out.toString();
  }
}
