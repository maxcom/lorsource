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

package ru.org.linux.auth

import com.typesafe.scalalogging.StrictLogging
import org.jasypt.exceptions.EncryptionOperationNotPossibleException
import org.jasypt.util.text.AES256TextEncryptor
import org.springframework.stereotype.Service
import ru.org.linux.spring.SiteConfig
import ru.org.linux.util.StringUtil

import java.sql.Timestamp
import java.time.Instant
import scala.util.Try

@Service
class SecretTokenService(siteConfig: SiteConfig) extends StrictLogging:
  private val base = siteConfig.getSecret

  def getResetCode(nick: String, email: String, tm: Timestamp): String =
    StringUtil.sha256hash(s"$base:$nick:$email:${tm.getTime.toString}:reset")

  def verifyResetCode(nick: String, email: String, tm: Timestamp, code: String): Boolean =
    val expected = s"$base:$nick:$email:${tm.getTime.toString}:reset"

    StringUtil.verifyHash(StringUtil.sha256hash(expected), code)

  def getActivationCode(nick: String, email: String): String = StringUtil.sha256hash(s"$base:$nick:$email")

  def verifyActivationCode(nick: String, email: String, code: String): Boolean =
    StringUtil.verifyHash(StringUtil.sha256hash(s"$base:$nick:$email"), code)

  def makeRegisterPermit(now: Instant = Instant.now()): String =
    val key = base
    val expiryMillis = now.plusMillis(3_600_000).toEpochMilli
    val message = s"permit:$expiryMillis"

    val textEncryptor = new AES256TextEncryptor
    textEncryptor.setPassword(key)
    textEncryptor.encrypt(message)

  def checkRegisterPermit(permit: String, now: Instant = Instant.now()): Boolean =
    val key = base
    val textEncryptor = new AES256TextEncryptor
    textEncryptor.setPassword(key)

    val decrypted = try
      textEncryptor.decrypt(permit)
    catch
      case _: EncryptionOperationNotPossibleException =>
        logger.debug("Invalid permit - decryption failed")
        return false

    decrypted.split(":", 2) match
      case Array("permit", date) =>
        Try(date.toLong) match
          case scala.util.Success(epochMillis) =>
            val decodedDate = Instant.ofEpochMilli(epochMillis)
            logger.debug(s"Decoded permit date: $decodedDate")
            decodedDate.isAfter(now)
          case scala.util.Failure(_) =>
            logger.warn(s"Invalid permit - date parse failed for: $date")
            false
      case other =>
        logger.warn(s"Invalid permit - decrypted: ${other.mkString}")
        false
