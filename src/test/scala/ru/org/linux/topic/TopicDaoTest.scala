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
package ru.org.linux.topic

import munit.FunSuite

import java.time.{OffsetDateTime, ZoneId, ZoneOffset}

class TopicDaoTest extends FunSuite:
  private val Moscow = ZoneId.of("Europe/Moscow")
  private val Santiago = ZoneId.of("America/Santiago")

  test("month interval end accounts for DST change inside the month"):
    // On 26.10.2014 Moscow went from UTC+4 to UTC+3; a plusMonths(1) on the start keeps the +04:00 offset and
    // leaves the last hour of October outside both October and November
    val (start, end) = TopicDao.monthInterval(2014, 10, Moscow)

    assertEquals(start, OffsetDateTime.of(2014, 10, 1, 0, 0, 0, 0, ZoneOffset.ofHours(4)))
    assertEquals(end, OffsetDateTime.of(2014, 11, 1, 0, 0, 0, 0, ZoneOffset.ofHours(3)))

  test("the last hour of October 2014 belongs to October"):
    // postdate of topic 10992146 (2014-10-31 23:22:29 MSK) that every full reindex kept missing
    val (start, end) = TopicDao.monthInterval(2014, 10, Moscow)
    val postdate = OffsetDateTime.of(2014, 10, 31, 23, 22, 29, 0, ZoneOffset.ofHours(3))

    assert(!postdate.isBefore(start))
    assert(postdate.isBefore(end))

  test("month boundaries are contiguous across years and DST"):
    for
      zone <- Seq(Moscow, Santiago);
      year <- 1998 to 2026;
      month <- 1 to 12
    do
      val (nextYear, nextMonth) =
        if month == 12 then
          (year + 1, 1)
        else
          (year, month + 1)

      assertEquals(TopicDao.monthInterval(year, month, zone)._2, TopicDao.monthInterval(nextYear, nextMonth, zone)._1)
