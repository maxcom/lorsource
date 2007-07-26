package ru.org.linux.boxlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import com.danga.MemCached.MemCachedClient;

import ru.org.linux.site.MemCachedSettings;
import ru.org.linux.site.Template;
import ru.org.linux.util.HTMLFormatter;
import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.UtilException;

public class BoxletVectorRunner {
  private static final Logger logger = Logger.getLogger("ru.org.linux");

  private final List boxes;

  private final MemCachedClient mcc = MemCachedSettings.getClient();

  public BoxletVectorRunner(List v) {
    boxes = v;
  }

  public String getContent(Template tmpl, Object config, ProfileHashtable profile) throws IOException, UtilException {
    StringBuffer out = new StringBuffer();
    BoxletLoader loader = new BoxletLoader();

    for (Iterator i = boxes.iterator(); i.hasNext(); ) {
      String name = (String) i.next();

      try {
        Boxlet bx = loader.loadBox(name);

        String cacheId = MemCachedSettings.getId(tmpl, "boxlet?name="+name+"&"+bx.getVariantID(profile));

        String res = (String) mcc.get(cacheId);

        if (res==null) {
          res = bx.getContent(config, profile);
          mcc.add(cacheId, res, bx.getExpire());
        }

        out.append("<div class=boxlet>");
        out.append(res);
        out.append("</div>");
      } catch (Exception e) {
        logger.severe(StringUtil.getStackTrace(e));

        if (profile.getBoolean("DebugMode")) {
          out.append("<b>Ошибка получения ").append(name).append(": ").append(e.toString()).append("</b><p>").append(HTMLFormatter.nl2br(StringUtil.getStackTrace(e)));
        } else {
          out.append("<b>Ошибка</b>");
        }
      }

    }

    return out.toString();
  }

  public String getEditContent(Object config, ProfileHashtable profile, String tag) throws IOException, UtilException {
    BoxletLoader loader = new BoxletLoader();
    StringBuffer out = new StringBuffer();
    BoxletDecorator decorator = new BoxletDecorator();

    for (int i = 0; i < boxes.size(); i++) {
      String name = (String) boxes.get(i);

      try {
        out.append("<div class=boxlet>");
        Boxlet bx = loader.loadBox(name);
        out.append(decorator.getMenuContent(bx, config, profile, "edit-boxes.jsp?tag=" + URLEncoder.encode(tag) + "&mode=add&id=" + i, "edit-boxes.jsp?tag=" + URLEncoder.encode(tag) + "&mode=remove&id=" + i));
        out.append("</div>");
      } catch (Exception e) {
        if (profile.getBoolean("DebugMode")) {
          out.append("<b>Ошибка получения ").append(boxes.get(i)).append(": ").append(e.toString()).append("</b><p>").append(HTMLFormatter.nl2br(StringUtil.getStackTrace(e)));
        } else {
          out.append("<b>Ошибка</b>");
        }
      }
    }

    out.append("[<a href=\"edit-boxes.jsp?tag=").append(URLEncoder.encode(tag)).append("&mode=add\">Добавить</a>]<p>");

    return out.toString();
  }
}
