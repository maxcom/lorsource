/*
 * Copyright 1998-2013 Linux.org.ru
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

package ru.org.linux.util;

public final class DateUtil {
  private DateUtil() {
  }

  /**
   * Returns string name of specified month number
   *
   * @param        month        1..12
   */
  public static String getMonth(int month) throws BadDateException {
    switch (month - 1) {
      case 0:
        return "Январь";
      case 1:
        return "Февраль";
      case 2:
        return "Март";
      case 3:
        return "Апрель";
      case 4:
        return "Май";
      case 5:
        return "Июнь";
      case 6:
        return "Июль";
      case 7:
        return "Август";
      case 8:
        return "Сентябрь";
      case 9:
        return "Октябрь";
      case 10:
        return "Ноябрь";
      case 11:
        return "Декабрь";
      default:
        throw new BadDateException("Указан месяц " + month);
    }
  }

}
