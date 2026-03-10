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

import org.springframework.jdbc.core.{BatchPreparedStatementSetter, JdbcTemplate}
import org.springframework.stereotype.Repository
import ru.org.linux.site.DeleteInfo
import ru.org.linux.user.User

import java.sql.{PreparedStatement, ResultSet}
import javax.sql.DataSource
import scala.jdk.CollectionConverters.*

/**
 * Получение информации кем и почему удален топик
 */
@Repository
class DeleteInfoDao(dataSource: DataSource) {
  private val jdbcTemplate: JdbcTemplate = new JdbcTemplate(dataSource)

  /**
   * Кто, когда и почему удалил сообщение
   * @param id id проверяемого сообщения
   * @param forUpdate блокировать запись до конца текущей транзакции (SELECT ... FOR UPDATE)
   * @return информация о удаленном сообщении
   */
  def getDeleteInfo(id: Int, forUpdate: Boolean = false): Option[DeleteInfo] = {
    val list = jdbcTemplate.query(
      if (forUpdate) DeleteInfoDao.QueryDeleteInfoForUpdate else DeleteInfoDao.QueryDeleteInfo,
      (rs: ResultSet, _: Int) => {
        val actualBonus: Option[Int] = if (rs.wasNull()) None else Some(rs.getInt("bonus"))

        DeleteInfo(
          rs.getInt("userid"),
          rs.getString("reason"),
          rs.getTimestamp("deldate"),
          actualBonus
        )
      },
      id
    )

    list.asScala.headOption
  }

  def insert(info: InsertDeleteInfo): Unit =
    insert(info.msgid, info.deleteUser, info.reason, info.bonus)

  private def insert(msgid: Int, deleter: User, reason: String, scoreBonus: Int): Unit = {
    require(scoreBonus <= 0, "Score bonus on delete must be non-positive")

    jdbcTemplate.update(DeleteInfoDao.InsertDeleteInfoSql, msgid, deleter.id, reason, scoreBonus)
  }

  def getRecentScoreLoss(user: User): Int = {
    Math.abs(
      jdbcTemplate.queryForObject(
        "select COALESCE(sum(bonus), 0) from del_info where deldate>CURRENT_TIMESTAMP-'3 days'::interval and " +
          "msgid in (select id from comments where comments.userid = ? union all select id from topics where topics.userid = ?)",
        classOf[Integer],
        user.id,
        user.id
      )
    )
  }

  def insert(deleteInfos: Seq[InsertDeleteInfo]): Unit = {
    if (deleteInfos.nonEmpty) {
      deleteInfos.foreach { info =>
        require(info.bonus <= 0, "Score bonus on delete must be non-positive")
      }

      jdbcTemplate.batchUpdate(
        DeleteInfoDao.InsertDeleteInfoSql,
        new BatchPreparedStatementSetter {
          override def setValues(ps: PreparedStatement, i: Int): Unit = {
            val info = deleteInfos(i)

            ps.setInt(1, info.msgid)
            ps.setInt(2, info.deleteUser.id)
            ps.setString(3, info.reason)
            ps.setInt(4, info.bonus)
          }

          override def getBatchSize: Int = deleteInfos.size
        }
      )
    }
  }

  def delete(msgid: Int): Unit =
    jdbcTemplate.update("DELETE FROM del_info WHERE msgid=?", msgid)

  def scoreLoss(msgid: Int): Int =
    jdbcTemplate.queryForObject(
      "select COALESCE((select sum(-bonus) as total_bonus from del_info " +
        "join comments on comments.id = del_info.msgid where bonus is not null and bonus!=0 and " +
        "comments.userid!=2 and comments.deleted and topic = ?), 0)",
      classOf[Int],
      msgid
    )
}

case class InsertDeleteInfo(msgid: Int, reason: String, bonus: Int, deleteUser: User)

object DeleteInfoDao {
  private val QueryDeleteInfo =
    "SELECT reason,delby as userid, deldate, bonus FROM del_info WHERE msgid=?"

  private val QueryDeleteInfoForUpdate =
    "SELECT reason,delby as userid, deldate, bonus FROM del_info WHERE msgid=? FOR UPDATE"

  private val InsertDeleteInfoSql =
    "INSERT INTO del_info (msgid, delby, reason, deldate, bonus) values(?,?,?, CURRENT_TIMESTAMP, ?)"
}
