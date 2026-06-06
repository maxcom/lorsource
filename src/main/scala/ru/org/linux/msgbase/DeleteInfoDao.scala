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
package ru.org.linux.msgbase

import org.springframework.stereotype.Repository
import ru.org.linux.site.DeleteInfo
import ru.org.linux.user.User
import ru.org.linux.scalikejdbc.SpringDB
import scalikejdbc.*

@Repository
class DeleteInfoDao(springDB: SpringDB):

  def getDeleteInfo(id: Int, forUpdate: Boolean = false): Option[DeleteInfo] =
    springDB.run:
      val query =
        if forUpdate then
          sql"SELECT reason, delby as userid, deldate, bonus FROM del_info WHERE msgid=$id FOR UPDATE"
        else
          sql"SELECT reason, delby as userid, deldate, bonus FROM del_info WHERE msgid=$id"

      query
        .map { rs =>
          val bonus: Option[Int] = rs.intOpt("bonus")
          DeleteInfo(rs.int("userid"), rs.string("reason"), rs.timestamp("deldate"), bonus)
        }
        .single
        .apply()

  def insert(info: InsertDeleteInfo): Unit = insert(info.msgid, info.deleteUser, info.reason, info.bonus)

  private def insert(msgid: Int, deleter: User, reason: String, scoreBonus: Int): Unit =
    require(scoreBonus <= 0, "Score bonus on delete must be non-positive")
    springDB.run:
      sql"INSERT INTO del_info (msgid, delby, reason, deldate, bonus) VALUES ($msgid, ${deleter
          .id}, $reason, CURRENT_TIMESTAMP, $scoreBonus)".update.apply()

  def getRecentScoreLoss(user: User): Int =
    springDB.run:
      sql"""SELECT COALESCE(sum(bonus), 0) FROM del_info
            WHERE deldate > CURRENT_TIMESTAMP - '3 days'::interval
            AND msgid IN (
              SELECT id FROM comments WHERE comments.userid = ${user.id}
              UNION ALL
              SELECT id FROM topics WHERE topics.userid = ${user.id}
            )""".map(rs => math.abs(rs.int(1))).single.apply().getOrElse(0)

  def insert(deleteInfos: Seq[InsertDeleteInfo]): Unit =
    if deleteInfos.nonEmpty then
      deleteInfos.foreach { info =>
        require(info.bonus <= 0, "Score bonus on delete must be non-positive")
      }

      springDB.run:
        sql"INSERT INTO del_info (msgid, delby, reason, deldate, bonus) VALUES ({msgid}, {delby}, {reason}, CURRENT_TIMESTAMP, {bonus})"
          .batchByName(
            deleteInfos.map { info =>
              Seq("msgid" -> info.msgid, "delby" -> info.deleteUser.id, "reason" -> info.reason, "bonus" -> info.bonus)
            }*)
          .apply()

  def delete(msgid: Int): Unit =
    springDB.run:
      sql"DELETE FROM del_info WHERE msgid=$msgid".update.apply()

  def scoreLoss(msgid: Int): Int =
    springDB.run:
      sql"""SELECT COALESCE(
              (SELECT sum(-bonus) AS total_bonus FROM del_info
               JOIN comments ON comments.id = del_info.msgid
               WHERE bonus IS NOT NULL AND bonus != 0
               AND comments.userid != 2 AND comments.deleted AND topic = $msgid),
              0)""".map(rs => rs.int(1)).single.apply().getOrElse(0)

end DeleteInfoDao

case class InsertDeleteInfo(msgid: Int, reason: String, bonus: Int, deleteUser: User)
