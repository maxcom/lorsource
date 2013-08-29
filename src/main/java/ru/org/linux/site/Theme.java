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

package ru.org.linux.site;

import com.google.common.collect.ImmutableList;

public class Theme {
  private static final Theme BLACK = new Theme("black", "black/head.jsp", "black/head-main.jsp");
  private static final Theme WHITE2 = new Theme("white2", "white2/head-main.jsp", "white2/head-main.jsp");
  private static final Theme TANGO = new Theme("tango", "tango/head-main.jsp", "tango/head-main.jsp");
  private static final Theme WALTZ = new Theme("waltz", "waltz/head-main.jsp", "waltz/head-main.jsp");

  public static final ImmutableList<Theme> THEMES = ImmutableList.of(
          TANGO,
          BLACK,
          WHITE2,
          WALTZ
  );

  private final String id;
  private final String head;
  private final String headMain;

  public Theme(String id, String head, String headMain) {
    this.id = id;
    this.head = head;
    this.headMain = headMain;
  }

  public String getId() {
    return id;
  }

  public String getHead() {
    return head;
  }

  public String getHeadMain() {
    return headMain;
  }
}