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
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.user.UserInvitesDao.ValidDays
import scalikejdbc.*

import java.security.SecureRandom
import java.sql.Timestamp
import java.time.{Instant, ZonedDateTime}
import java.util.Base64

@Repository
class UserInvitesDao(springDB: SpringDB):
  def createInvite(owner: User, email: String): (String, Instant) =
    val random = SecureRandom()
    val value = new Array[Byte](16)
    random.nextBytes(value)
    val inviteCode = Base64.getEncoder.encodeToString(value)
    val validUntil = ZonedDateTime.now().plusDays(ValidDays).toInstant

    springDB.run:
      sql"insert into user_invites (invite_code, owner, valid_until, email) values ($inviteCode, ${owner
          .id}, ${Timestamp.from(validUntil)}, $email)".update.apply()

    (inviteCode, validUntil)

  def emailFromValidInvite(inviteCode: String): Option[String] =
    springDB.run:
      sql"""select email from user_invites where invite_code=$inviteCode and
            invited_user is null and valid_until>CURRENT_TIMESTAMP and owner not in (select id from users where blocked)"""
        .map(rs => rs.string("email"))
        .single
        .apply()

  def ownerOfInvite(inviteCode: String): Option[Int] =
    springDB.run:
      sql"select owner from user_invites where invite_code=$inviteCode".map(rs => rs.int("owner")).single.apply()

  def markUsed(token: String, newUserId: Int): Boolean =
    springDB.run:
      sql"update user_invites set invited_user=$newUserId where invite_code=$token and invited_user is null and valid_until>CURRENT_TIMESTAMP"
        .update
        .apply() > 0

  def countValidInvites(user: User): (Int, Int) =
    springDB.run:
      sql"select count(*), count(*) filter (where owner=${user
          .id}) from user_invites where valid_until > CURRENT_TIMESTAMP"
        .map(rs => (rs.int(1), rs.int(2)))
        .single
        .apply()
        .get

  def getAllInvitedUsers(user: User): Seq[Int] =
    springDB.run:
      sql"select invited_user from user_invites where owner = ${user
          .id} and invited_user is not null order by issue_date".map(rs => rs.int("invited_user")).list.apply()

object UserInvitesDao:
  val ValidDays = 3
