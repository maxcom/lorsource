/*
 * Copyright 1998-2009 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
