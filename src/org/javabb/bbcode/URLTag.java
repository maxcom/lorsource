package org.javabb.bbcode;

import java.sql.Connection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.org.linux.util.URLUtil;

public class URLTag extends SimpleRegexTag {
  public URLTag(String tagName, String regex, String replacement) {
    super(tagName, regex, replacement);
  }
  
  @Override
  public void substitute(Connection db, CharSequence from, StringBuffer to, RegexTag regex, String replacement) {
    to.setLength(0);

    Pattern p = regex.getRegex();
    Matcher m = p.matcher(from);
    while (m.find()) {
      String value = m.group(1);

      if (!URLUtil.isUrl(value)) {
        m.appendReplacement(to, BAD_DATA);
      } else {
        m.appendReplacement(to, replacement);
      }
    }
    m.appendTail(to);
  }
}
