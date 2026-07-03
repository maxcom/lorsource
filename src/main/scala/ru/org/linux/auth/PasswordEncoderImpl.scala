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

import org.jasypt.exceptions.EncryptionOperationNotPossibleException
import org.jasypt.util.password.BasicPasswordEncryptor
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

import java.nio.charset.StandardCharsets

@Component
class PasswordEncoderImpl extends PasswordEncoder:
  private val legacyEncryptor = new BasicPasswordEncryptor
  private val logger = LoggerFactory.getLogger(classOf[PasswordEncoderImpl])
  private val bcryptEncoder = new BCryptPasswordEncoder

  // BCrypt only uses the first 72 bytes of the password and throws
  // IllegalArgumentException for anything longer. Truncate to a 72-byte prefix
  // (on a UTF-8 character boundary) so long passwords authenticate instead of
  // crashing the login flow. The limit is a property of bcrypt itself, so
  // existing hashes are unaffected: they were already computed from the first
  // 72 bytes of the password.
  private def truncateForBcrypt(rawPassword: CharSequence): String =
    val bytes = rawPassword.toString.getBytes(StandardCharsets.UTF_8)
    if bytes.length <= 72 then
      rawPassword.toString
    else
      var n = 72
      // Back up so we don't split a multibyte sequence: the first excluded byte
      // (index n) must be a starter byte. Continuation bytes are 10xxxxxx
      // (0x80..0xBF); back up while index n is a continuation byte.
      while n > 0 && (bytes(n) & 0xc0) == 0x80 do
        n -= 1
      new String(bytes, 0, n, StandardCharsets.UTF_8)

  override def encode(rawPassword: CharSequence): String = bcryptEncoder.encode(truncateForBcrypt(rawPassword))

  override def matches(rawPassword: CharSequence, encodedPassword: String): Boolean =
    if rawPassword.isEmpty then
      false
    else if PasswordEncoderImpl.isBcrypt(encodedPassword) then
      bcryptEncoder.matches(truncateForBcrypt(rawPassword), encodedPassword)
    else
      try
        legacyEncryptor.checkPassword(rawPassword.toString, encodedPassword)
      catch
        case _: EncryptionOperationNotPossibleException =>
          logger.warn("Can't check password")
          false

  override def upgradeEncoding(encodedPassword: String): Boolean = !PasswordEncoderImpl.isBcrypt(encodedPassword)

object PasswordEncoderImpl:
  def isBcrypt(encodedPassword: String): Boolean =
    encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$") || encodedPassword.startsWith("$2y$")
