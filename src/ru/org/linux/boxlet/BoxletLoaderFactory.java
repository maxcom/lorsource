package ru.org.linux.boxlet;

import java.io.IOException;
import java.util.Properties;

import ru.org.linux.util.HTMLFormatter;
import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.UtilException;

public class BoxletLoaderFactory extends BoxletFactory {
  public BoxletLoaderFactory(Object Config) {
    super(Config);
  }

  private Boxlet loadBox(String name) throws BoxletException {
    try {
      Class BoxletFactory = Class.forName("ru.org.linux.site.boxes." + name);
      return (Boxlet) BoxletFactory.newInstance();
    } catch (Exception e) {
      throw new BoxletLoadException(e);
    }
  }

  public String getContent(String name, ProfileHashtable profile) throws BoxletException, IOException {
    Boxlet bx = loadBox(name);

    StringBuffer buf = new StringBuffer();

    try {
      buf.append(bx.getContent(config, profile));
    } catch (Exception e) {
/*			if (profile.getBooleanProperty("DebugMode"))
				buf.append("<h2>Ошибка: "+e.toString()+"</h2>"+HTMLFormatter.nl2br(StringUtil.getStackTrace(e)));
			else
				buf.append("<h2>Ошибка</h2>");
*/
      throw new BoxletLoadException(e);
    }

    return buf.toString();
  }

  public String getMenuContent(String name, ProfileHashtable profile, String addUrl, String removeUrl) throws BoxletException, IOException, UtilException {
    Boxlet bx = loadBox(name);

    StringBuffer buf = new StringBuffer();

    try {
      buf.append(bx.getContent(config, profile));
    } catch (Exception e) {
      if (profile.getBooleanProperty("DebugMode")) {
        buf.append("<h2>Ошибка: " + e.toString() + "</h2>" + HTMLFormatter.nl2br(StringUtil.getStackTrace(e)));
      } else {
        buf.append("<h2>Ошибка</h2>");
      }
    }

    buf.append("<p>");
    buf.append("<strong>Меню редактирования:</strong><br>");
    if (addUrl != null) {
      buf.append("* <a href=\"" + addUrl + "\">добавить сюда</a><br>");
    }
    if (removeUrl != null) {
      buf.append("* <a href=\"" + removeUrl + "\">удалить</a><br>");
    }

    return buf.toString();
  }


  String getVariantID(String name, ProfileHashtable profile, Properties request) throws BoxletException, UtilException {
    Boxlet bx = loadBox(name);

    return bx.getVariantID(profile, request);
  }

  long getVersionID(String name, ProfileHashtable profile, Properties request) throws BoxletException {
    Boxlet bx = loadBox(name);

    return bx.getVersionID(profile, request);
  }

}
