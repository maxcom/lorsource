package org.javabb.bbcode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.org.linux.util.StringUtil;

public class UserTag extends SimpleRegexTag {
  public UserTag(String tagName, String regex, String replacement) {
    super(tagName, regex, replacement);
  }

  @Override
  public void substitute(CharSequence from, StringBuffer to, RegexTag regex, String replacement) {
    to.setLength(0);

    Pattern p = regex.getRegex();
    Matcher m = p.matcher(from);
    while (m.find()) {
      String value = m.group(1);

      if (!StringUtil.checkLoginName(value)) {
        m.appendReplacement(to, BAD_DATA);
      } else {
        m.appendReplacement(to, replacement);
      }
    }
    m.appendTail(to);
  }

}