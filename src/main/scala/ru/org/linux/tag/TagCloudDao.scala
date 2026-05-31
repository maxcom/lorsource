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
package ru.org.linux.tag

import org.springframework.stereotype.Repository
import ru.org.linux.scalikejdbc.SpringDB
import scalikejdbc.*

import scala.jdk.CollectionConverters.*
import scala.math.log

@Repository
class TagCloudDao(springDB: SpringDB):

  def getTags(tagcount: Int): java.util.List[TagCloudDao.TagDTO] =
    val result = springDB.run:
      sql"SELECT value, counter FROM tags_values WHERE counter >= 10 ORDER BY counter DESC LIMIT $tagcount"
        .map(rs => (rs.string("value"), rs.int("counter")))
        .list
        .apply()

    if result.isEmpty then
      java.util.Collections.emptyList[TagCloudDao.TagDTO]
    else
      val logCounts = result.map(_._2.toDouble).map(log)
      val maxc = logCounts.max
      val minc =
        if maxc == logCounts.min then
          maxc - 1
        else
          logCounts.min

      val tags = result.map: (value, counter) =>
        val tag = new TagCloudDao.TagDTO
        tag.setValue(value)
        tag.setCounter(log(counter))
        tag

      tags.foreach: tag =>
        tag.setWeight(math.round(10 * (tag.getCounter - minc) / (maxc - minc)).toInt)

      tags.sortBy(_.getValue).asJava

end TagCloudDao

object TagCloudDao:
  class TagDTO extends Comparable[TagDTO] with java.io.Serializable:
    private var weight: Int = 0
    private var value: String = null
    private var counter: Double = 0.0

    def getWeight: Int = weight
    def setWeight(weight: Int): Unit = this.weight = weight
    def getValue: String = value
    def setValue(value: String): Unit = this.value = value
    def getCounter: Double = counter
    def setCounter(counter: Double): Unit = this.counter = counter

    override def compareTo(o: TagDTO): Int = value.compareTo(o.value)
