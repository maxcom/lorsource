/*
 * Copyright 1998-2026 Linux.org.ru
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

package ru.org.linux.search

object SearchEnums:
  enum SearchRange(val param: String, val title: String):
    case ALL extends SearchRange(null, "темы и комментарии")
    case TOPICS extends SearchRange("false", "только темы")
    case COMMENTS extends SearchRange("true", "только комментарии")

    def getValue: String = param
    def getColumn: String = "is_comment"
    def getTitle: String = title

  enum SearchInterval(val range: String, val title: String):
    case MONTH extends SearchInterval("now/h-1M", "месяц")
    case THREE_MONTH extends SearchInterval("now/d-3M", "три месяца")
    case YEAR extends SearchInterval("now/d-1y", "год")
    case THREE_YEAR extends SearchInterval("now/w-3y", "три года")
    case ALL extends SearchInterval(null, "весь период")

    def getRange: String = range
    def getTitle: String = title
    def getColumn: String = "postdate"
