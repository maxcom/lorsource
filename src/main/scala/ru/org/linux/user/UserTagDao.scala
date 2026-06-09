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
package ru.org.linux.user

import org.springframework.stereotype.Repository
import ru.org.linux.scalikejdbc.{SpringDB, Transaction}
import ru.org.linux.scalikejdbc.Transaction.given
import scalikejdbc.*

@Repository
class UserTagDao(springDB: SpringDB):
  def addTag(userId: Int, tagId: Int, isFavorite: Boolean)(using Transaction): Unit =
    sql"INSERT INTO user_tags (user_id, tag_id, is_favorite) VALUES ($userId, $tagId, $isFavorite) ON CONFLICT DO NOTHING"
      .update
      .apply()

  def deleteTag(userId: Int, tagId: Int, isFavorite: Boolean)(using Transaction): Unit =
    sql"DELETE FROM user_tags WHERE user_id=$userId AND tag_id=$tagId AND is_favorite=$isFavorite".update.apply()

  def deleteTags(tagId: Int)(using Transaction): Unit = sql"DELETE FROM user_tags WHERE tag_id=$tagId".update.apply()

  def getTags(userId: Int, isFavorite: Boolean): List[String] =
    springDB.run:
      sql"""SELECT tags_values.value FROM user_tags, tags_values WHERE
            user_tags.user_id=$userId AND tags_values.id=user_tags.tag_id AND user_tags.is_favorite=$isFavorite
            ORDER BY value""".map(rs => rs.string("value")).list.apply()

  def getUserIdListByTags(userId: Int, tags: Seq[Int]): List[Int] =
    if tags.isEmpty then
      List.empty
    else
      springDB.run:
        sql"""select distinct user_id from user_tags where tag_id in ($tags)
              AND is_favorite = true
              AND user_id not in (
                select userid from ignore_list where ignored=$userId union
                select $userId union
                select user_id from user_tags where tag_id in ($tags) and is_favorite = false)"""
          .map(rs => rs.int("user_id"))
          .list
          .apply()

  def replaceTag(oldTagId: Int, newTagId: Int)(using Transaction): Unit =
    sql"UPDATE user_tags SET tag_id=$newTagId WHERE tag_id=$oldTagId AND user_id NOT IN (SELECT user_id FROM user_tags WHERE tag_id=$newTagId)"
      .update
      .apply()

  def countFavs(tagId: Int): Int =
    springDB.run:
      sql"SELECT count(*) FROM user_tags WHERE tag_id=$tagId AND is_favorite"
        .map(rs => rs.int(1))
        .single
        .apply()
        .getOrElse(0)

  def countIgnore(tagId: Int): Int =
    springDB.run:
      sql"SELECT count(*) FROM user_tags WHERE tag_id=$tagId AND NOT is_favorite"
        .map(rs => rs.int(1))
        .single
        .apply()
        .getOrElse(0)

  def deleteUnusedTags()(using Transaction): Int =
    sql"delete from user_tags where not exists (select * from tags join topics on topics.id=tags.msgid where tagid=user_tags.tag_id and not deleted)"
      .update
      .apply()
