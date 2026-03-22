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
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import java.util.Date

@RunWith(classOf[JUnitRunner])
class DateFormatsSpec extends Specification {
  private val Moscow = DateTimeZone.forID("Europe/Moscow")
  private val Novosibirsk = DateTimeZone.forID("Asia/Novosibirsk")

  private def makeDate(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0, second: Int = 0): Date = {
    new DateTime(year, month, day, hour, minute, second, Moscow).toDate
  }

  "DateFormats" should {
    "formatDefault" in {
      val date = makeDate(2024, 6, 15, 14, 30, 45)
      DateFormats.formatDefault(Moscow, date) must equalTo("15.06.24 14:30:45 MSK")
    }

    "formatDefault Novosibirsk" in {
      val date = makeDate(2024, 6, 15, 14, 30, 45)
      DateFormats.formatDefault(Novosibirsk, date) must equalTo("15.06.24 18:30:45 GMT+07:00")
    }

    "formatIso8601" in {
      val date = makeDate(2024, 6, 15, 14, 30, 45)
      DateFormats.formatIso8601(date) must equalTo("2024-06-15T14:30:45.000+03:00")
    }

    "formatRfc822" in {
      val date = makeDate(2024, 6, 15, 14, 30, 45)
      DateFormats.formatRfc822(date) must equalTo("Sat, 15 Jun 2024 14:30:45 +0300")
    }

    "formatDateLong" in {
      val date = makeDate(2024, 6, 15, 14, 30, 45)
      DateFormats.formatDateLong(Moscow, date) must equalTo("15 июня 2024\u202fг.")
    }

    "formatDateOnly" in {
      val date = makeDate(2024, 6, 15, 14, 30, 45)
      DateFormats.formatDateOnly(Moscow, date) must equalTo("15.06.24")
    }

    "formatTime" in {
      val date = makeDate(2024, 6, 15, 14, 30, 45)
      DateFormats.formatTime(Moscow, date) must equalTo("14:30")
    }

    "formatShort" in {
      val date = makeDate(2024, 6, 15, 14, 30, 45)
      DateFormats.formatShort(Moscow, date) must equalTo("15.06.24 14:30")
    }

    "formatIntervalImpl less than minute" in {
      val now = makeDate(2024, 6, 15, 14, 0, 0)
      val date = makeDate(2024, 6, 15, 13, 59, 30)
      DateFormats.formatIntervalImpl(date, Moscow, new DateTime(now.getTime, Moscow)) must equalTo("минуту назад")
    }

    "formatIntervalImpl few minutes" in {
      val now = makeDate(2024, 6, 15, 14, 0, 0)
      val date = makeDate(2024, 6, 15, 13, 55, 0)
      DateFormats.formatIntervalImpl(date, Moscow, new DateTime(now.getTime, Moscow)) must equalTo("5&nbsp;минут назад")
    }

    "formatIntervalImpl today" in {
      val now = new DateTime(2024, 6, 15, 12, 0, 0, Moscow)
      val date = makeDate(2024, 6, 15, 10, 30, 0)
      DateFormats.formatIntervalImpl(date, Moscow, now) must equalTo("сегодня 10:30")
    }

    "formatIntervalImpl yesterday" in {
      val now = new DateTime(2024, 6, 15, 12, 0, 0, Moscow)
      val date = makeDate(2024, 6, 14, 10, 30, 0)
      DateFormats.formatIntervalImpl(date, Moscow, now) must equalTo("вчера 10:30")
    }

    "formatIntervalImpl older" in {
      val now = new DateTime(2024, 6, 15, 12, 0, 0, Moscow)
      val date = makeDate(2024, 6, 10, 10, 30, 0)
      DateFormats.formatIntervalImpl(date, Moscow, now) must equalTo("10.06.24 10:30")
    }

    "formatCompactIntervalImpl less than hour" in {
      val now = makeDate(2024, 6, 15, 14, 0, 0)
      val date = makeDate(2024, 6, 15, 13, 30, 0)
      DateFormats.formatCompactIntervalImpl(date, Moscow, new DateTime(now.getTime, Moscow)) must equalTo("30&nbsp;мин")
    }

    "formatCompactIntervalImpl few hours" in {
      val now = makeDate(2024, 6, 15, 14, 0, 0)
      val date = makeDate(2024, 6, 15, 11, 30, 0)
      DateFormats.formatCompactIntervalImpl(date, Moscow, new DateTime(now.getTime, Moscow)) must equalTo("11:30")
    }

    "formatCompactIntervalImpl yesterday" in {
      val now = new DateTime(2024, 6, 15, 12, 0, 0, Moscow)
      val date = makeDate(2024, 6, 14, 10, 30, 0)
      DateFormats.formatCompactIntervalImpl(date, Moscow, now) must equalTo("вчера")
    }

    "formatCompactIntervalImpl older" in {
      val now = new DateTime(2024, 6, 15, 12, 0, 0, Moscow)
      val date = makeDate(2024, 6, 10, 10, 30, 0)
      DateFormats.formatCompactIntervalImpl(date, Moscow, now) must equalTo("10.06.24")
    }
  }
}
