package ru.org.linux.user

import javax.sql.DataSource

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

import scala.collection.JavaConversions._

@Repository
class RemarkDao @Autowired() (ds:DataSource) {
  private val jdbcTemplate = new JdbcTemplate(ds)

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

  def getRemarks(user: User, refs:java.lang.Iterable[User]):java.util.Map[Integer, Remark] = {
    val data = for {
      ref <- refs
      remark <- getRemark(user, ref)
    } yield new Integer(ref.getId) -> remark

    val map = data.toMap

    map
  }

  private def setRemark(user: User, ref: User, text: String):Unit = {
    if (text.nonEmpty) {
      jdbcTemplate.update("INSERT INTO user_remarks (user_id,ref_user_id,remark_text) VALUES (?,?,?)", user.getId, ref.getId, text)
    }
  }

  private def updateRemark(id: Int, text: String) {
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

    jdbcTemplate.queryAndMap(qs, user.getId, limit, offset) { (rs, _) ⇒ new Remark(rs) }
  }
}
