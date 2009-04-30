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

package ru.org.linux.site.boxes;

import ru.org.linux.boxlet.Boxlet;
import ru.org.linux.util.ProfileHashtable;

public class ibm extends Boxlet {
  @Override
  public String getContentImpl(ProfileHashtable profile) throws Exception {
    return "<h2>Новые материалы на IBM developerWorks</h2>\n" +
        "  <div class=\"boxlet_content\">"+
        "  <iframe src=\"dw.jsp?height=400&amp;width=219&amp;main=1\" width=\"222\" height=\"400\" scrolling=\"no\" frameborder=\"0\"></iframe>\n" +
        "  <br>&nbsp;<br>\n" +
      '\n' +
        "  Профессиональный ресурс от IBM для специалистов в области разработки ПО. Рассылка выходит 1 раз в неделю.\n" +
        "  <form id=\"data1\" method=\"post\" enctype=\"multipart/form-data\" action=\"http://www-931.ibm.com/bin/subscriptions/esi/subscribe/RURU/10209/\">\n" +
        "                       e-mail:<br>\n" +
        "  <input type=\"text\" size=\"15\" name=\"email\" style=\"width: 90%\" value=\"\">\n" +
        "  <br>\n" +
        "  <input alt=\"subscribe\" type=\"submit\" name=\"butSubmit1\" value=\"Подписаться\">\n" +
        "  </form></div>";
  }

  @Override
  public String getInfo() {
    return "Новые материалы на IBM developerWorks";
  }
}
