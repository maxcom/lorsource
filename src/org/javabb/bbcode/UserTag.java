package org.javabb.bbcode;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.org.linux.site.User;
import ru.org.linux.site.UserNotFoundException;
import ru.org.linux.util.StringUtil;

public class UserTag extends SimpleRegexTag {
  private static final String USER_VIEW = "<img src=\"/favicon.ico\"><a style=\"text-decoration: none\" href='/whois.jsp?nick=$1'>$1</a>";
  private static final String BLOCKED_VIEW = "<img src=\"/favicon.ico\"><s><a style=\"text-decoration: none\" href='/whois.jsp?nick=$1'>$1</a></s>";

  public UserTag(String tagName, String regex) {
    super(tagName, regex, USER_VIEW);
  }

  @Override
  public void substitute(Connection db, CharSequence from, StringBuffer to, RegexTag regex, String replacement) throws SQLException {
    to.setLength(0);

    Pattern p = regex.getRegex();
    Matcher m = p.matcher(from);
    while (m.find()) {
      String value = m.group(1);

      if (!StringUtil.checkLoginName(value)) {
        m.appendReplacement(to, BAD_DATA);
      } else {
        try {
          User user = User.getUser(db, value);

          if (!user.isBlocked()) {
            m.appendReplacement(to, replacement);
          } else {
            m.appendReplacement(to, BLOCKED_VIEW);            
          }
        } catch (UserNotFoundException ex) {
          m.appendReplacement(to, BAD_DATA);
        }
      }
    }
    m.appendTail(to);
  }
}