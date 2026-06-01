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
import ru.org.linux.scalikejdbc.SpringDB
import scalikejdbc.*

@Repository
class RemarkDao(springDB: SpringDB):
  def remarkCount(user: User): Int =
    springDB.run:
      sql"SELECT count(*) as c FROM user_remarks WHERE user_id=${user.id}"
        .map(rs => rs.int("c"))
        .single
        .apply()
        .getOrElse(0)

  def hasRemarks(user: User): Boolean = remarkCount(user) > 0

  def getRemark(user: User, ref: User): Option[Remark] =
    springDB.run:
      sql"SELECT id, ref_user_id, remark_text FROM user_remarks WHERE user_id=${user.id} AND ref_user_id=${ref.id}"
        .map(rs => Remark(rs.underlying))
        .single
        .apply()

  def getRemarks(user: User, refs: Iterable[User]): Map[Int, Remark] =
    if refs.isEmpty then
      Map.empty
    else
      val refIds = refs.map(_.id).toSeq
      springDB.run:
        sql"SELECT id, ref_user_id, remark_text FROM user_remarks WHERE user_id=${user.id} AND ref_user_id IN ($refIds)"
          .map { rs =>
            val r = Remark(rs.underlying)
            r.refUserId -> r
          }
          .list
          .apply()
          .toMap

  def setOrUpdateRemark(user: User, ref: User, text: String): Unit =
    if text.isEmpty then
      springDB.run:
        sql"DELETE FROM user_remarks WHERE user_id=${user.id} AND ref_user_id=${ref.id}".update.apply()
    else
      springDB.run:
        sql"""INSERT INTO user_remarks (user_id, ref_user_id, remark_text) VALUES (${user.id}, ${ref.id}, $text)
              ON CONFLICT (user_id, ref_user_id) DO UPDATE SET remark_text=$text""".update.apply()

  def getRemarkList(user: User, offset: Int, sortorder: Int, limit: Int): Seq[Remark] =
    if sortorder == 1 then
      springDB.run:
        sql"SELECT id, ref_user_id, remark_text FROM user_remarks WHERE user_id=${user
            .id} ORDER BY remark_text ASC LIMIT $limit OFFSET $offset".map(rs => Remark(rs.underlying)).list.apply()
    else
      springDB.run:
        sql"""SELECT user_remarks.id as id, user_remarks.user_id as user_id, user_remarks.ref_user_id as ref_user_id, user_remarks.remark_text as remark_text
              FROM user_remarks, users WHERE user_remarks.user_id=${user
            .id} AND users.id = user_remarks.ref_user_id ORDER BY users.nick ASC LIMIT $limit OFFSET $offset"""
          .map(rs => Remark(rs.underlying))
          .list
          .apply()
