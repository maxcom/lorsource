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
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import ru.org.linux.util.StringUtil
import ru.org.linux.util.URLUtil

import java.sql.Timestamp
import java.time.Instant
import javax.annotation.Nullable
import javax.mail.internet.{AddressException, InternetAddress}
import javax.sql.DataSource
import scala.jdk.CollectionConverters._

@Repository
class UserDao(ds: DataSource) extends StrictLogging {
  private val jdbcTemplate = new JdbcTemplate(ds)
  private val namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate.javaTemplate)

  private val queryChangeScore = "UPDATE users SET score=score+? WHERE id=?"
  private val queryUserById = "SELECT id,nick,score,max_score,candel,canmod,corrector,passwd,blocked,activated,photo,email,name,unread_events,style,frozen_until FROM users where id=?"
  private val queryUserIdByNick = "SELECT id FROM users where nick=?"
  private val updateUserStyle = "UPDATE users SET style=? WHERE id=?"

  private val queryBanInfoClass = "SELECT * FROM ban_info WHERE userid=?"

  private val queryCommentStat = "SELECT count(*) as c FROM comments WHERE userid=? AND not deleted"
  private val queryCommentDates = "SELECT min(postdate) as first,max(postdate) as last FROM comments WHERE comments.userid=?"

  @throws(classOf[UserNotFoundException])
  def findUserId(nick: String): Int = {
    if (nick == null) {
      throw new NullPointerException()
    }

    if (!StringUtil.checkLoginName(nick)) {
      throw new UserNotFoundException("<invalid name>")
    }

    val list = jdbcTemplate.queryForSeq[Int](queryUserIdByNick, nick)

    if (list.isEmpty) {
      throw new UserNotFoundException(nick)
    }

    if (list.size > 1) {
      throw new RuntimeException("list.size()>1 ???")
    }

    list.head
  }

  /**
   * Загружает пользователя из БД не используя кеш (всегда обновляет кеш).
   * Метод используется там, где нужно проверить права пользователя, совершить какой-то
   * update или получить самый свежий варинт из БД. В остальных случаях нужно
   * использовать метод getUserCached()
   *
   * @param id идентификатор пользователя
   * @return объект пользователя
   * @throws UserNotFoundException если пользователь с таким id не найден
   */
  @throws(classOf[UserNotFoundException])
  def getUser(id: Int): User = {
    val list = jdbcTemplate.queryAndMap(queryUserById, id) { (rs, _) =>
      User.fromResultSet(rs)
    }

    if (list.isEmpty) {
      throw new UserNotFoundException(id)
    }

    if (list.size > 1) {
      throw new RuntimeException("list.size()>1 ???")
    }

    list.head
  }

  /**
   * Получить информацию о пользователе
   * @param user пользователь
   * @return информация
   */
  def getUserInfo(user: User): UserInfo =
    jdbcTemplate.queryForObjectAndMap(
      "SELECT url, town, lastlogin, regdate, freezing_reason, frozen_by, userinfo FROM users WHERE id=?",
      user.id
    ) { (rs, _) =>
      UserInfo(rs)
    }.orNull

  /**
   * Получить информацию о бане
   * @param user пользователь
   * @return информация о бане :-)
   */
  @Nullable
  def getBanInfoClass(user: User): BanInfo = {
    val infoList = jdbcTemplate.queryAndMap(queryBanInfoClass, user.id) { (rs, _) =>
      val date = rs.getTimestamp("bandate")
      val reason = rs.getString("reason")
      val moderator = rs.getInt("ban_by")
      BanInfo(date, reason, moderator)
    }

    if (infoList.isEmpty) {
      null
    } else {
      infoList.head
    }
  }

  def getExactCommentCount(user: User): Int =
    try {
      jdbcTemplate.queryForObject[Int](queryCommentStat, user.id).get
    } catch {
      case _: EmptyResultDataAccessException => 0
    }

  def getFirstAndLastCommentDate(user: User): (Timestamp, Timestamp) =
    jdbcTemplate.queryForObjectAndMap(queryCommentDates, user.id) { (rs, _) =>
      (rs.getTimestamp("first"), rs.getTimestamp("last"))
    }.get

  /**
   * Получить список новых пользователей зарегистрирововавшихся за последние 3(три) дня
   * @return список новых пользователей
   */
  def getNewUserIds: Seq[Int] =
    jdbcTemplate.queryForSeq[Int](
      "SELECT id FROM users where " +
        "regdate IS NOT null " +
        "AND regdate > CURRENT_TIMESTAMP - interval '3 days' " +
        "ORDER BY regdate"
    )

  def getNewUsersByIP(@Nullable ip: String, @Nullable userAgent: Integer): Seq[(Int, Timestamp, Timestamp)] = {
    if (ip != null || userAgent != null) {
      val params = new java.util.HashMap[String, Any]()
      params.put("ip", ip)
      params.put("user_agent", userAgent)

      val ipQuery = if (ip != null) "AND (info->'ip')::inet <<= :ip::inet " else ""
      val userAgentQuery = if (userAgent != null) "AND (info->'user_agent')=:user_agent::text " else ""

      val result = namedJdbcTemplate.query(
        "SELECT users.id, lastlogin, regdate from users join user_log on users.id = user_log.userid WHERE " +
          "regdate IS NOT null " +
          "AND regdate > CURRENT_TIMESTAMP - interval '3 days' " +
          "and action='register' " + ipQuery + userAgentQuery +
          "ORDER BY regdate desc",
        params,
        new RowMapper[(Int, Timestamp, Timestamp)]() {
          override def mapRow(rs: java.sql.ResultSet, rowNum: Int): (Int, Timestamp, Timestamp) =
            (rs.getInt("id"), rs.getTimestamp("regdate"), rs.getTimestamp("lastlogin"))
        }
      )
      result.asScala.toSeq
    } else {
      jdbcTemplate.queryAndMap(
        "SELECT users.id, lastlogin, regdate from users WHERE " +
          "regdate IS NOT null " +
          "AND regdate > CURRENT_TIMESTAMP - interval '3 days' " +
          "ORDER BY regdate desc"
      ) { (rs, _) =>
        (rs.getInt("id"), rs.getTimestamp("regdate"), rs.getTimestamp("lastlogin"))
      }
    }
  }

  def getFrozenUserIds: Seq[(Int, Option[Instant])] =
    jdbcTemplate.queryAndMap(
      "SELECT id, lastlogin FROM users where " +
        "frozen_until > CURRENT_TIMESTAMP and not blocked " +
        "ORDER BY frozen_until"
    ) { (rs, _) =>
      (
        rs.getInt("id"),
        Option(rs.getTimestamp("lastlogin")).map(_.toInstant)
      )
    }

  def getUnFrozenUserIds: Seq[(Int, Option[Instant])] =
    jdbcTemplate.queryAndMap(
      "SELECT id, lastlogin FROM users where " +
        "frozen_until < CURRENT_TIMESTAMP and frozen_until > CURRENT_TIMESTAMP - '3 days'::interval and not blocked " +
        "ORDER BY frozen_until"
    ) { (rs, _) =>
      (
        rs.getInt("id"),
        Option(rs.getTimestamp("lastlogin")).map(_.toInstant)
      )
    }

  def removeTown(user: User): Unit =
    jdbcTemplate.update("UPDATE users SET town=null WHERE id=?", user.id)

  def removeUrl(user: User): Unit =
    jdbcTemplate.update("UPDATE users SET url=null WHERE id=?", user.id)

  /**
   * Отчистка userpicture пользователя, с обрезанием шкворца если удаляет модератор
   * @param user пользователь у которого чистят
   */
  def resetUserpic(user: User): Boolean =
    jdbcTemplate.update("UPDATE users SET photo=null WHERE id=? and photo is not null", user.id) > 0

  /**
   * Обновление userpic-а пользовтаеля
   * @param user пользователь
   * @param photo userpick
   */
  def setPhoto(user: User, photo: String): Unit =
    jdbcTemplate.update("UPDATE users SET photo=? WHERE id=?", photo, user.id)

  /**
   * Обновление дополнительной информации пользователя
   * @param userid пользователь
   * @param text текст дополнительной информации
   */
  def updateUserInfo(userid: Int, text: String): Boolean =
    jdbcTemplate.update("UPDATE users SET userinfo=? where id=? AND userinfo is distinct from ?", text, userid, text) > 0

  /**
   * Изменение шкворца пользовтаеля, принимает отрицательные и положительные значения
   * не накладывает никаких ограни используется в купэ с другими
   *чений на параметры методами и не является транзакцией
   * @param id id пользователя
   * @param delta дельта на которую меняется шкворец
   */
  def changeScore(id: Int, delta: Int): Unit =
    if (jdbcTemplate.update(queryChangeScore, delta, id) == 0) {
      throw new IllegalArgumentException(new UserNotFoundException(id))
    }

  /**
   * Смена признака корректора для пользователя
   * @param user пользователь у которого меняется признак корректора
   */
  def setCorrector(user: User): Unit =
    if (!user.isCorrector()) {
      jdbcTemplate.update("UPDATE users SET corrector='t' WHERE id=?", user.id)
    }

  /**
   * Смена признака корректора для пользователя
   * @param user пользователь у которого меняется признак корректора
   */
  def unsetCorrector(user: User): Unit =
    if (user.isCorrector()) {
      jdbcTemplate.update("UPDATE users SET corrector='f' WHERE id=?", user.id)
    }

  /**
   * Смена стиля\темы пользователя
   * @param user пользователь у которого меняется стиль\тема
   */
  def setStyle(user: User, theme: String): Unit =
    jdbcTemplate.update(updateUserStyle, theme, user.id)

  def setPassword(user: User, password: String): Unit = {
    val encryptor: PasswordEncryptor = new BasicPasswordEncryptor()
    val encryptedPassword = encryptor.encryptPassword(password)

    jdbcTemplate.update("UPDATE users SET passwd=?, lostpwd = 'epoch' WHERE id=?",
      encryptedPassword, user.id)
  }

  def updateResetDate(user: User, now: Timestamp): Unit =
    jdbcTemplate.update("UPDATE users SET lostpwd=? WHERE id=?", now, user.id)

  def getResetDate(user: User): Timestamp =
    jdbcTemplate.queryForObject[Timestamp]("SELECT lostpwd FROM users WHERE id=?", user.id).get

  /**
   * Блокировка пользователя
   * @param user блокируемый пользователь
   * @param moderator модератор который блокирует пользователя
   * @param reason причина блокировки
   */
  def block(user: User, moderator: User, reason: String): Unit = {
    jdbcTemplate.update("UPDATE users SET blocked='t' WHERE id=?", user.id)
    jdbcTemplate.update("INSERT INTO ban_info (userid, reason, ban_by) VALUES (?, ?, ?)", user.id, reason, moderator.id)
  }

  /**
   * Заморозка и разморозка пользователя.
   * @param user пользователь для совершения над ним действия
   * @param moderator модератор который это делает
   * @param reason причина заморозки
   * @param until до каких пор ему быть замороженным, если указано прошлое,
   *              то пользователь будет разморожен
   */
  def freezeUser(user: User, moderator: User, reason: String, until: Timestamp): Unit =
    jdbcTemplate.update(
      "UPDATE users SET frozen_until=?,frozen_by=?,freezing_reason=? WHERE id=?",
      until, Integer.valueOf(moderator.id), reason, Integer.valueOf(user.id))

  /**
   * Ставим score=50 если он меньше
   *
   * @param user кому ставим score
   */
  def score50(user: User): Boolean =
    jdbcTemplate.update("UPDATE users SET score=GREATEST(score, 50), max_score=GREATEST(max_score, 50) WHERE id=? AND score<50", user.id) > 0

  /**
   * Разблокировка пользователя
   * @param user разблокируемый пользователь
   */
  def unblock(user: User): Unit = {
    jdbcTemplate.update("UPDATE users SET blocked='f' WHERE id=?", user.id)
    jdbcTemplate.update("DELETE FROM ban_info WHERE userid=?", user.id)
  }

  def getModerators: Seq[(Int, Option[Instant])] =
    jdbcTemplate.queryAndMap(
      "SELECT id, lastlogin FROM users where canmod ORDER BY id"
    ) { (rs, _) =>
      (
        rs.getInt("id"),
        Option(rs.getTimestamp("lastlogin")).map(_.toInstant)
      )
    }

  def getCorrectors: Seq[(Int, Option[Instant])] =
    jdbcTemplate.queryAndMap(
      "SELECT id, lastlogin FROM users where corrector ORDER BY id"
    ) { (rs, _) =>
      (
        rs.getInt("id"),
        Option(rs.getTimestamp("lastlogin")).map(_.toInstant)
      )
    }

  def getByEmail(email: String, searchBlocked: Boolean): Int =
    try {
      val parsedAddress = new InternetAddress(email.toLowerCase, true)

      if (searchBlocked) {
        jdbcTemplate.queryForObject[Int](
          "SELECT id FROM users WHERE normalize_email(email)=normalize_email(?) ORDER BY blocked ASC, id DESC LIMIT 1",
          parsedAddress.getAddress.toLowerCase
        ).get
      } else {
        jdbcTemplate.queryForObject[Int](
          "SELECT id FROM users WHERE normalize_email(email)=normalize_email(?) AND NOT blocked ORDER BY id DESC LIMIT 1",
          parsedAddress.getAddress.toLowerCase
        ).get
      }
    } catch {
      case _: EmptyResultDataAccessException | _: AddressException => 0
    }

  def getAllByEmail(email: String): Seq[Int] =
    if (email == null || email.isEmpty) {
      Seq.empty
    } else {
      jdbcTemplate.queryForSeq[Int](
        "SELECT id FROM users WHERE normalize_email(email)=normalize_email(?) ORDER BY id DESC",
        email.toLowerCase
      )
    }

  def activateUser(user: User): Unit =
    jdbcTemplate.update("UPDATE users SET activated='t' WHERE id=?", user.id)

  def updateName(user: User, name: String): Boolean =
    jdbcTemplate.update("UPDATE users SET name=? WHERE id=? and name is distinct from ?", name, user.id, name) > 0

  def updateUrl(user: User, url: String): Boolean =
    jdbcTemplate.update("UPDATE users SET url=? WHERE id=? and url is distinct from ?", url, user.id, url) > 0

  def updateTown(user: User, town: String): Boolean =
    jdbcTemplate.update("UPDATE users SET town=? WHERE id=? AND town is distinct from ?", town, user.id, town) > 0

  def setNewEmail(user: User, newEmail: String): Unit =
    jdbcTemplate.update("UPDATE users SET new_email=? WHERE id=?", newEmail, user.id)

  def createUser(
    name: String,
    nick: String,
    password: String,
    url: String,
    mail: InternetAddress,
    town: String
  ): Int = {
    val encryptor: PasswordEncryptor = new BasicPasswordEncryptor()

    val userid = jdbcTemplate.queryForObject[Int]("select nextval('s_uid') as userid").get

    jdbcTemplate.update(
      "INSERT INTO users " +
        "(id, name, nick, passwd, url, email, town, score, max_score,regdate) " +
        "VALUES (?,?,?,?,?,?,?,45,45,current_timestamp)",
      Integer.valueOf(userid),
      name,
      nick,
      encryptor.encryptPassword(password),
      if (url == null) null else URLUtil.fixURL(url),
      mail.getAddress,
      town
    )

    userid
  }

  def isUserExists(nick: String): Boolean = {
    val c = jdbcTemplate.queryForObject[Int]("SELECT count(*) as c FROM users WHERE nick=?", nick).get
    c > 0
  }

  def hasSimilarUsers(nick: String): Boolean = {
    val c = jdbcTemplate.queryForObject[Int](
      "SELECT count(*) FROM users WHERE " +
        "score>=200 AND lastlogin>CURRENT_TIMESTAMP-'3 years'::INTERVAL " +
        "AND levenshtein_less_equal(lower(nick), ?, 1)<=1",
      nick.toLowerCase
    ).get
    c > 0
  }

  @Nullable
  def getNewEmail(user: User): String =
    jdbcTemplate.queryForObject[String]("SELECT new_email FROM users WHERE id=?", user.id).orNull

  def acceptNewEmail(user: User, newEmail: String): Unit =
    jdbcTemplate.update("UPDATE users SET email=?, new_email=null WHERE id=?", newEmail, user.id)

  /**
   * Update lastlogin time in database
   * @param user logged user
   * @throws java.sql.SQLException on database failure
   */
  def updateLastlogin(user: User, force: Boolean): Boolean =
    if (force) {
      jdbcTemplate.update("UPDATE users SET lastlogin=CURRENT_TIMESTAMP WHERE id=?", user.id)
      true
    } else {
      jdbcTemplate.update("UPDATE users SET lastlogin=CURRENT_TIMESTAMP WHERE id=? AND CURRENT_TIMESTAMP-lastlogin > '1 hour'::interval", user.id) > 0
    }

  /**
   * Sign out from all sessions
   * @param user logged user
   */
  def unloginAllSessions(user: User): Unit =
    jdbcTemplate.update("UPDATE users SET token_generation=token_generation+1 WHERE id=?", user.id)

  def getTokenGeneration(nick: String): Int =
    jdbcTemplate.queryForObject[Int]("SELECT token_generation FROM users WHERE nick=?", nick).get

  def countUnactivated(ip: String): Int =
    jdbcTemplate.queryForObject[Int](
      "select count(*) from users join user_log on users.id = user_log.userid " +
        "where not activated and not blocked and regdate>CURRENT_TIMESTAMP-'1 day'::interval " +
        "and action='register' and info->'ip'=?",
      ip
    ).get

  def getUsersWithAgent(ip: Option[String], userAgent: Option[Int], limit: Int): Seq[UserAndAgent] = {
    val params = new java.util.HashMap[String, Any]()
    ip.foreach(params.put("ip", _))
    userAgent.foreach(params.put("userAgent", _))
    params.put("limit", Integer.valueOf(limit))

    val ipQuery = ip.map(_ => "AND c.postip <<= :ip::inet ").getOrElse("")
    val userAgentQuery = userAgent.map(_ => "AND c.ua_id=:userAgent ").getOrElse("")

    val mapper = new RowMapper[UserAndAgent]() {
      override def mapRow(rs: java.sql.ResultSet, rowNum: Int): UserAndAgent =
        UserAndAgent(
          rs.getTimestamp("lastdate"),
          rs.getString("nick"),
          rs.getString("user_agent"),
          Boolean.box(rs.getBoolean("blocked"))
        )
    }

    namedJdbcTemplate.query(
      "SELECT MAX(c.postdate) AS lastdate, u.nick, c.ua_id, ua.name AS user_agent, blocked " +
        "FROM (SELECT ua_id, userid, postdate, postip FROM comments UNION ALL SELECT ua_id, userid, postdate, postip FROM topics) c " +
        "LEFT JOIN user_agents ua ON c.ua_id = ua.id " +
        "JOIN users u ON c.userid = u.id " +
        "WHERE c.postdate>CURRENT_TIMESTAMP - '1 year'::interval " +
        ipQuery +
        userAgentQuery +
        "GROUP BY u.nick, blocked, c.ua_id, ua.name " +
        "ORDER BY MAX(c.postdate) DESC, u.nick, ua.name " +
        "LIMIT :limit",
      params,
      mapper
    ).asScala.toSeq
  }
}
