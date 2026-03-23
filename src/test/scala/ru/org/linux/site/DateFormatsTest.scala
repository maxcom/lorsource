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
import munit.FunSuite

import java.util.Date

class DateFormatsTest extends FunSuite {
  private val Moscow = DateTimeZone.forID("Europe/Moscow")
  private val Novosibirsk = DateTimeZone.forID("Asia/Novosibirsk")

  private def makeDate(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0, second: Int = 0): Date = {
    new DateTime(year, month, day, hour, minute, second, Moscow).toDate
  }

  test("formatDefault") {
    val date = makeDate(2024, 6, 15, 14, 30, 45)
    assertEquals(DateFormats.formatDefault(Moscow, date), "15.06.24 14:30:45 MSK")
  }

  test("formatDefault Novosibirsk") {
    val date = makeDate(2024, 6, 15, 14, 30, 45)
    assertEquals(DateFormats.formatDefault(Novosibirsk, date), "15.06.24 18:30:45 GMT+07:00")
  }

  test("formatIso8601") {
    val date = makeDate(2024, 6, 15, 14, 30, 45)
    assertEquals(DateFormats.formatIso8601(date), "2024-06-15T14:30:45.000+03:00")
  }

  test("formatRfc822") {
    val date = makeDate(2024, 6, 15, 14, 30, 45)
    assertEquals(DateFormats.formatRfc822(date), "Sat, 15 Jun 2024 14:30:45 +0300")
  }

  test("formatDateLong") {
    val date = makeDate(2024, 6, 15, 14, 30, 45)
    assertEquals(DateFormats.formatDateLong(Moscow, date), "15 июня 2024\u202fг.")
  }

  test("formatDateOnly") {
    val date = makeDate(2024, 6, 15, 14, 30, 45)
    assertEquals(DateFormats.formatDateOnly(Moscow, date), "15.06.24")
  }

  test("formatTime") {
    val date = makeDate(2024, 6, 15, 14, 30, 45)
    assertEquals(DateFormats.formatTime(Moscow, date), "14:30")
  }

  test("formatShort") {
    val date = makeDate(2024, 6, 15, 14, 30, 45)
    assertEquals(DateFormats.formatShort(Moscow, date), "15.06.24 14:30")
  }

  test("formatIntervalImpl less than minute") {
    val now = makeDate(2024, 6, 15, 14, 0, 0)
    val date = makeDate(2024, 6, 15, 13, 59, 30)
    assertEquals(DateFormats.formatIntervalImpl(date, Moscow, new DateTime(now.getTime, Moscow)), "минуту назад")
  }

  test("formatIntervalImpl few minutes") {
    val now = makeDate(2024, 6, 15, 14, 0, 0)
    val date = makeDate(2024, 6, 15, 13, 55, 0)
    assertEquals(DateFormats.formatIntervalImpl(date, Moscow, new DateTime(now.getTime, Moscow)), "5&nbsp;минут назад")
  }

  test("formatIntervalImpl today") {
    val now = new DateTime(2024, 6, 15, 12, 0, 0, Moscow)
    val date = makeDate(2024, 6, 15, 10, 30, 0)
    assertEquals(DateFormats.formatIntervalImpl(date, Moscow, now), "сегодня 10:30")
  }

  test("formatIntervalImpl yesterday") {
    val now = new DateTime(2024, 6, 15, 12, 0, 0, Moscow)
    val date = makeDate(2024, 6, 14, 10, 30, 0)
    assertEquals(DateFormats.formatIntervalImpl(date, Moscow, now), "вчера 10:30")
  }

  test("formatIntervalImpl older") {
    val now = new DateTime(2024, 6, 15, 12, 0, 0, Moscow)
    val date = makeDate(2024, 6, 10, 10, 30, 0)
    assertEquals(DateFormats.formatIntervalImpl(date, Moscow, now), "10.06.24 10:30")
  }

  test("formatCompactIntervalImpl less than hour") {
    val now = makeDate(2024, 6, 15, 14, 0, 0)
    val date = makeDate(2024, 6, 15, 13, 30, 0)
    assertEquals(DateFormats.formatCompactIntervalImpl(date, Moscow, new DateTime(now.getTime, Moscow)), "30&nbsp;мин")
  }

  test("formatCompactIntervalImpl few hours") {
    val now = makeDate(2024, 6, 15, 14, 0, 0)
    val date = makeDate(2024, 6, 15, 11, 30, 0)
    assertEquals(DateFormats.formatCompactIntervalImpl(date, Moscow, new DateTime(now.getTime, Moscow)), "11:30")
  }

  test("formatCompactIntervalImpl yesterday") {
    val now = new DateTime(2024, 6, 15, 12, 0, 0, Moscow)
    val date = makeDate(2024, 6, 14, 10, 30, 0)
    assertEquals(DateFormats.formatCompactIntervalImpl(date, Moscow, now), "вчера")
  }

  test("formatCompactIntervalImpl older") {
    val now = new DateTime(2024, 6, 15, 12, 0, 0, Moscow)
    val date = makeDate(2024, 6, 10, 10, 30, 0)
    assertEquals(DateFormats.formatCompactIntervalImpl(date, Moscow, now), "10.06.24")
  }
}