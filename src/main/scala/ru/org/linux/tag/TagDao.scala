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

@Repository
class TagDao(springDB: SpringDB):

  private def escapeLikeWildcards(str: String): String = str.replaceAll("[_%]", "\\\\$0")

  def createTag(tagName: String): Int =
    assume(TagName.isGoodTag(tagName), "Tag name must be valid")
    springDB.run:
      sql"INSERT INTO tags_values (value) VALUES ($tagName) RETURNING id".map(rs => rs.int("id")).single.apply().get

  def createTagSynonym(tagName: String, id: Int): Unit =
    assume(TagName.isGoodTag(tagName), "Tag name must be valid")
    springDB.run:
      sql"INSERT INTO tags_synonyms (value, tagid) VALUES ($tagName, $id)".update.apply()

  def updateTagSynonym(oldId: Int, newId: Int): Unit =
    springDB.run:
      sql"UPDATE tags_synonyms SET tagid=$newId WHERE tagid=$oldId".update.apply()

  def changeTag(tagId: Int, tagName: String): Unit =
    springDB.run:
      sql"UPDATE tags_values SET value=$tagName WHERE id=$tagId".update.apply()

  def deleteTag(tagId: Int): Unit =
    springDB.run:
      sql"DELETE FROM tags_synonyms WHERE tagid=$tagId".update.apply()
      sql"DELETE FROM tags_values WHERE id=$tagId".update.apply()

  def deleteTagSynonym(tagName: String): Unit =
    springDB.run:
      sql"DELETE FROM tags_synonyms WHERE value=$tagName".update.apply()

  private[tag] def getFirstLetters: Seq[String] =
    springDB.run:
      sql"""select distinct firstchar from
            (select lower(substr(value,1,1)) as firstchar from tags_values
            where counter > 0 order by firstchar) firstchars""".map(rs => rs.string("firstchar")).list.apply().sorted

  private[tag] def getTagsByPrefix(prefix: String, minCount: Int): Seq[TagInfo] =
    springDB.run:
      sql"select counter, value, id from tags_values where value like ${escapeLikeWildcards(prefix) +
          "%"} and counter >= $minCount order by value"
        .map(rs => TagInfo(rs.string("value"), rs.int("counter"), rs.int("id")))
        .list
        .apply()
        .toSeq

  private[tag] def getTopTagsByPrefix(prefix: String, minCount: Int, count: Int): Seq[String] =
    val wildcard = escapeLikeWildcards(prefix) + "%"
    springDB.run:
      sql"""select value from
            (select s.value, counter from tags_synonyms s join tags_values v on s.tagid=v.id where s.value like $wildcard
            union all
            select value, counter from tags_values where value like $wildcard) j
           where counter>=$minCount order by counter desc limit $count"""
        .map(rs => rs.string("value"))
        .list
        .apply()
        .sorted

  def getTagId(tag: String, skipZero: Boolean = false): Option[Int] =
    springDB.run:
      val query =
        if skipZero then
          sql"SELECT id FROM tags_values WHERE value=$tag AND counter>0"
        else
          sql"SELECT id FROM tags_values WHERE value=$tag"
      query.map(rs => rs.int("id")).single.apply()

  def getTagSynonymId(tag: String): Option[Int] =
    springDB.run:
      sql"SELECT tagid FROM tags_synonyms WHERE value=$tag".map(rs => rs.int("tagid")).single.apply()

  def getTagInfo(tagId: Int): TagInfo =
    springDB.run:
      sql"SELECT counter, value, id FROM tags_values WHERE id=$tagId"
        .map(rs => TagInfo(rs.string("value"), rs.int("counter"), rs.int("id")))
        .single
        .apply()
        .get

  def getSynonymsFor(tagId: Int): Seq[String] =
    springDB.run:
      sql"SELECT value FROM tags_synonyms WHERE tagid=$tagId".map(rs => rs.string("value")).list.apply().toSeq
