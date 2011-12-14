/*
 * Copyright 1998-2011 Linux.org.ru
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
package ru.org.linux.admin.ipmanage;

import java.util.Comparator;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Перечисление, описывающее возможные варианты периодов бана IP-адреса.
 */
public enum BanPeriodEnum {
  HOUR_1("1  час", Calendar.HOUR_OF_DAY, 1),
  DAY_1("1 день", Calendar.DAY_OF_MONTH, 1),
  MONTH_1("1 месяц", Calendar.MONTH, 1),
  MONTH_3("3 месяца", Calendar.MONTH, 3),
  MONTH_6("6 месяцев", Calendar.MONTH, 6),
  PERMANENT("постоянно", 0, 0),
  REMOVE("не блокировать", 0, 0),
  CUSTOM("указать (дней)", 0, 0);

  private final String description;
  private final int calendarPeriod;
  private final int calendarNumPeriods;

  BanPeriodEnum(String description, int calendarPeriod, int calendarNumPeriods) {
    this.description = description;
    this.calendarPeriod = calendarPeriod;
    this.calendarNumPeriods = calendarNumPeriods;
  }

  public int getCalendarPeriod() {
    return calendarPeriod;
  }

  public int getCalendarNumPeriods() {
    return calendarNumPeriods;
  }

  /**
   * Получает карту описаний значений перечисления.
   *
   * @return карта описаний значений в виде {значение перечисления, описание перечистения}.
   */
  public static Map<String, String> getDescriptions() {
    Map<String, String> descriptions = new HashMap<String, String>();
    for (BanPeriodEnum banPeriod : values()) {
      descriptions.put(banPeriod.toString(), banPeriod.description);
    }
    Map<String, String> descriptionsSorted = new TreeMap<String, String>(new ValueComparer(descriptions));
    descriptionsSorted.putAll(descriptions);
    return descriptionsSorted;
  }

  /**
   * Класс-сравнитель для сортировки карты по значению.
   */
  private static class ValueComparer implements Comparator {
    private Map data = null;

    public ValueComparer(Map data) {
      super();
      this.data = data;
    }

    public int compare(Object o1, Object o2) {
      String e1 = (String) data.get(o1);
      String e2 = (String) data.get(o2);
      return e1.compareTo(e2);
    }
  }
}
