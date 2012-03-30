/*
 * Copyright 1998-2012 Linux.org.ru
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

package ru.org.linux.site.tags;

import java.util.List;

public class Collection {
  /**
   * JSTL-помощник для поиска значения в массиве.
   *
   * @param list    массив
   * @param object  искомое значение
   * @return true если значение присутствует в массиве
   */
  public static boolean arrayContains(List list, java.lang.Object object) {
    return list.contains(object);
  }
}
