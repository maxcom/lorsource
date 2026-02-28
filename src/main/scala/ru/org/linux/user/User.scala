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

import org.springframework.validation.Errors
import ru.org.linux.util.StringUtil

import java.sql.{ResultSet, Timestamp}
import javax.annotation.Nullable
import scala.beans.BeanProperty
import scala.beans.BooleanBeanProperty

case class User(
  @BeanProperty nick: String,
  @BeanProperty id: Int,
  @BooleanBeanProperty canmod: Boolean,
  @BooleanBeanProperty candel: Boolean,
  @BooleanBeanProperty anonymous: Boolean,
  @BooleanBeanProperty corrector: Boolean,
  @BooleanBeanProperty blocked: Boolean,
  @BeanProperty password: String,
  score: Int,
  maxScore: Int,
  @Nullable @BeanProperty photo: String,
  @Nullable @BeanProperty email: String,
  @Nullable @BeanProperty fullName: String,
  @BeanProperty unreadEvents: Int,
  @Nullable @BeanProperty style: String,
  @Nullable @BeanProperty frozenUntil: Timestamp,
  @BeanProperty frozenBy: Int,
  @Nullable @BeanProperty freezingReason: String,
  @BooleanBeanProperty activated: Boolean
) {
  import User._

  def checkBlocked(errors: Errors): Unit = {
    if (blocked) {
      errors.reject(null, "Пользователь заблокирован")
    }

    if (!activated) {
      errors.reject(null, "Пользователь не активирован")
    }
  }

  def checkFrozen(errors: Errors): Unit = {
    if (isFrozen) {
      errors.reject(null, "Пользователь временно заморожен")
    }
  }

  def isFrozen: Boolean = {
    if (frozenUntil == null) {
      false
    } else {
      frozenUntil.after(new Timestamp(System.currentTimeMillis()))
    }
  }

  def isModerator: Boolean = canmod

  def isAdministrator: Boolean = candel

  def canCorrect: Boolean = corrector && !isFrozen

  def getActivationCode(base: String): String = User.getActivationCode(base, nick, email)

  def getActivationCodeWithEmail(base: String, email: String): String = StringUtil.md5hash(s"$base:$nick:$email")

  def getScore: Int = if (anonymous) 0 else score

  def getMaxScore: Int = if (anonymous) 0 else maxScore

  @deprecated("Use field access or User.getStars(score, maxScore, html) instead", since = "2026")
  def getStars: String = User.getStars(score, maxScore, true)

  def getStatus: String = {
    val text: String = if (score < ANONYMOUS_LEVEL_SCORE) {
      "анонимный"
    } else if (score < 100 && maxScore < 100) {
      "новый пользователь"
    } else {
      ""
    }

    if (maxScore >= 100 && text.isEmpty) {
      User.getStars(score, maxScore, true)
    } else if (maxScore >= 100) {
      s"$text ${User.getStars(score, maxScore, true)}"
    } else {
      text
    }
  }

  def isAnonymousScore: Boolean = anonymous || blocked || score < ANONYMOUS_LEVEL_SCORE

  def hasEmail: Boolean = email != null

  @Nullable
  def getName: String = fullName

  override def equals(o: Any): Boolean = o match {
    case user: User => id == user.id
    case _ => false
  }

  override def hashCode: Int = id
}

object User {
  final val ANONYMOUS_LEVEL_SCORE = 50

  def fromResultSet(rs: ResultSet): User = {
    val id = rs.getInt("id")
    val nick = rs.getString("nick")
    val canmod = rs.getBoolean("canmod")
    val candel = rs.getBoolean("candel")
    val corrector = rs.getBoolean("corrector")
    val activated = rs.getBoolean("activated")
    val blocked = rs.getBoolean("blocked")
    val score = rs.getInt("score")
    val maxScore = rs.getInt("max_score")
    val fullName = rs.getString("name")
    var pwd = rs.getString("passwd")
    if (pwd == null) {
      pwd = ""
    }
    val anonymous = pwd.isEmpty
    val password = pwd
    val photo = rs.getString("photo")
    val email = rs.getString("email")
    val unreadEvents = rs.getInt("unread_events")
    val style = rs.getString("style")
    val frozenUntil = rs.getTimestamp("frozen_until")
    val frozenBy = rs.getInt("frozen_by")
    val freezingReason = rs.getString("freezing_reason")

    new User(
      nick = nick,
      id = id,
      canmod = canmod,
      candel = candel,
      anonymous = anonymous,
      corrector = corrector,
      blocked = blocked,
      password = password,
      score = score,
      maxScore = maxScore,
      photo = photo,
      email = email,
      fullName = fullName,
      unreadEvents = unreadEvents,
      style = style,
      frozenUntil = frozenUntil,
      frozenBy = frozenBy,
      freezingReason = freezingReason,
      activated = activated
    )
  }

  def getActivationCode(base: String, nick: String, email: String): String =
    StringUtil.md5hash(s"$base:$nick:$email")

  private def getGreenStars(score: Int): Int = {
    var s = if (score < 0) 0 else score
    if (s >= 600) {
      s = 599
    }
    Math.floor(s / 100.0).toInt
  }

  private def getGreyStars(score: Int, maxScore: Int): Int = {
    var ms = if (maxScore < 0) 0 else maxScore
    if (ms < score) {
      ms = score
    }
    if (ms >= 600) {
      ms = 599
    }
    val stars = getGreenStars(score)
    Math.floor(ms / 100.0).toInt - stars
  }

  def getStars(score: Int, maxScore: Int, html: Boolean): String = {
    val out = new StringBuilder

    val stars = getGreenStars(score)
    val greyStars = getGreyStars(score, maxScore)

    if (html) {
      out.append("<span class=\"stars\">")
    }

    out.append("★" * Math.max(0, stars))
    out.append("☆" * Math.max(0, greyStars))

    if (html) {
      out.append("</span>")
    }

    out.toString
  }
}
