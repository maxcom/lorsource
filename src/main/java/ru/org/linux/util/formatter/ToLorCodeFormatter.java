/*
 * Copyright 1998-2015 Linux.org.ru
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
  /**
   * Форматирует текст
   * @param text текст
   * @return отфарматированный текст
   */
  public String format(String text) {
    return ToLorCodeTexFormatter.quote(text, "[br]");
  }
}
