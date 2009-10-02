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

/*
 * Copyright 2004 JavaFree.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.javabb.bbcode;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleRegexTag implements RegexTag {
  private final String _tagName;
  private Pattern _regex;
  private String _replacement;

  public static final String BAD_DATA = "<s>$1</s>";

  public SimpleRegexTag(String tagName, String regex, String replacement) {
    _tagName = tagName;
    _regex = Pattern.compile(regex);
    _replacement = replacement;
  }

  @Override
  public String getTagName() {
    return _tagName;
  }

  @Override
  public Pattern getRegex() {
    return _regex;
  }

  @Override
  public String getReplacement() {
    return _replacement;
  }

  public void setRegex(String regex) {
    _regex = Pattern.compile(regex);
  }

  public void setReplacement(String replacement) {
    _replacement = replacement;
  }

  @Override
  public void substitute(Connection db, CharSequence from, StringBuffer to, RegexTag regex, String replacement) throws SQLException {
    to.setLength(0);

    Pattern p = regex.getRegex();
    Matcher m = p.matcher(from);
    while (m.find()) {
      m.appendReplacement(to, replacement);
    }
    m.appendTail(to);
  }
}
