/*
 * Copyright 1998-2022 Linux.org.ru
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

import org.joda.time.DateTime
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import ru.org.linux.user.UserInvitesDao.ValidDays

import java.security.SecureRandom
import java.sql.Timestamp
import java.util.Base64
import javax.sql.DataSource
import scala.jdk.CollectionConverters._

@Repository
class UserInvitesDao(ds: DataSource) {
  private val insert = {
    val i = new SimpleJdbcInsert(ds)
    i.setTableName("user_invites")
    i.setColumnNames(Seq("invite_code", "owner", "valid_until", "email").asJava)
    i
  }

  def createInvite(owner: User, email: String): (String, DateTime) = {
    val random = new SecureRandom

    val value = new Array[Byte](16)
    random.nextBytes(value)

    val inviteCode = Base64.getEncoder.encodeToString(value)

    val validUntil = DateTime.now().plusDays(ValidDays)

    insert.execute(Map(
      "invite_code" -> inviteCode,
      "owner" -> owner.getId,
      "valid_until" -> new Timestamp(validUntil.getMillis),
      "email" -> email
    ).asJava)

    (inviteCode, validUntil)
  }
}

object UserInvitesDao {
  val ValidDays = 3;
}
