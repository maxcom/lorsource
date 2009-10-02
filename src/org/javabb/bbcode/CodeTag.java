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

/**
 * @author
 * @since 18/01/2005
 */
public class CodeTag {
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
    int start = buffer.indexOf("[code]");
    int end;

    for (; (start < buffer.length()) && (start != -1); start = buffer.indexOf("[code]", end)) {
      end = buffer.indexOf("[/code]", start);

      if (end < 0) {
        break;
      }

      end += "[/code]".length();

      String content = buffer.substring(start + "[code]".length(), end - "[/code]".length());
      content = escapeHtmlBBcode(content);

      String replacement =
          "<pre class=code><p>"
              + content
              + "</pre>";
      buffer.replace(start, end, replacement);

      end = start + replacement.length();
    }
  }

  /**
   * @param content
   * @return -
   */
  private static String escapeHtmlBBcode(String content) {
    // escaping single characters
    content = replaceAll(content, ":[]<>(){}\t".toCharArray(), new String[]{
        "&#58;",
        "&#91;",
        "&#93;",
        "&lt;",
        "&gt;",
        "&#40;",
        "&#41;",
        "&#123;",
        "&#125;",
        "&nbsp; &nbsp;"});

    return content;
  }

  private static String replaceAll(CharSequence str, char[] chars, String[] replacement) {
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
