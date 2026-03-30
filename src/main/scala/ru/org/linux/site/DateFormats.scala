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

import java.time.{Instant, ZoneId}
import java.time.format.{DateTimeFormatter, FormatStyle}
import java.time.temporal.ChronoUnit
import java.util.{Date, Locale}

object DateFormats:
  val RussianLocale = Locale.forLanguageTag("ru")
  private val Default = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm:ss z").withLocale(RussianLocale)
  private val Short = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm").withLocale(RussianLocale)
  private val Time = DateTimeFormatter.ofPattern("HH:mm").withLocale(RussianLocale)
  private val Date = DateTimeFormatter.ofPattern("dd.MM.yy").withLocale(RussianLocale)
  private val Moscow = ZoneId.of("Europe/Moscow")

  private val Iso8601: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
  private val Rfc822: DateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME
  private val DateLong: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(RussianLocale)

  def formatDefault(tz: ZoneId, date: Date) = Default.withZone(tz).format(date.toInstant)
  def formatIso8601(date: Date): String = Iso8601.withZone(Moscow).format(date.toInstant)
  def formatRfc822(date: Date): String = Rfc822.withZone(Moscow).format(date.toInstant)
  def formatDateLong(tz: ZoneId, date: Date): String = DateLong.withZone(tz).format(date.toInstant)
  def formatDateOnly(tz: ZoneId, date: Date) = Date.withZone(tz).format(date.toInstant)
  private[site] def formatTime(tz: ZoneId, date: Date) = Time.withZone(tz).format(date.toInstant)
  private[site] def formatShort(tz: ZoneId, date: Date) = Short.withZone(tz).format(date.toInstant)

  def formatInterval(date: Date, timezone: ZoneId): String =
    formatIntervalImpl(date, timezone, Instant.now)

  private[site] def formatIntervalImpl(date: Date, timezone: ZoneId, now: Instant): String =
    val diff = now.toEpochMilli - date.getTime
    val c = date.toInstant.atZone(timezone)

    val today = now.atZone(timezone).truncatedTo(ChronoUnit.DAYS);
    val yesterday = now.atZone(timezone).truncatedTo(ChronoUnit.DAYS).minusDays(1)

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

  def formatCompactInterval(date: Date, timezone: ZoneId): String =
    formatCompactIntervalImpl(date, timezone, Instant.now)

  private[site] def formatCompactIntervalImpl(date: Date, timezone: ZoneId, now: Instant): String =
    val diff = now.toEpochMilli - date.getTime
    val c = date.toInstant.atZone(timezone)

    val today = now.atZone(timezone).truncatedTo(ChronoUnit.DAYS);
    val yesterday = now.atZone(timezone).truncatedTo(ChronoUnit.DAYS).minusDays(1)

    if diff < 1000 * 60 * 60 then
      val min = Math.max(1, diff / (1000 * 60))

      s"$min&nbsp;мин"
    else if diff < 1000 * 60 * 60 * 4 || c.isAfter(today) then
      formatTime(timezone, date)
    else if c.isAfter(yesterday) then
      "вчера"
    else
      formatDateOnly(timezone, date)
