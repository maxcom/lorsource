/*
 * Copyright 1998-2014 Linux.org.ru
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

import com.google.common.collect._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import ru.org.linux.site.DateFormats

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

object TopicListTools {
  private val ThisYearFormat = DateTimeFormat.forPattern("MMMMMMMM YYYY").withLocale(DateFormats.RUSSIAN_LOCALE)
  private val OldYearFormat = DateTimeFormat.forPattern("YYYY")

  def datePartition(topics: java.util.List[Topic]): ImmutableListMultimap[String, Topic] = {
    val startOfToday = DateTime.now.withTimeAtStartOfDay
    val startOfYesterday = DateTime.now.minusDays(1).withTimeAtStartOfDay
    val startOfYear = DateTime.now.withDayOfYear(1).withTimeAtStartOfDay

    Multimaps.index(topics, new com.google.common.base.Function[Topic, String]() {
      override def apply(input: Topic): String = {
        input.getEffectiveDate match {
          case date if date.isAfter(startOfToday)     ⇒ "Сегодня"
          case date if date.isAfter(startOfYesterday) ⇒ "Вчера"
          case date if date.isAfter(startOfYear)      ⇒ ThisYearFormat.print(date)
          case date                                   ⇒ OldYearFormat.print(date)
        }
      }
    })
  }

  def split[T](topics: ListMultimap[String, T]): java.util.List[java.util.List[(String, java.util.List[T])]] = {
    if (topics.isEmpty) {
      Seq()
    } else {
      val splitAt = topics.size / 2 + (topics.size % 2)
      val first: ArrayBuffer[(String, java.util.List[T])] = ArrayBuffer()
      val second: ArrayBuffer[(String, java.util.List[T])] = ArrayBuffer()
      var total: Int = 0

      for (entry <- topics.asMap.entrySet) {
        val currentSize = entry.getValue.size
        if (total + (currentSize / 2) <= splitAt) {
          first.append(entry.getKey -> entry.getValue.toSeq)
        } else {
          second.append(entry.getKey -> entry.getValue.toSeq)
        }
        total += currentSize
      }

      Seq(first.asJava, second.asJava)
    }
  }
}
