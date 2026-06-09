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

import com.typesafe.scalalogging.StrictLogging
import org.jasypt.util.password.BasicPasswordEncryptor
import org.jasypt.util.password.PasswordEncryptor
import org.springframework.stereotype.Repository
import ru.org.linux.markup.MarkupType
import ru.org.linux.scalikejdbc.{SpringDB, Transaction}
import ru.org.linux.scalikejdbc.Transaction.given
import ru.org.linux.section.SectionController.NonTech
import ru.org.linux.util.StringUtil
import ru.org.linux.util.URLUtil
import scalikejdbc.*

import java.sql.Timestamp
import java.time.Instant
import javax.annotation.Nullable
import jakarta.mail.internet.{AddressException, InternetAddress}

@Repository
class UserDao(springDB: SpringDB) extends StrictLogging:

  @throws(classOf[UserNotFoundException])
  def findUserId(nick: String): Int =
    if nick == null then
      throw new NullPointerException()

    if !StringUtil.checkLoginName(nick) then
      throw new UserNotFoundException("<invalid name>")

    val list = springDB.run(sql"SELECT id FROM users WHERE nick=${nick}".map(rs => rs.int("id")).list.apply())

    if list.isEmpty then
      throw new UserNotFoundException(nick)

    if list.size > 1 then
      throw new RuntimeException("list.size()>1 ???")

    list.head

  @throws(classOf[UserNotFoundException])
  def getUser(id: Int): User =
    springDB.run(
      sql"SELECT id,nick,score,max_score,candel,canmod,corrector,passwd,blocked,activated,photo,email,name,unread_events,frozen_until FROM users WHERE id=${id}"
        .map(rs => User.fromResultSet(rs.underlying))
        .single
        .apply()
        .getOrElse(throw new UserNotFoundException(id)))

  def getUserInfo(user: User): UserInfo =
    springDB.run(
      sql"SELECT url, town, lastlogin, regdate, freezing_reason, frozen_by, userinfo, userinfo_markup FROM users WHERE id=${user
          .id}".map(rs => UserInfo(rs.underlying)).single.apply().orNull)

  @Nullable
  def getBanInfoClass(user: User): BanInfo =
    springDB.run(
      sql"SELECT * FROM ban_info WHERE userid=${user.id}"
        .map(rs => BanInfo(rs.timestamp("bandate"), rs.string("reason"), rs.int("ban_by")))
        .single
        .apply()
        .orNull)

  def getExactCommentCount(user: User): Int =
    springDB.run(
      sql"SELECT count(*) as c FROM comments WHERE userid=${user.id} AND NOT deleted"
        .map(rs => rs.int("c"))
        .single
        .apply()
        .getOrElse(0))

  def getFirstAndLastCommentDate(user: User): (Timestamp, Timestamp) =
    springDB.run(
      sql"SELECT min(postdate) as first, max(postdate) as last FROM comments WHERE comments.userid=${user.id}"
        .map(rs => (rs.timestamp("first"), rs.timestamp("last")))
        .single
        .apply()
        .get)

  def getNewUserIds: Seq[Int] =
    springDB.run(sql"""SELECT id FROM users
            WHERE regdate IS NOT NULL
            AND regdate > CURRENT_TIMESTAMP - interval '3 days'
            ORDER BY regdate""".map(rs => rs.int("id")).list.apply())

  def getNewUsersByIP(
      @Nullable
      ip: String,
      @Nullable
      userAgent: Integer): Seq[(Int, Timestamp, Timestamp)] =
    if ip != null || userAgent != null then
      val ipFragment: SQLSyntax = Option(ip)
        .map(v => sqls"AND (info->'ip')::inet <<= ${v}::inet")
        .getOrElse(SQLSyntax.empty)
      val uaFragment: SQLSyntax = Option(userAgent)
        .map(v => sqls"AND (info->'user_agent')=${v}::text")
        .getOrElse(SQLSyntax.empty)

      springDB.run(
        sql"""SELECT users.id, lastlogin, regdate FROM users JOIN user_log ON users.id = user_log.userid
              WHERE regdate IS NOT NULL
              AND regdate > CURRENT_TIMESTAMP - interval '3 days'
              AND action='register'
              $ipFragment $uaFragment
              ORDER BY regdate DESC"""
          .map(rs => (rs.int("id"), rs.timestamp("regdate"), rs.timestamp("lastlogin")))
          .list
          .apply())
    else
      springDB.run(
        sql"""SELECT users.id, lastlogin, regdate FROM users
              WHERE regdate IS NOT NULL
              AND regdate > CURRENT_TIMESTAMP - interval '3 days'
              ORDER BY regdate DESC"""
          .map(rs => (rs.int("id"), rs.timestamp("regdate"), rs.timestamp("lastlogin")))
          .list
          .apply())

  def getFrozenUserIds: Seq[(Int, Option[Instant])] =
    springDB.run(
      sql"SELECT id, lastlogin FROM users WHERE frozen_until > CURRENT_TIMESTAMP AND NOT blocked ORDER BY frozen_until"
        .map(rs => (rs.int("id"), rs.timestampOpt("lastlogin").map(_.toInstant)))
        .list
        .apply())

  def getUnFrozenUserIds: Seq[(Int, Option[Instant])] =
    springDB.run(
      sql"""SELECT id, lastlogin FROM users
            WHERE frozen_until < CURRENT_TIMESTAMP AND frozen_until > CURRENT_TIMESTAMP - '3 days'::interval AND NOT blocked
            ORDER BY frozen_until"""
        .map(rs => (rs.int("id"), rs.timestampOpt("lastlogin").map(_.toInstant)))
        .list
        .apply())

  def removeTown(user: User)(using Transaction): Unit =
    sql"UPDATE users SET town=null WHERE id=${user.id}".update.apply()

  def removeUrl(user: User)(using Transaction): Unit = sql"UPDATE users SET url=null WHERE id=${user.id}".update.apply()

  def resetUserpic(user: User)(using Transaction): Boolean =
    sql"UPDATE users SET photo=null WHERE id=${user.id} AND photo IS NOT NULL".update.apply() > 0

  def setPhoto(user: User, photo: String)(using Transaction): Unit =
    sql"UPDATE users SET photo=${photo} WHERE id=${user.id}".update.apply()

  def updateUserInfo(userid: Int, text: String, markup: MarkupType)(using Transaction): Boolean =
    sql"UPDATE users SET userinfo=${text}, userinfo_markup=${markup
        .id} WHERE id=${userid} AND (userinfo IS DISTINCT FROM ${text} OR userinfo_markup IS DISTINCT FROM ${markup
        .id})".update.apply() > 0

  def resetUserInfo(userid: Int)(using Transaction): Boolean =
    sql"UPDATE users SET userinfo=null WHERE id=${userid} AND userinfo IS NOT NULL".update.apply() > 0

  def changeScore(id: Int, delta: Int)(using Transaction): Unit =
    val updated = sql"UPDATE users SET score=score+${delta} WHERE id=${id}".update.apply()
    if updated == 0 then
      throw new IllegalArgumentException(new UserNotFoundException(id))

  def setCorrector(user: User)(using Transaction): Unit =
    if !user.corrector then
      sql"UPDATE users SET corrector='t' WHERE id=${user.id}".update.apply()

  def unsetCorrector(user: User)(using Transaction): Unit =
    if user.corrector then
      sql"UPDATE users SET corrector='f' WHERE id=${user.id}".update.apply()

  def setPassword(user: User, password: String)(using Transaction): Unit =
    val encryptor: PasswordEncryptor = new BasicPasswordEncryptor()
    val encryptedPassword = encryptor.encryptPassword(password)
    sql"UPDATE users SET passwd=${encryptedPassword}, lostpwd = 'epoch' WHERE id=${user.id}".update.apply()

  def updateResetDate(user: User, now: Timestamp)(using Transaction): Unit =
    sql"UPDATE users SET lostpwd=${now} WHERE id=${user.id}".update.apply()

  def getResetDate(user: User): Timestamp =
    springDB.run(
      sql"SELECT lostpwd FROM users WHERE id=${user.id}".map(rs => rs.timestamp("lostpwd")).single.apply().get)

  def block(user: User, moderator: User, reason: String)(using Transaction): Unit =
    sql"UPDATE users SET blocked='t' WHERE id=${user.id}".update.apply()
    sql"INSERT INTO ban_info (userid, reason, ban_by) VALUES (${user.id}, ${reason}, ${moderator.id})".update.apply()

  def freezeUser(user: User, moderator: User, reason: String, until: Timestamp)(using Transaction): Unit =
    sql"UPDATE users SET frozen_until=${until}, frozen_by=${moderator.id}, freezing_reason=${reason} WHERE id=${user
        .id}".update.apply()

  def score50(user: User)(using Transaction): Boolean =
    sql"UPDATE users SET score=GREATEST(score, 50), max_score=GREATEST(max_score, 50) WHERE id=${user.id} AND score<50"
      .update
      .apply() > 0

  def unblock(user: User)(using Transaction): Unit =
    sql"UPDATE users SET blocked='f' WHERE id=${user.id}".update.apply()
    sql"DELETE FROM ban_info WHERE userid=${user.id}".update.apply()

  def getModerators: Seq[(Int, Option[Instant])] =
    springDB.run(
      sql"SELECT id, lastlogin FROM users WHERE canmod ORDER BY id"
        .map(rs => (rs.int("id"), rs.timestampOpt("lastlogin").map(_.toInstant)))
        .list
        .apply())

  def getCorrectors: Seq[(Int, Option[Instant])] =
    springDB.run(
      sql"SELECT id, lastlogin FROM users WHERE corrector ORDER BY id"
        .map(rs => (rs.int("id"), rs.timestampOpt("lastlogin").map(_.toInstant)))
        .list
        .apply())

  def getByEmail(email: String, searchBlocked: Boolean): Int =
    try
      val parsedAddress = new InternetAddress(email.toLowerCase, true)
      val address = parsedAddress.getAddress.toLowerCase

      if searchBlocked then
        springDB.run(
          sql"SELECT id FROM users WHERE normalize_email(email)=normalize_email(${address}) ORDER BY blocked ASC, id DESC LIMIT 1"
            .map(rs => rs.int("id"))
            .single
            .apply()
            .getOrElse(0))
      else
        springDB.run(
          sql"SELECT id FROM users WHERE normalize_email(email)=normalize_email(${address}) AND NOT blocked ORDER BY id DESC LIMIT 1"
            .map(rs => rs.int("id"))
            .single
            .apply()
            .getOrElse(0))
    catch
      case _: AddressException =>
        0

  def getAllByEmail(email: String): Seq[Int] =
    if email == null || email.isEmpty then
      Seq.empty
    else
      springDB.run(
        sql"SELECT id FROM users WHERE normalize_email(email)=normalize_email(${email.toLowerCase}) ORDER BY id DESC"
          .map(rs => rs.int("id"))
          .list
          .apply())

  def activateUser(user: User)(using Transaction): Unit =
    sql"UPDATE users SET activated='t' WHERE id=${user.id}".update.apply()

  def updateName(user: User, name: String)(using Transaction): Boolean =
    sql"UPDATE users SET name=${name} WHERE id=${user.id} AND name IS DISTINCT FROM ${name}".update.apply() > 0

  def updateUrl(user: User, url: String)(using Transaction): Boolean =
    sql"UPDATE users SET url=${url} WHERE id=${user.id} AND url IS DISTINCT FROM ${url}".update.apply() > 0

  def updateTown(user: User, town: String)(using Transaction): Boolean =
    sql"UPDATE users SET town=${town} WHERE id=${user.id} AND town IS DISTINCT FROM ${town}".update.apply() > 0

  def setNewEmail(user: User, newEmail: String)(using Transaction): Unit =
    sql"UPDATE users SET new_email=${newEmail} WHERE id=${user.id}".update.apply()

  def createUser(name: String, nick: String, password: String, url: String, mail: InternetAddress, town: String)(using
      Transaction): Int =
    val encryptor: PasswordEncryptor = new BasicPasswordEncryptor()
    val fixedUrl = Option(url).map(URLUtil.fixURL).orNull

    val userid = sql"select nextval('s_uid') as userid".map(rs => rs.int("userid")).single.apply().get
    sql"""INSERT INTO users
          (id, name, nick, passwd, url, email, town, score, max_score, regdate, userinfo_markup)
          VALUES (${userid}, ${name}, ${nick}, ${encryptor.encryptPassword(password)}, ${fixedUrl}, ${mail
        .getAddress}, ${town}, 45, 45, current_timestamp, ${Profile.DEFAULT.formatMode.id})""".update.apply()
    userid

  def isUserExists(nick: String): Boolean =
    springDB.run(
      sql"SELECT count(*) as c FROM users WHERE nick=${nick}".map(rs => rs.int("c")).single.apply().getOrElse(0) > 0)

  def hasSimilarUsers(nick: String): Boolean =
    springDB.run(
      sql"""SELECT count(*) FROM users
            WHERE score>=200 AND lastlogin>CURRENT_TIMESTAMP-'3 years'::interval
            AND levenshtein_less_equal(lower(nick), ${nick.toLowerCase}, 1)<=1"""
        .map(rs => rs.int("count"))
        .single
        .apply()
        .getOrElse(0) > 0)

  @Nullable
  def getNewEmail(user: User): String =
    springDB.run(
      sql"SELECT new_email FROM users WHERE id=${user.id}".map(rs => rs.string("new_email")).single.apply().orNull)

  def acceptNewEmail(user: User, newEmail: String)(using Transaction): Unit =
    sql"UPDATE users SET email=${newEmail}, new_email=null WHERE id=${user.id}".update.apply()

  def updateLastlogin(user: User, force: Boolean): Boolean =
    if force then
      springDB.run(sql"UPDATE users SET lastlogin=CURRENT_TIMESTAMP WHERE id=${user.id}".update.apply())
      true
    else
      springDB.run(
        sql"UPDATE users SET lastlogin=CURRENT_TIMESTAMP WHERE id=${user
            .id} AND CURRENT_TIMESTAMP-lastlogin > '1 hour'::interval".update.apply()) > 0

  def unloginAllSessions(user: User): Unit =
    springDB.run(sql"UPDATE users SET token_generation=token_generation+1 WHERE id=${user.id}".update.apply())

  def getTokenGeneration(nick: String): Int =
    springDB.run(
      sql"SELECT token_generation FROM users WHERE nick=${nick}"
        .map(rs => rs.int("token_generation"))
        .single
        .apply()
        .get)

  def countUnactivated(ip: String): Int =
    springDB.run(sql"""SELECT count(*) FROM users JOIN user_log ON users.id = user_log.userid
            WHERE NOT activated AND NOT blocked AND regdate>CURRENT_TIMESTAMP-'1 day'::interval
            AND action='register' AND info->'ip'=${ip}""".map(rs => rs.int("count")).single.apply().getOrElse(0))

  def getUsersWithAgent(ip: Option[String], userAgent: Option[Int], limit: Int): Seq[UserAndAgent] =
    val ipFragment: SQLSyntax = ip.map(v => sqls"AND c.postip <<= ${v}::inet").getOrElse(SQLSyntax.empty)
    val uaFragment: SQLSyntax = userAgent.map(v => sqls"AND c.ua_id=${v}").getOrElse(SQLSyntax.empty)

    springDB.run(sql"""SELECT MAX(c.postdate) AS lastdate, u.nick, c.ua_id, ua.name AS user_agent, blocked
            FROM (SELECT ua_id, userid, postdate, postip FROM comments UNION ALL SELECT ua_id, userid, postdate, postip FROM topics) c
            LEFT JOIN user_agents ua ON c.ua_id = ua.id
            JOIN users u ON c.userid = u.id
            WHERE c.postdate>CURRENT_TIMESTAMP - '1 year'::interval
            $ipFragment $uaFragment
            GROUP BY u.nick, blocked, c.ua_id, ua.name
            ORDER BY MAX(c.postdate) DESC, u.nick, ua.name
            LIMIT ${limit}""".map(toUserAndAgent).list.apply())

  private val toUserAndAgent =
    (rs: WrappedResultSet) =>
      UserAndAgent(rs.timestamp("lastdate"), rs.string("nick"), rs.string("user_agent"), rs.boolean("blocked"))

  def updateScore()(using Transaction): Unit =
    sql"""update users set score=score+1
            where id in (
              select distinct comments.userid from comments, topics
              where comments.postdate>CURRENT_TIMESTAMP-'2 days'::interval
              and topics.id=comments.topic and
              not topics.groupid in (${NonTech}) and
              not comments.deleted and not topics.deleted and not topics.notop
            )""".update.apply()
    updateMaxScore()

  def updateMaxScore()(using Transaction): Unit =
    sql"update users set max_score=score where score>max_score".update.apply()

  def blockLowScoreUsers(): Unit =
    springDB.run(
      sql"update users set blocked='t' where id in (select id from users where score<-50 and nick!='anonymous' and max_score<150 and not blocked)"
        .update
        .apply())
    springDB.run(
      sql"update users set blocked='t' where id in (select id from users where score<-50 and nick!='anonymous' and max_score<150 and blocked is null)"
        .update
        .apply())

  def deleteInactivatedAccounts()(using Transaction): (Int, Int) =
    sql"delete from user_events where userid in (select id from users where not activated and not blocked and regdate<CURRENT_TIMESTAMP-'12 hours'::interval)"
      .update
      .apply()
    sql"delete from topic_users_notified where userid in (select id from users where not activated and not blocked and regdate<CURRENT_TIMESTAMP-'12 hours'::interval)"
      .update
      .apply()
    val deleted =
      sql"delete from users where not activated and not blocked and regdate<CURRENT_TIMESTAMP-'12 hours'::interval"
        .update
        .apply()
    sql"delete from ban_info where userid in (select id from users where not activated and regdate<CURRENT_TIMESTAMP-'30 days'::interval)"
      .update
      .apply()
    sql"delete from user_events where userid in (select id from users where not activated and regdate<CURRENT_TIMESTAMP-'30 days'::interval)"
      .update
      .apply()
    sql"delete from topic_users_notified where userid in (select id from users where not activated and regdate<CURRENT_TIMESTAMP-'30 days'::interval)"
      .update
      .apply()
    val deletedBlocked = sql"delete from users where not activated and regdate<CURRENT_TIMESTAMP-'30 days'::interval"
      .update
      .apply()
    (deleted, deletedBlocked)
