package ru.org.linux.util.bbcode;

import ru.org.linux.site.User;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: hizel
 * Date: 8/24/11
 * Time: 7:12 PM
 */
public class ParserResult {
  private final String html;
  private final Set<User> replier;

  public ParserResult(String html, Set<User> replier) {
    this.html = html;
    this.replier = replier;
  }

  public String getHtml() {
    return html;
  }

  public Set<User> getReplier() {
    return replier;
  }
}
