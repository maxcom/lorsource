package ru.org.linux.boxlet;

import java.io.IOException;

import ru.org.linux.util.HTMLFormatter;
import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.UtilException;

public class BoxletDecorator {
  public String getMenuContent(Boxlet bx, Object config, ProfileHashtable profile, String addUrl, String removeUrl) throws  IOException, UtilException {
    StringBuffer buf = new StringBuffer();

    try {
      buf.append(bx.getContent(config, profile));
    } catch (Exception e) {
      if (profile.getBoolean("DebugMode")) {
        buf.append("<h2>Ошибка: ").append(e.toString()).append("</h2>").append(HTMLFormatter.nl2br(StringUtil.getStackTrace(e)));
      } else {
        buf.append("<h2>Ошибка</h2>");
      }
    }

    buf.append("<p>");
    buf.append("<strong>Меню редактирования:</strong><br>");
    if (addUrl != null) {
      buf.append("* <a href=\"").append(addUrl).append("\">добавить сюда</a><br>");
    }
    if (removeUrl != null) {
      buf.append("* <a href=\"").append(removeUrl).append("\">удалить</a><br>");
    }

    return buf.toString();
  }
}
