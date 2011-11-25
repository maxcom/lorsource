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
import ru.org.linux.util.StringUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Формирует сообщение с TeX переносами для сохранения в базе
 * Основная функции: экранирование тэга code и выделение цитат
 */
@Service
public class ToLorCodeTexFormatter {

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
      return newText;
    }
  }

  public static final Pattern QUOTE_PATTERN = Pattern.compile("^(\\>+)");

  protected String quote(String text) {
    StringBuilder buf = new StringBuilder();
    String[] lines = text.split("(\\r?\\n)");
    int globalNestingLevel = 0;
    int currentLine = 0;

    for(String line : lines) {
      currentLine = currentLine + 1;
      if(line.isEmpty()) {
        if(globalNestingLevel == 0) {
          buf.append('\n');
        }
        continue;
      }
      Matcher m = QUOTE_PATTERN.matcher(line);
      if(m.find()) {
        int nestingLevel = m.group(1).length();
        if(globalNestingLevel == 0) {
          buf.append(StringUtil.repeat("[quote]", nestingLevel));
          globalNestingLevel = nestingLevel;
        } else if(nestingLevel < globalNestingLevel) {
          buf.append(StringUtil.repeat("[/quote]", globalNestingLevel - nestingLevel));
          globalNestingLevel = nestingLevel;
        }
        buf.append(line.substring(nestingLevel));
        buf.append("[br]");
      } else {
        if(globalNestingLevel > 0) {
          buf.append(StringUtil.repeat("[/quote]", globalNestingLevel));
          globalNestingLevel = 0;
        }
        buf.append(line);
        buf.append('\n');
      }
    }
    if(globalNestingLevel > 0) {
      buf.append(StringUtil.repeat("[/quote]", globalNestingLevel));
    }
    return buf.toString();
  }


}
