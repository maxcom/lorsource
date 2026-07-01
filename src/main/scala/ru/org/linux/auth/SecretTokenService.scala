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
import org.springframework.stereotype.Service
import ru.org.linux.spring.SiteConfig
import ru.org.linux.util.StringUtil

import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.sql.Timestamp
import java.time.Instant
import java.util.Base64
import scala.util.Try

@Service
class SecretTokenService(siteConfig: SiteConfig) extends StrictLogging:
  private val base = siteConfig.getSecret

  private val Random = new SecureRandom
  private val SaltLength = 16
  private val IvLength = 12
  private val KeyLengthBits = 256
  private val Pbkdf2Iterations = 65536
  private val GcmTagLengthBits = 128

  private def deriveKey(salt: Array[Byte]): SecretKeySpec =
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = new PBEKeySpec(base.toCharArray, salt, Pbkdf2Iterations, KeyLengthBits)
    val keyBytes = factory.generateSecret(spec).getEncoded
    new SecretKeySpec(keyBytes, "AES")

  private def encrypt(plaintext: String): String =
    val salt = new Array[Byte](SaltLength)
    Random.nextBytes(salt)
    val iv = new Array[Byte](IvLength)
    Random.nextBytes(iv)

    val secretKey = deriveKey(salt)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GcmTagLengthBits, iv))

    val ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8))

    val buffer = ByteBuffer.allocate(salt.length + iv.length + ciphertext.length)
    buffer.put(salt)
    buffer.put(iv)
    buffer.put(ciphertext)
    Base64.getEncoder.encodeToString(buffer.array)

  private def decrypt(encoded: String): Option[String] =
    Try {
      val data = Base64.getDecoder.decode(encoded)

      if data.length < SaltLength + IvLength + 16 then
        None
      else
        val buffer = ByteBuffer.wrap(data)
        val salt = new Array[Byte](SaltLength)
        buffer.get(salt)
        val iv = new Array[Byte](IvLength)
        buffer.get(iv)
        val ciphertext = new Array[Byte](buffer.remaining)
        buffer.get(ciphertext)

        val secretKey = deriveKey(salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GcmTagLengthBits, iv))

        val plaintext = new String(cipher.doFinal(ciphertext), java.nio.charset.StandardCharsets.UTF_8)
        Some(plaintext)
    }.toOption.flatten

  def getResetCode(nick: String, email: String, tm: Timestamp): String =
    StringUtil.hmacSha256(base, s"$nick:$email:${tm.getTime.toString}:reset")

  def verifyResetCode(nick: String, email: String, tm: Timestamp, code: String): Boolean =
    val hmacExpected = StringUtil.hmacSha256(base, s"$nick:$email:${tm.getTime.toString}:reset")
    StringUtil.verifyHash(hmacExpected, code)

  def getActivationCode(nick: String, email: String, regdate: Timestamp): String =
    StringUtil.hmacSha256(base, s"$nick:$email:${regdate.getTime}:activate")

  def verifyActivationCode(nick: String, email: String, regdate: Timestamp, code: String): Boolean =
    val hmacExpected = StringUtil.hmacSha256(base, s"$nick:$email:${regdate.getTime}:activate")
    StringUtil.verifyHash(hmacExpected, code)

  def makeRegisterPermit(now: Instant = Instant.now()): String =
    val expiryMillis = now.plusMillis(3_600_000).toEpochMilli
    val message = s"permit:$expiryMillis"
    encrypt(message)

  def checkRegisterPermit(permit: String, now: Instant = Instant.now()): Boolean =
    decrypt(permit) match
      case None =>
        logger.debug("Invalid permit - decryption failed")
        false
      case Some(decrypted) =>
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
