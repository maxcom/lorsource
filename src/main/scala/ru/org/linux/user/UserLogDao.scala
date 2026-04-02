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

import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Repository
import org.springframework.transaction.PlatformTransactionManager

import java.time.{Duration, Instant, OffsetDateTime}
import java.util
import javax.annotation.Nullable
import javax.sql.DataSource
import scala.jdk.CollectionConverters.*

@Repository
class UserLogDao(ds: DataSource, val transactionManager: PlatformTransactionManager) extends TransactionManagement {
  private val jdbcTemplate = new JdbcTemplate(ds)

  def logResetUserpic(user: User, actionUser: User, bonus: Int): Unit = transactional() { _ =>
    var map = Map[String, Any]()

    if (bonus != 0) {
      map = map + (UserLogDao.OptionBonus -> Int.box(bonus))
    }

    if (user.photo != null) {
      map = map + (UserLogDao.OptionOldUserpic -> user.photo)
    }

    jdbcTemplate.update(
      "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
      Int.box(user.id),
      Int.box(actionUser.id),
      UserLogAction.RESET_USERPIC.toString,
      map.asJava
    )
  }

  def logSetUserpic(user: User, userpic: String): Unit = transactional() { _ =>
    var map = Map[String, Any]()

    if (user.photo != null) {
      map = map + (UserLogDao.OptionOldUserpic -> user.photo)
    }

    map = map + (UserLogDao.OptionNewUserpic -> userpic)

    jdbcTemplate.update(
      "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
      Int.box(user.id),
      Int.box(user.id),
      UserLogAction.SET_USERPIC.toString,
      map.asJava
    )
  }

  def logBlockUser(user: User, moderator: User, reason: String): Unit = transactional() { _ =>
    jdbcTemplate.update(
      "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
      Int.box(user.id),
      Int.box(moderator.id),
      UserLogAction.BLOCK_USER.toString,
      Map(UserLogDao.OptionReason -> reason).asJava
    )
  }

  def logFreezeUser(user: User, moderator: User, reason: String, until: Instant): Unit = transactional() { _ =>
    val options: Map[String, String] =
      if (until.isBefore(Instant.now())) {
        Map(UserLogDao.OptionReason -> reason)
      } else {
        Map(UserLogDao.OptionReason -> reason, UserLogDao.OptionUntil -> until.toString)
      }

    val action = if (until.isBefore(Instant.now())) UserLogAction.DEFROSTED else UserLogAction.FROZEN

    jdbcTemplate.update(
      "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
      Int.box(user.id),
      Int.box(moderator.id),
      action.toString,
      options.asJava
    )
  }

  def logScore50(user: User, moderator: User): Unit = transactional() { _ =>
    jdbcTemplate.update(
      "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, '')",
      Int.box(user.id),
      Int.box(moderator.id),
      UserLogAction.SCORE50.toString
    )
  }

  def logUnblockUser(user: User, moderator: User): Unit = transactional() { _ =>
    jdbcTemplate.update(
      "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
      Int.box(user.id),
      Int.box(moderator.id),
      UserLogAction.UNBLOCK_USER.toString,
      Map.empty[String, String].asJava
    )
  }

  def logAcceptNewEmail(user: User, newEmail: String): Unit = transactional() { _ =>
    var map = Map[String, Any]()

    map = map + (UserLogDao.OptionNewEmail -> newEmail)

    if (user.email != null) {
      map = map + (UserLogDao.OptionOldEmail -> user.email)
    }

    jdbcTemplate.update(
      "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
      Int.box(user.id),
      Int.box(user.id),
      UserLogAction.ACCEPT_NEW_EMAIL.toString,
      map.asJava
    )
  }

  def logResetInfo(user: User, moderator: User, userInfo: String, bonus: Int): Unit = transactional() { _ =>
    jdbcTemplate.update(
      "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
      Int.box(user.id),
      Int.box(moderator.id),
      UserLogAction.RESET_INFO.toString,
      Map(
        UserLogDao.OptionOldInfo -> userInfo,
        UserLogDao.OptionBonus -> Int.box(bonus)
      ).asJava
    )
  }

  def logResetUrl(user: User, moderator: User, url: String, bonus: Int): Unit = transactional() { _ =>
    jdbcTemplate.update(
      "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
      Int.box(user.id),
      Int.box(moderator.id),
      UserLogAction.RESET_URL.toString,
      Map(
        UserLogDao.OptionOldUrl -> url,
        UserLogDao.OptionBonus -> Int.box(bonus)
      ).asJava
    )
  }

  def logResetTown(user: User, moderator: User, town: String, bonus: Int): Unit = transactional() { _ =>
    jdbcTemplate.update(
      "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
      Int.box(user.id),
      Int.box(moderator.id),
      UserLogAction.RESET_TOWN.toString,
      Map(
        UserLogDao.OptionOldTown -> town,
        UserLogDao.OptionBonus -> Int.box(bonus)
      ).asJava
    )
  }

  def logSentPasswordReset(resetFor: User, @Nullable resetBy: User, email: String): Unit = transactional() { _ =>
    jdbcTemplate.update(
      "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
      Int.box(resetFor.id),
      Int.box(if (resetBy != null) resetBy.id else resetFor.id),
      UserLogAction.SENT_PASSWORD_RESET.toString,
      Map(UserLogDao.OptionEmail -> email).asJava
    )
  }

  def logResetPassword(user: User, moderator: User): Unit = transactional() { _ =>
    jdbcTemplate.update(
      "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
      Int.box(user.id),
      Int.box(moderator.id),
      UserLogAction.RESET_PASSWORD.toString,
      Map.empty[String, String].asJava
    )
  }

  def logSetPassword(user: User, ip: String): Unit = transactional() { _ =>
    jdbcTemplate.update(
      "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
      Int.box(user.id),
      Int.box(user.id),
      UserLogAction.SET_PASSWORD.toString,
      Map(UserLogDao.OptionIp -> ip).asJava
    )
  }

  def logSetUserInfo(user: User, info: java.util.Map[String, String]): Unit = transactional() { _ =>
    jdbcTemplate.update(
      "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
      Int.box(user.id),
      Int.box(user.id),
      UserLogAction.SET_INFO.toString,
      info
    )
  }

  def getLogItems(user: User, includeSelf: Boolean): Seq[UserLogItem] = {
    val sql =
      if (includeSelf) {
        "SELECT id, userid, action_userid, action_date, action, info FROM user_log WHERE userid=? ORDER BY id DESC"
      } else {
        "SELECT id, userid, action_userid, action_date, action, info FROM user_log WHERE userid=? AND userid!=action_userid ORDER BY id DESC"
      }

    jdbcTemplate.queryAndMap(
      sql,
      user.id
    ) { (rs, _) =>
      val options = 
        rs
          .getObject("info")
          .asInstanceOf[util.Map[String, String]]
          .asScala
          .view
          .mapValues(v => if (v == null) "" else v)
          .toMap
      
      UserLogItem(
        rs.getInt("id"),
        rs.getInt("userid"),
        rs.getInt("action_userid"),
        rs.getTimestamp("action_date").toInstant,
        UserLogAction.valueOf(rs.getString("action").toUpperCase),
        options)
    }
  }

  def getUserpicSetCount(user: User, duration: Duration): Int = {
    jdbcTemplate.queryForObject[Int](
      "SELECT count(*) FROM user_log WHERE userid=? AND action=?::user_log_action AND action_date>?",
      Int.box(user.id),
      UserLogAction.SET_USERPIC.toString,
      OffsetDateTime.now().minus(duration)
    ).get
  }

  def hasRecentModerationEvent(user: User, duration: Duration, action: UserLogAction): Boolean = {
    jdbcTemplate.queryForObject[Boolean](
      "SELECT EXISTS (SELECT * FROM user_log WHERE userid=? AND action=?::user_log_action AND action_date>? AND userid!=action_userid)",
      Int.box(user.id),
      action.toString,
      OffsetDateTime.now().minus(duration)
    ).get
  }

  def hasRecentSelfEvent(user: User, duration: Duration, action: UserLogAction): Boolean = {
    jdbcTemplate.queryForObject[Boolean](
      "SELECT EXISTS (SELECT * FROM user_log WHERE userid=? AND action=?::user_log_action AND action_date>? AND userid=action_userid)",
      Int.box(user.id),
      action.toString,
      OffsetDateTime.now().minus(duration)
    ).get
  }

  def getRecentlyHasEvent(action: UserLogAction): Seq[Int] = {
    jdbcTemplate.queryForSeq[Int](
      "SELECT userid FROM user_log WHERE action=?::user_log_action AND action_date>CURRENT_TIMESTAMP - interval '3 days' ORDER BY action_date",
      action.toString
    )
  }

  def logRegister(userid: Int, ip: String, invitedBy: Option[Int], userAgent: Int, language: Option[String]): Unit = transactional() { _ =>
    var map = Map[String, String]()

    map = map + (UserLogDao.OptionIp -> ip)
    map = map + (UserLogDao.OptionUserAgent -> Integer.toString(userAgent))
    language.foreach(lang => map = map + (UserLogDao.OptionAcceptLanguage -> lang))
    invitedBy.foreach(user => map = map + (UserLogDao.OptionInvitedBy -> user.toString))

    jdbcTemplate.update(
      "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
      Int.box(userid),
      Int.box(userid),
      UserLogAction.REGISTER.toString,
      map.asJava
    )
  }

  def setCorrector(user: User, moderator: User): Unit = transactional() { _ =>
    jdbcTemplate.update(
      "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
      Int.box(user.id),
      Int.box(moderator.id),
      UserLogAction.SET_CORRECTOR.toString,
      Map.empty[String, String].asJava
    )
  }

  def unsetCorrector(user: User, moderator: User): Unit = transactional() { _ =>
    jdbcTemplate.update(
      "INSERT INTO user_log (userid, action_userid, action_date, action, info) VALUES (?,?,CURRENT_TIMESTAMP, ?::user_log_action, ?)",
      Int.box(user.id),
      Int.box(moderator.id),
      UserLogAction.UNSET_CORRECTOR.toString,
      Map.empty[String, String].asJava
    )
  }
}

object UserLogDao {
  val OptionOldUserpic = "old_userpic"
  val OptionNewUserpic = "new_userpic"
  val OptionBonus = "bonus"
  val OptionReason = "reason"
  val OptionEmail = "email"
  val OptionOldEmail = "old_email"
  val OptionNewEmail = "new_email"
  val OptionOldInfo = "old_info"
  val OptionOldTown = "old_town"
  val OptionOldUrl = "old_url"
  val OptionIp = "ip"
  val OptionUserAgent = "user_agent"
  val OptionInvitedBy = "invited_by"
  val OptionAcceptLanguage = "accept_lang"
  val OptionUntil = "until"
}
