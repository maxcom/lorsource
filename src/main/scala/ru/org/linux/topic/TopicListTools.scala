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

import org.apache.commons.lang3.StringUtils
import ru.org.linux.site.DateFormats

import java.time.format.{DateTimeFormatter, TextStyle}
import java.time.temporal.ChronoUnit
import java.time.{Instant, Month, ZoneId}
import scala.collection.Seq
import scala.jdk.CollectionConverters.*

object TopicListTools {
  private val OldYearFormat = DateTimeFormatter.ofPattern("yyyy", DateFormats.RussianLocale)
  
  def partitionOf(date: Instant, timezone: ZoneId, now: Instant) =
    def startOfToday = now.atZone(timezone).truncatedTo(ChronoUnit.DAYS)
    def startOfYesterday = now.atZone(timezone).truncatedTo(ChronoUnit.DAYS).minusDays(1)
    def yearAgo = now.atZone(timezone).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).minusYears(1)

    date match
      case date if date.isAfter(startOfToday.toInstant) =>
        "Сегодня"
      case date if date.isAfter(startOfYesterday.toInstant) =>
        "Вчера"
      case date if date.isAfter(yearAgo.toInstant) =>
        val zonedDateTime = date.atZone(timezone)
        val month = Month.of(zonedDateTime.getMonthValue).getDisplayName(TextStyle.FULL_STANDALONE, DateFormats.RussianLocale)

        s"${StringUtils.capitalize(month)} ${zonedDateTime.getYear}"
      case date =>
        OldYearFormat.format(date.atZone(timezone))

  def datePartition(topics: Seq[Topic], timezone: ZoneId): Seq[(String, Topic)] =
    topics.map: topic =>
      partitionOf(topic.getEffectiveDate, timezone, Instant.now) -> topic

  private def grouped[T](seq: Seq[(String, T)]):java.util.List[(String, java.util.List[T])] = {
    val start = (Vector.empty[(String, java.util.List[T])], "", Vector.empty[T])

    val folded = seq.foldLeft(start) { (current, tuple) =>
      val (acc, currentKey, currentSeq) = current
      val (newKey, value) = tuple

      if (newKey == currentKey) {
        (acc, currentKey, currentSeq :+ value)
      } else if (currentSeq.nonEmpty) {
        (acc :+ (currentKey -> currentSeq.asJava), newKey, Vector(value))
      } else {
        (acc, newKey, Vector(value))
      }
    }

    (folded._1 :+ (folded._2 -> folded._3.asJava)).asJava
  }

  private def spacers[T](seq: Seq[(String, T)], count: Int):Seq[Option[(String, T)]] = {
    seq.foldLeft((Vector.empty[Option[(String, T)]], "")) { (current, tuple) =>
      val (acc, currentKey) = current
      val (newKey, _) = tuple

      if (currentKey!=newKey && currentKey.nonEmpty) {
        (acc ++ Vector.fill(count)(None) :+ Some(tuple), newKey)
      } else {
        (acc :+ Some(tuple), newKey)
      }
    }._1
  }

  def split[T](topics: Seq[(String, T)]): java.util.List[java.util.List[(String, java.util.List[T])]] = {
    if (topics.isEmpty) {
      Seq().asJava
    } else {
      val withSpacers = spacers(topics, 1)
      val (first, second) = withSpacers.splitAt(withSpacers.size / 2 + withSpacers.size % 2)

      Seq(grouped(first.flatten), grouped(second.flatten)).asJava
    }
  }
}
