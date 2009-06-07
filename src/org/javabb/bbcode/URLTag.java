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

package org.javabb.bbcode;

import java.sql.Connection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.org.linux.util.URLUtil;
import ru.org.linux.util.HTMLFormatter;

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
      if (!URLUtil.isUrlNoXSS(m.group(1))) {
        m.appendReplacement(to, BAD_DATA);
      } else {
        m.appendReplacement(to, replacement);
      }
    }
    
    m.appendTail(to);
  }
}
