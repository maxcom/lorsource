/*
 * Copyright 1998-2016 Linux.org.ru
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

package ru.org.linux.search;

public class SearchEnums {
  public enum SearchRange {
    ALL(null, "темы и комментарии"),
    TOPICS("false", "только темы"),
    COMMENTS("true", "только комментарии");

    private final String param;
    private final String title;

    SearchRange(String param, String title) {
      this.param = param;
      this.title = title;
    }

    public String getValue() {
      return param;
    }

    public String getColumn() {
      return "is_comment";
    }

    public String getTitle() {
      return title;
    }
  }

  public enum SearchInterval {
    MONTH("now/h-1M", "месяц"),
    THREE_MONTH("now/d-3M", "три месяца"),
    YEAR("now/d-1y", "год"),
    THREE_YEAR("now/w-3y", "три года"),
    ALL(null, "весь период");

    private final String range;
    private final String title;

    SearchInterval(String range, String title) {
      this.range = range;
      this.title = title;
    }

    public String getRange() {
      return range;
    }

    public String getTitle() {
      return title;
    }

    public String getColumn() {
      return "postdate";
    }
  }
}
