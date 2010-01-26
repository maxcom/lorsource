/*
 * Copyright 1998-2010 Linux.org.ru
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

package org.javabb.bbcode;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.org.linux.site.User;
import ru.org.linux.site.UserNotFoundException;
import ru.org.linux.util.StringUtil;

public class UserTag extends SimpleRegexTag {
  private static final String USER_VIEW = "<span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><a style=\"text-decoration: none\" href='/people/$1/profile'>$1</a></span>";
  private static final String BLOCKED_VIEW = "<span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><s><a style=\"text-decoration: none\" href='/people/$1/profile'>$1</a></s></span>";

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