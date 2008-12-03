package ru.org.linux.site.boxes;

import ru.org.linux.boxlet.Boxlet;
import ru.org.linux.util.ProfileHashtable;

public class ibm extends Boxlet {
  public String getContentImpl(ProfileHashtable profile) throws Exception {
    return "<h2>Новые материалы на IBM developerWorks</h2>\n" +
        "  <iframe src=\"dw.jsp?height=400&amp;width=235&amp;main=1\" width=\"238\" height=\"400\" scrolling=\"no\" frameborder=\"0\"></iframe>\n" +
        "  <br>&nbsp;<br>\n" +
        "\n" +
        "  Профессиональный ресурс от IBM для специалистов в области разработки ПО. Рассылка выходит 1 раз в неделю.\n" +
        "  <form id=\"data1\" method=\"post\" enctype=\"multipart/form-data\" action=\"http://www-931.ibm.com/bin/subscriptions/esi/subscribe/RURU/10209/\">\n" +
        "                       e-mail:<br>\n" +
        "  <input type=\"text\" size=\"15\" name=\"email\" style=\"width: 90%\" value=\"\">\n" +
        "  <br>\n" +
        "  <input alt=\"subscribe\" type=\"submit\" name=\"butSubmit1\" value=\"Подписаться\">\n" +
        "  </form>";
  }

  public String getInfo() {
    return "Новые материалы на IBM developerWorks";
  }
}
