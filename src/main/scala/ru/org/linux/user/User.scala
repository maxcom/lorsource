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

import ru.org.linux.util.StringUtil

import java.sql.{ResultSet, Timestamp}
import java.time.{LocalDate, Month}
import javax.annotation.Nullable
import scala.beans.{BeanProperty, BooleanBeanProperty}

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
  @BooleanBeanProperty activated: Boolean,
  @Nullable @BeanProperty mostPopularReaction: String,
  @BeanProperty mostPopularCount: Int
) {
  import User.*

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

  def getStars: String = {
    val today = LocalDate.now

    // Check if Month is APRIL and Day is 1
    if ((today.getMonth == Month.APRIL) && (today.getDayOfMonth == 1)) {
      val out = new StringBuilder

      out.append("<span class=\"stars\">")

      if (mostPopularReaction != null) {
        val stars = getGreenStars(mostPopularCount)

        if mostPopularReaction == "\u2615\u2615" then
          out.append("\u2615" * Math.max(0, stars))
        else
          out.append(mostPopularReaction * Math.max(0, stars))
      }

      out.append("</span>")
      
      out.toString
    } else {
      User.getStars(score, maxScore = maxScore, html = true)
    }
  }

  def getStatus: String = {
    val text: String = if (score < AnonymousLevelScore) {
      "анонимный"
    } else if (score < 100 && maxScore < 100) {
      "новый пользователь"
    } else {
      ""
    }

    if (maxScore >= 100 && text.isEmpty) {
      getStars
    } else if (maxScore >= 100) {
      s"$text ${getStars}"
    } else {
      text
    }
  }

  def isAnonymousScore: Boolean = anonymous || blocked || score < AnonymousLevelScore

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
  private val AnonymousLevelScore = 50

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
      activated = activated,
      mostPopularReaction = rs.getString("most_popular_reaction"),
      mostPopularCount = rs.getInt("most_popular_count")
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
