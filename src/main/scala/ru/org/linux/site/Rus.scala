/*
 * Copyright 1998-2025 Linux.org.ru
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
package ru.org.linux.site

import org.joda.time.{DateTime, DateTimeZone, ReadableInstant}
import org.joda.time.format.DateTimeFormatter

class Rus(dateTimeFormatter: DateTimeFormatter, timezone: DateTimeZone) {
  def withZone(tz: DateTimeZone): Rus = new Rus(dateTimeFormatter.withZone(tz), tz)

  def print(instant: Long): String = print(new DateTime(instant))

  def print(instant: ReadableInstant): String = {
    val tokens = dateTimeFormatter.print(instant).split(" ")

    val time = tokens.lift(1).getOrElse("")

    val danas = DateTime.now.withZone(timezone).withTimeAtStartOfDay
    val jucer = DateTime.now.withZone(timezone).minusDays(1).withTimeAtStartOfDay
    val onomdanu = DateTime.now.withZone(timezone).minusDays(5).withTimeAtStartOfDay

    if (instant.isAfter(danas)) {
      "днесь " + time
    } else if (instant.isAfter(jucer)) {
      "давеча " + time
    } else if (instant.isAfter(onomdanu)) {
      "ономнясь " + time
    } else {
      "давным-давно " + time
    }
  }
}
