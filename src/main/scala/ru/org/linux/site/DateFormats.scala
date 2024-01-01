/*
 * Copyright 1998-2024 Linux.org.ru
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

import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat

import java.util.{Date, Locale}

object DateFormats {
  private val RussianLocale = new Locale("ru")
  private val Default = DateTimeFormat.forPattern("dd.MM.yy HH:mm:ss z").withLocale(RussianLocale)
  private val Short = DateTimeFormat.forPattern("dd.MM.yy HH:mm").withLocale(RussianLocale)
  private val Time = DateTimeFormat.forPattern("HH:mm").withLocale(RussianLocale)
  private val Date = DateTimeFormat.forPattern("dd.MM.yy").withLocale(RussianLocale)

  val Iso8601: DateTimeFormatter = ISODateTimeFormat.dateTime
  val Rfc822: DateTimeFormatter = DateTimeFormat.forPattern("EEE, d MMM yyyy HH:mm:ss Z").withLocale(Locale.US)
  val DateLong: DateTimeFormatter = DateTimeFormat.longDate().withLocale(RussianLocale);

  def getDefault(tz: DateTimeZone): DateTimeFormatter = Default.withZone(tz)
  def dateLong(tz: DateTimeZone): DateTimeFormatter = DateLong.withZone(tz)

  private def short(tz: DateTimeZone): DateTimeFormatter = Short.withZone(tz)
  private def time(tz: DateTimeZone): DateTimeFormatter = Time.withZone(tz)
  def date(tz: DateTimeZone): DateTimeFormatter = Date.withZone(tz)

  def formatInterval(date: Date, timezone: DateTimeZone): String = {
    val diff = System.currentTimeMillis - date.getTime
    val c = new DateTime(date.getTime)

    val today = DateTime.now.withZone(timezone).withTimeAtStartOfDay
    val yesterday = DateTime.now.withZone(timezone).minusDays(1).withTimeAtStartOfDay

    if (diff < 2 * 1000 * 60) {
      "минуту назад"
    } else if (diff < 1000 * 60 * 60) {
      val min = diff / (1000 * 60)

      if (min % 10 < 5 && min % 10 > 1 && (min > 20 || min < 10)) {
        min + "&nbsp;минуты назад"
      } else if (min % 10 == 1 && min > 20) {
        min + "&nbsp;минута назад"
      } else {
        min + "&nbsp;минут назад"
      }
    } else if (c.isAfter(today)) {
      "сегодня " + time(timezone).print(c)
    } else if (c.isAfter(yesterday)) {
      "вчера " + time(timezone).print(c)
    } else {
      short(timezone).print(c)
    }
  }

  def formatCompactInterval(date: Date, timezone: DateTimeZone): String = {
    val diff = System.currentTimeMillis - date.getTime
    val c = new DateTime(date.getTime)

    val today = DateTime.now.withZone(timezone).withTimeAtStartOfDay
    val yesterday = DateTime.now.withZone(timezone).minusDays(1).withTimeAtStartOfDay

    if (diff < 1000 * 60 * 60) {
      val min = Math.max(1, diff / (1000 * 60))

      min + "&nbsp;мин"
    } else if (diff < 1000 * 60 * 60 * 4 || c.isAfter(today)) {
      time(timezone).print(c)
    } else if (c.isAfter(yesterday)) {
      "вчера"
    } else {
      DateFormats.date(timezone).print(c)
    }
  }
}
