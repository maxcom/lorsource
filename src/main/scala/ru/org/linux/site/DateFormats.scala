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
package ru.org.linux.site

import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat

import java.util.{Date, Locale}

object DateFormats:
  private val RussianLocale = Locale.forLanguageTag("ru")
  private val Default = DateTimeFormat.forPattern("dd.MM.yy HH:mm:ss z").withLocale(RussianLocale)
  private val Short = DateTimeFormat.forPattern("dd.MM.yy HH:mm").withLocale(RussianLocale)
  private val Time = DateTimeFormat.forPattern("HH:mm").withLocale(RussianLocale)
  private val Date = DateTimeFormat.forPattern("dd.MM.yy").withLocale(RussianLocale)

  private val Iso8601: DateTimeFormatter = ISODateTimeFormat.dateTime
  private val Rfc822: DateTimeFormatter = DateTimeFormat.forPattern("EEE, d MMM yyyy HH:mm:ss Z").withLocale(Locale.US)
  private val DateLong: DateTimeFormatter = DateTimeFormat.longDate().withLocale(RussianLocale)

  def formatDefault(tz: DateTimeZone, date: Date) = Default.withZone(tz).print(date.getTime)
  def formatIso8601(date: Date): String = Iso8601.print(date.getTime)
  def formatRfc822(date: Date): String = Rfc822.print(date.getTime)
  def formatDateLong(tz: DateTimeZone, date: Date): String = DateLong.withZone(tz).print(date.getTime)
  def formatDateOnly(tz: DateTimeZone, date: Date) = Date.withZone(tz).print(date.getTime)
  private[site] def formatTime(tz: DateTimeZone, date: Date) = Time.withZone(tz).print(date.getTime)
  private[site] def formatShort(tz: DateTimeZone, date: Date) = Short.withZone(tz).print(date.getTime)

  def formatInterval(date: Date, timezone: DateTimeZone): String =
    formatIntervalImpl(date, timezone, DateTime.now)

  private[site] def formatIntervalImpl(date: Date, timezone: DateTimeZone, now: DateTime): String =
    val diff = now.getMillis - date.getTime
    val c = new DateTime(date.getTime)

    val today = now.withZone(timezone).withTimeAtStartOfDay
    val yesterday = now.withZone(timezone).minusDays(1).withTimeAtStartOfDay

    if diff < 2 * 1000 * 60 then
      "минуту назад"
    else if diff < 1000 * 60 * 60 then
      val min = diff / (1000 * 60)

      if min % 10 < 5 && min % 10 > 1 && (min > 20 || min < 10) then
        s"$min&nbsp;минуты назад"
      else if min % 10 == 1 && min > 20 then
        s"$min&nbsp;минута назад"
      else
        s"$min&nbsp;минут назад"
    else if c.isAfter(today) then
      "сегодня " + formatTime(timezone, date)
    else if c.isAfter(yesterday) then
      "вчера " + formatTime(timezone, date)
    else
      formatShort(timezone, date)

  def formatCompactInterval(date: Date, timezone: DateTimeZone): String =
    formatCompactIntervalImpl(date, timezone, DateTime.now)

  private[site] def formatCompactIntervalImpl(date: Date, timezone: DateTimeZone, now: DateTime): String =
    val diff = now.getMillis - date.getTime
    val c = new DateTime(date.getTime)

    val today = now.withZone(timezone).withTimeAtStartOfDay
    val yesterday = now.withZone(timezone).minusDays(1).withTimeAtStartOfDay

    if diff < 1000 * 60 * 60 then
      val min = Math.max(1, diff / (1000 * 60))

      s"$min&nbsp;мин"
    else if diff < 1000 * 60 * 60 * 4 || c.isAfter(today) then
      formatTime(timezone, date)
    else if c.isAfter(yesterday) then
      "вчера"
    else
      formatDateOnly(timezone, date)
