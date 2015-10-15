/*
 * Copyright 1998-2015 Linux.org.ru
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

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.collection.JavaConverters._

object TopicListTools {
  private val OldYearFormat = DateTimeFormat.forPattern("YYYY")

  // в локали месяца не в том склонении :-(
  private val Months = IndexedSeq(
    "Январь", "Февраль",
    "Март", "Апрель", "Май",
    "Июнь", "Июль", "Август",
    "Сентябрь", "Октябрь", "Ноябрь",
    "Декабрь")

  def datePartition(topics: Seq[Topic]): Seq[(String, Topic)] = {
    val startOfToday = DateTime.now.withTimeAtStartOfDay
    val startOfYesterday = DateTime.now.minusDays(1).withTimeAtStartOfDay
    val yearAgo = DateTime.now.withDayOfMonth(1).minusMonths(12).withTimeAtStartOfDay

    topics.map { topic ⇒
      val key = topic.getEffectiveDate match {
                case date if date.isAfter(startOfToday)     ⇒ "Сегодня"
                case date if date.isAfter(startOfYesterday) ⇒ "Вчера"
                case date if date.isAfter(yearAgo)          ⇒ s"${monthName(date)} ${date.getYear}"
                case date                                   ⇒ OldYearFormat.print(date)
              }

      key -> topic
    }
  }

  def monthName(date: DateTime) = Months(date.getMonthOfYear - date.getChronology.monthOfYear().getMinimumValue)

  private def grouped[T](seq: Seq[(String, T)]):java.util.List[(String, java.util.List[T])] = {
    val start = (Vector.empty[(String, java.util.List[T])], "", Vector.empty[T])

    val folded = seq.foldLeft(start) { (current, tuple) ⇒
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
    seq.foldLeft((Vector.empty[Option[(String, T)]], "")) { (current, tuple) ⇒
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