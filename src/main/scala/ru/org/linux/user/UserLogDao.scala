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
import ru.org.linux.scalikejdbc.SpringDB.given
import ru.org.linux.scalikejdbc.Transaction.given
import scalikejdbc.*

import java.time.{Duration, Instant, OffsetDateTime}
import java.util as ju
import javax.annotation.Nullable
import scala.jdk.CollectionConverters.*

@Repository
class UserLogDao(springDB: SpringDB):

  private def insertLog(userid: Int, actionUserId: Int, action: UserLogAction, info: ju.Map[String, String])(using
      DBSession): Unit =
    sql"""INSERT INTO user_log (userid, action_userid, action_date, action, info)
          VALUES ($userid, $actionUserId, CURRENT_TIMESTAMP, ${action.toDbName}::user_log_action, $info)"""
      .update
      .apply()

  def logResetUserpic(user: User, actionUser: User, bonus: Int)(using Transaction): Unit =
    val info: ju.Map[String, String] = (
      Map.empty[String, String] ++ Option.when(bonus != 0)(UserLogDao.OptionBonus -> bonus.toString) ++
        Option.when(user.photo != null)(UserLogDao.OptionOldUserpic -> user.photo)
    ).asJava

    insertLog(user.id, actionUser.id, UserLogAction.ResetUserpic, info)

  def logSetUserpic(user: User, userpic: String)(using Transaction): Unit =
    val info: ju.Map[String, String] = (
      Map.empty[String, String] ++ Option.when(user.photo != null)(UserLogDao.OptionOldUserpic -> user.photo) +
        (UserLogDao.OptionNewUserpic -> userpic)
    ).asJava

    insertLog(user.id, user.id, UserLogAction.SetUserpic, info)

  def logBlockUser(user: User, moderator: User, reason: String)(using Transaction): Unit =
    insertLog(user.id, moderator.id, UserLogAction.BlockUser, Map(UserLogDao.OptionReason -> reason).asJava)

  def logFreezeUser(user: User, moderator: User, reason: String, until: Instant)(using Transaction): Unit =
    val action =
      if until.isBefore(Instant.now()) then
        UserLogAction.Defrosted
      else
        UserLogAction.Frozen

    val info: ju.Map[String, String] =
      if until.isBefore(Instant.now()) then
        Map(UserLogDao.OptionReason -> reason).asJava
      else
        Map(UserLogDao.OptionReason -> reason, UserLogDao.OptionUntil -> until.toString).asJava

    insertLog(user.id, moderator.id, action, info)

  def logScore50(user: User, moderator: User)(using Transaction): Unit =
    insertLog(user.id, moderator.id, UserLogAction.Score50, ju.Collections.emptyMap[String, String]())

  def logUnblockUser(user: User, moderator: User)(using Transaction): Unit =
    insertLog(user.id, moderator.id, UserLogAction.UnblockUser, ju.Collections.emptyMap[String, String]())

  def logAcceptNewEmail(user: User, newEmail: String)(using Transaction): Unit =
    val info: ju.Map[String, String] = (
      Map(UserLogDao.OptionNewEmail -> newEmail) ++
        Option.when(user.email != null)(UserLogDao.OptionOldEmail -> user.email)
    ).asJava

    insertLog(user.id, user.id, UserLogAction.AcceptNewEmail, info)

  def logResetInfo(user: User, moderator: User, userInfo: String, bonus: Int)(using Transaction): Unit =
    val info = Map(UserLogDao.OptionOldInfo -> userInfo, UserLogDao.OptionBonus -> bonus.toString).asJava
    insertLog(user.id, moderator.id, UserLogAction.ResetInfo, info)

  def logResetUrl(user: User, moderator: User, url: String, bonus: Int)(using Transaction): Unit =
    val info = Map(UserLogDao.OptionOldUrl -> url, UserLogDao.OptionBonus -> bonus.toString).asJava
    insertLog(user.id, moderator.id, UserLogAction.ResetUrl, info)

  def logResetTown(user: User, moderator: User, town: String, bonus: Int)(using Transaction): Unit =
    val info = Map(UserLogDao.OptionOldTown -> town, UserLogDao.OptionBonus -> bonus.toString).asJava
    insertLog(user.id, moderator.id, UserLogAction.ResetTown, info)

  def logSentPasswordReset(
      resetFor: User,
      @Nullable
      resetBy: User,
      email: String)(using Transaction): Unit =
    val actionUserId =
      if resetBy != null then
        resetBy.id
      else
        resetFor.id
    insertLog(resetFor.id, actionUserId, UserLogAction.SentPasswordReset, Map(UserLogDao.OptionEmail -> email).asJava)

  def logResetPassword(user: User, moderator: User)(using Transaction): Unit =
    insertLog(user.id, moderator.id, UserLogAction.ResetPassword, ju.Collections.emptyMap[String, String]())

  def logSetPassword(user: User, ip: String)(using Transaction): Unit =
    insertLog(user.id, user.id, UserLogAction.SetPassword, Map(UserLogDao.OptionIp -> ip).asJava)

  def logSetUserInfo(user: User, info: ju.Map[String, String])(using Transaction): Unit =
    insertLog(user.id, user.id, UserLogAction.SetInfo, info)

  def getLogItems(user: User, includeSelf: Boolean): Seq[UserLogItem] =
    springDB.run {
      if includeSelf then
        sql"""SELECT id, userid, action_userid, action_date, action, info
            FROM user_log WHERE userid=${user.id} ORDER BY id DESC""".map(toLogItem).list.apply()
      else
        sql"""SELECT id, userid, action_userid, action_date, action, info
            FROM user_log WHERE userid=${user.id} AND userid!=action_userid ORDER BY id DESC"""
          .map(toLogItem)
          .list
          .apply()
    }

  def getUserpicSetCount(user: User, duration: Duration): Int =
    springDB.run {
      sql"""SELECT count(*) FROM user_log
          WHERE userid=${user.id} AND action=${UserLogAction
          .SetUserpic
          .toDbName}::user_log_action AND action_date>${OffsetDateTime.now().minus(duration)}"""
        .map(rs => rs.int(1))
        .single
        .apply()
        .getOrElse(0)
    }

  def hasRecentModerationEvent(user: User, duration: Duration, action: UserLogAction): Boolean =
    springDB.run {
      sql"""SELECT EXISTS (SELECT * FROM user_log
          WHERE userid=${user.id} AND action=${action.toDbName}::user_log_action AND action_date>${OffsetDateTime
          .now()
          .minus(duration)} AND userid!=action_userid)""".map(rs => rs.boolean(1)).single.apply().getOrElse(false)
    }

  def hasRecentSelfEvent(user: User, duration: Duration, action: UserLogAction): Boolean =
    springDB.run {
      sql"""SELECT EXISTS (SELECT * FROM user_log
          WHERE userid=${user.id} AND action=${action.toDbName}::user_log_action AND action_date>${OffsetDateTime
          .now()
          .minus(duration)} AND userid=action_userid)""".map(rs => rs.boolean(1)).single.apply().getOrElse(false)
    }

  def getRecentlyHasEvent(action: UserLogAction): Seq[Int] =
    springDB.run {
      sql"""SELECT userid FROM user_log
          WHERE action=${action
          .toDbName}::user_log_action AND action_date>CURRENT_TIMESTAMP - interval '3 days' ORDER BY action_date"""
        .map(rs => rs.int("userid"))
        .list
        .apply()
    }

  def logRegister(userid: Int, ip: String, userAgent: Int, language: Option[String])(using Transaction): Unit =
    val info: ju.Map[String, String] = (
      Map(UserLogDao.OptionIp -> ip, UserLogDao.OptionUserAgent -> userAgent.toString) ++
        language.map(lang => UserLogDao.OptionAcceptLanguage -> lang)
    ).asJava

    insertLog(userid, userid, UserLogAction.Register, info)

  def setCorrector(user: User, moderator: User)(using Transaction): Unit =
    insertLog(user.id, moderator.id, UserLogAction.SetCorrector, ju.Collections.emptyMap[String, String]())

  def unsetCorrector(user: User, moderator: User)(using Transaction): Unit =
    insertLog(user.id, moderator.id, UserLogAction.UnsetCorrector, ju.Collections.emptyMap[String, String]())

  private def toLogItem(rs: WrappedResultSet): UserLogItem =
    val options =
      rs.get[ju.Map[String, String]]("info")
        .asScala
        .view
        .mapValues(v =>
          if v == null then
            ""
          else
            v)
        .toMap
    UserLogItem(
      rs.int("id"),
      rs.int("userid"),
      rs.int("action_userid"),
      rs.timestamp("action_date").toInstant,
      UserLogAction.fromDbName(rs.string("action")),
      options)

object UserLogDao:
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
