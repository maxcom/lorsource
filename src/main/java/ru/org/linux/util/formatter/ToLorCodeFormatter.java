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

package ru.org.linux.util.formatter;

import org.springframework.stereotype.Service;

/**
 * Формирует сообщение для сохранения в базе
 * Основная функции: экранирование тэга code и выделение цитат
 */
@Service
public class ToLorCodeFormatter {

  private static final String NL_PATTERN = "\r?\n";

  /**
   * Форматирует текст
   * @param text текст
   * @param quoting выделять ли в тексте цитаты
   * @return отфарматированный текст
   */
  public String format(String text, boolean quoting) {
    String newText = text.replaceAll("\\[(/?code)\\]", "[[$1]]");
    if(quoting) {
      return quote(newText);
    } else {
      return fixNL(newText);
    }
  }

  private String fixNL(String text) {
    return text.replaceAll(NL_PATTERN, "[br]\n");
  }

  private String quote(String text) {
    StringBuilder buf = new StringBuilder();

    boolean quot = false;

    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '\r') {
        continue;
      }
      if (text.charAt(i) == '\n' || i == 0) {
        if (quot) {
          quot = false;
          buf.append("[/i]");
        }

        if (text.substring(i).trim().startsWith(">")) {
          quot = true;
          buf.append("[i]");
        }

        if (text.charAt(i) == '\n') {
          buf.append("[br]");
        }
      }

      buf.append(text.charAt(i));
    }

    if (quot) {
      buf.append("[/i]");
    }

    return buf.toString();
  }
}
