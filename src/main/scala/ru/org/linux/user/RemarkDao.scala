/*
 * Copyright 1998-2016 Linux.org.ru
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

import java.sql.ResultSet
import javax.sql.DataSource

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

import scala.collection.JavaConverters._

@Repository
class RemarkDao(ds:DataSource) {
  private val jdbcTemplate = new JdbcTemplate(ds)
  private val namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate.javaTemplate)

  def remarkCount(user: User):Int = {
    val count:Option[Int] = jdbcTemplate.queryForObject[Integer](
      "SELECT count(*) as c FROM user_remarks WHERE user_id=?",
      user.getId).map(_.toInt)

    count.getOrElse(0)
  }

  def hasRemarks(user: User):Boolean = remarkCount(user) > 0

  /**
   * Получить комментарий пользователя user о ref
   * @param user logged user
   * @param ref  user
   */
  def getRemark(user: User, ref: User): Option[Remark] = {
    jdbcTemplate.queryAndMap("SELECT id, ref_user_id, remark_text FROM user_remarks WHERE user_id=? AND ref_user_id=?", user.getId, ref.getId) { (rs, _) =>
      new Remark(rs)
    }.headOption
  }

  def getRemarks(user: User, refs:java.lang.Iterable[User]): java.util.Map[Integer, Remark] = {
    val r: Map[Integer, Remark] = if (refs.asScala.isEmpty) {
      Map.empty
    } else {
      namedTemplate.query(
        "SELECT id, ref_user_id, remark_text FROM user_remarks WHERE user_id=:user AND ref_user_id IN (:list)",
        Map("list" -> refs.asScala.map(_.getId).toSeq.asJavaCollection, "user" -> user.getId).asJava,
        new RowMapper[(Integer, Remark)]() {
          override def mapRow(rs: ResultSet, rowNum: Int) = {
            val remark = new Remark(rs)
            Integer.valueOf(remark.getRefUserId) -> remark
          }
        }
      ).asScala.toMap
    }

    r.asJava
  }

  private def setRemark(user: User, ref: User, text: String):Unit = {
    if (text.nonEmpty) {
      jdbcTemplate.update("INSERT INTO user_remarks (user_id,ref_user_id,remark_text) VALUES (?,?,?)", user.getId, ref.getId, text)
    }
  }

  private def updateRemark(id: Int, text: String):Unit = {
    if (text.isEmpty) {
      jdbcTemplate.update("DELETE FROM user_remarks WHERE id=?", id)
    } else {
      jdbcTemplate.update("UPDATE user_remarks SET remark_text=? WHERE id=?", text, id)
    }
  }

  /**
   * Сохранить или обновить комментарий пользователя user о ref.
   * Если комментарий нулевой длины - он удаляется из базы
   *
   * @param user logged user
   * @param ref  user
   * @param text текст комментария
   */
  def setOrUpdateRemark(user: User, ref: User, text: String) = {
    getRemark(user, ref) match {
      case Some(remark) ⇒ updateRemark(remark.getId, text)
      case None         ⇒ setRemark(user, ref, text)
    }
  }

  /**
   * Получить комментарии пользователя user
   * @param user logged user
   */
  def getRemarkList(user: User, offset: Int, sortorder: Int, limit: Int): java.util.List[Remark] = {
    val qs = if (sortorder == 1) {
      "SELECT id, ref_user_id, remark_text FROM user_remarks WHERE user_id=? ORDER BY remark_text ASC LIMIT ? OFFSET ?"
    } else {
      "SELECT user_remarks.id as id, user_remarks.user_id as user_id, user_remarks.ref_user_id as ref_user_id, user_remarks.remark_text as remark_text FROM user_remarks, users WHERE user_remarks.user_id=? AND users.id = user_remarks.ref_user_id ORDER BY users.nick ASC LIMIT ? OFFSET ?"
    }

    jdbcTemplate.queryAndMap(qs, user.getId, limit, offset) { (rs, _) ⇒
      new Remark(rs)
    }.asJava
  }
}
