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

import org.springframework.stereotype.Repository
import ru.org.linux.scalikejdbc.{SpringDB, Transaction}
import ru.org.linux.scalikejdbc.Transaction.given
import ru.org.linux.tag.TagInfo
import scalikejdbc.*

@Repository
class TopicTagDao(springDB: SpringDB):

  def addTag(msgId: Int, tagId: Int)(using Transaction): Unit =
    val inserted = sql"INSERT INTO tags VALUES($msgId, $tagId) ON CONFLICT DO NOTHING".update.apply()
    if inserted > 0 then
      sql"UPDATE tags_values SET counter = counter + 1 WHERE id = $tagId".update.apply()

  def deleteTag(msgId: Int, tagId: Int)(using Transaction): Unit =
    sql"DELETE FROM tags WHERE msgid=$msgId AND tagid=$tagId".update.apply()

  def lockTagValues(ids: Seq[Int])(using Transaction): Unit =
    if ids.nonEmpty then
      sql"SELECT id FROM tags_values WHERE id IN ($ids) ORDER BY id FOR UPDATE"
        .map(rs => rs.int("id")).list.apply()

  def getTags(msgid: Int): Seq[TagInfo] =
    springDB.run:
      sql"""SELECT tags_values.value, tags_values.counter, tags_values.id
            FROM tags, tags_values
            WHERE tags.msgid=$msgid AND tags_values.id=tags.tagid
            ORDER BY value""".map(rs => TagInfo(rs.string("value"), rs.int("counter"), rs.int("id"))).list.apply()

  def getCountReplacedTags(oldTagId: Int, newTagId: Int): Int =
    springDB.run:
      sql"""SELECT count(tagid) FROM tags
            WHERE tagid=$oldTagId AND msgid NOT IN (
              SELECT msgid FROM tags WHERE tagid=$newTagId
            )""".map(rs => rs.int(1)).single.apply().getOrElse(0)

  def replaceTag(oldTagId: Int, newTagId: Int)(using Transaction): Unit =
    sql"""UPDATE tags SET tagid=$newTagId
          WHERE tagid=$oldTagId AND msgid NOT IN (
            SELECT msgid FROM tags WHERE tagid=$newTagId
          )""".update.apply()

  def deleteTag(tagId: Int)(using Transaction): Unit = sql"DELETE FROM tags WHERE tagid=$tagId".update.apply()

  def reCalculateAllCounters()(using Transaction): Unit =
    sql"""UPDATE tags_values SET counter =
          (SELECT count(*) FROM tags JOIN topics ON tags.msgid=topics.id
           JOIN groups ON topics.groupid = groups.id
           JOIN sections ON sections.id = groups.section
           WHERE tags.tagid=tags_values.id AND NOT deleted AND (topics.moderate OR NOT sections.moderate))"""
      .update
      .apply()

  def getTags(topics: Seq[Int]): Seq[(Int, TagInfo)] =
    if topics.isEmpty then
      Vector.empty
    else
      springDB.run:
        sql"""SELECT msgid, tags_values.value, tags_values.counter, tags_values.id
              FROM tags, tags_values
              WHERE tags.msgid IN ($topics) AND tags_values.id=tags.tagid
              ORDER BY value"""
          .map(rs => rs.int("msgid") -> TagInfo(rs.string("value"), rs.int("counter"), rs.int("id")))
          .list
          .apply()

  def increaseCounterById(tagId: Int, tagCount: Int)(using Transaction): Unit =
    sql"UPDATE tags_values SET counter=counter+$tagCount WHERE id=$tagId".update.apply()

  def processTopicsByTag(tagId: Int, f: Int => Unit): Unit =
    springDB.run:
      sql"SELECT msgid FROM tags WHERE tags.tagid=$tagId".map(rs => rs.int(1)).list.apply().foreach(f)

  def getTagSections(tagId: Int): Seq[Int] =
    springDB.run:
      sql"""SELECT DISTINCT section FROM
            groups JOIN topics ON topics.groupid=groups.id JOIN tags ON tags.msgid = topics.id
            WHERE tagid=$tagId AND NOT deleted AND NOT draft ORDER BY section""".map(rs => rs.int(1)).list.apply()

end TopicTagDao
