/*
 * Copyright 1998-2014 Linux.org.ru
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

import com.google.common.collect.ImmutableList;

import java.util.List;

public final class ListUtil {
  public static <T> List<T> headOrEmpty(List<T> list) {
    return list.isEmpty() ? ImmutableList.<T>of() : list.subList(0, 1);
  }

  public static <T> List<T> tailOrEmpty(List<T> list) {
    return list.size() <= 1 ? ImmutableList.<T>of() : list.subList(1, list.size());
  }

  public static <T> List<T> firstHalf(List<T> list) {
    int split = list.size() / 2 + (list.size() % 2);

    return list.subList(0, split);
  }

  public static <T> List<T> secondHalf(List<T> list) {
    int split = list.size() / 2 + (list.size() % 2);

    return list.subList(split, list.size());
  }
}
