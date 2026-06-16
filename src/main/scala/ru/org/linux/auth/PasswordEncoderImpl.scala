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

@Component
class PasswordEncoderImpl extends PasswordEncoder:
  private val legacyEncryptor = new BasicPasswordEncryptor
  private val logger = LoggerFactory.getLogger(classOf[PasswordEncoderImpl])
  private val bcryptEncoder = new BCryptPasswordEncoder

  override def encode(rawPassword: CharSequence): String = bcryptEncoder.encode(rawPassword)

  override def matches(rawPassword: CharSequence, encodedPassword: String): Boolean =
    if rawPassword.isEmpty then
      false
    else if PasswordEncoderImpl.isBcrypt(encodedPassword) then
      bcryptEncoder.matches(rawPassword, encodedPassword)
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
