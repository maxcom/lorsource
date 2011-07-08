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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableMap;

public class CodeTag {
  private final Pattern codePattern = Pattern.compile("\\[code(=\\w+)?\\]");

  private static final ImmutableMap<String, String> brushes =
    ImmutableMap.<String, String>builder().
      put("bash", "language-bash")
      .put("shell", "language-bash")
      .put("cpp", "language-cpp")
      .put("cxx", "language-cpp")
      .put("cc", "language-cpp")
      .put("c", "language-cpp")
      .put("diff", "language-diff")
      .put("patch", "language-diff")
      .put("java", "language-java")
      .put("js", "language-javascript")
      .put("javascript", "language-javascript")
      .put("perl", "language-perl")
      .put("php", "language-php")
      .put("plain", "no-highlight")
      .put("python", "language-python")

      .put("css", "language-css")
      .put("delphi", "language-delphi")
      .put("pascal", "language-delphi")
      .put("html", "language-html")
      .put("xml", "language-xml")
      .put("lisp", "language-lisp")
      .put("scheme", "language-lisp")
      .put("ruby", "language-ruby")
      .build();

  /**
   * @return tag name
   */
  public String getTagName() {
    return "code";
  }

  /**
   * @param buffer
   */
  public void processContent(StringBuffer buffer) {
    int end = 0;

    while (true) {
      Matcher matcher = codePattern.matcher(buffer);

      if (!matcher.find(end)) {
        break;
      }

      int start = matcher.start();
      end = buffer.indexOf("[/code]", start);

      if (end < 0) {
        break;
      }

      end += "[/code]".length();

      String content = buffer.substring(matcher.end(), end - "[/code]".length());
      content = escapeHtmlBBcode(content);

      String brush = "no-highlight";

      if (matcher.group(1) != null) {
        String value = matcher.group(1).substring(1).toLowerCase();

        if (brushes.containsKey(value)) {
          brush = brushes.get(value);
        }
      }

      String replacement =
        "<div class=code><pre class=\"" + brush + "\"><code>"
          + content
          + "</code></pre></div><p>";
      buffer.replace(start, end, replacement);

      end = start + replacement.length();
    }
  }

  /**
   * @param content
   * @return -
   */
  public static String escapeHtmlBBcode(String content) {
    // escaping single characters
    content = replaceAll(content, "[]<>(){}\t\n\r".toCharArray(), new String[]{
//  :       "&#58;",
      "&#91;",
      "&#93;",
      "&lt;",
      "&gt;",
      "&#40;",
      "&#41;",
      "&#123;",
      "&#125;",
      "&nbsp; &nbsp;",
      "[code-br]",
      ""});

    return content;
  }

  public static String replaceAll(CharSequence str, char[] chars, String[] replacement) {
    StringBuilder buffer = new StringBuilder();
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      boolean matched = false;
      for (int j = 0; j < chars.length; j++) {
        if (c == chars[j]) {
          buffer.append(replacement[j]);
          matched = true;
        }
      }
      if (!matched) {
        buffer.append(c);
      }
    }
    return buffer.toString();
  }
}
