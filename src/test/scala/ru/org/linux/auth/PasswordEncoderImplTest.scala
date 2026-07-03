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

import munit.FunSuite
import org.jasypt.util.password.BasicPasswordEncryptor

class PasswordEncoderImplTest extends FunSuite:
  private val encoder = new PasswordEncoderImpl

  test("encode returns bcrypt hash"):
    val encoded = encoder.encode("password")
    assert(PasswordEncoderImpl.isBcrypt(encoded))

  test("matches bcrypt password"):
    val encoded = encoder.encode("password")
    assert(encoder.matches("password", encoded))
    assert(!encoder.matches("wrong", encoded))

  test("matches legacy jasypt password"):
    val legacyEncryptor = new BasicPasswordEncryptor
    val legacyHash = legacyEncryptor.encryptPassword("password")
    assert(encoder.matches("password", legacyHash))
    assert(!encoder.matches("wrong", legacyHash))

  test("matches empty password returns false"):
    assert(!encoder.matches("", encoder.encode("password")))
    assert(!encoder.matches("", "anystack"))

  test("matches invalid hash returns false"):
    assert(!encoder.matches("password", "not-a-valid-hash"))

  test("upgradeEncoding returns true for legacy hash"):
    val legacyEncryptor = new BasicPasswordEncryptor
    val legacyHash = legacyEncryptor.encryptPassword("password")
    assert(encoder.upgradeEncoding(legacyHash))

  test("upgradeEncoding returns false for bcrypt hash"):
    val bcryptHash = encoder.encode("password")
    assert(!encoder.upgradeEncoding(bcryptHash))

  test("encode handles passwords longer than 72 bytes"):
    val longPassword = "x" * 200
    val encoded = encoder.encode(longPassword)
    assert(PasswordEncoderImpl.isBcrypt(encoded))
    assert(encoder.matches(longPassword, encoded))
    // a password sharing the same 72-byte prefix must also match (bcrypt only
    // considers the first 72 bytes)
    assert(encoder.matches("x" * 72 + "y" * 100, encoded))
    assert(!encoder.matches("y" * 200, encoded))

  test("matches truncates at a UTF-8 character boundary"):
    // 'a' is 1 byte and 'я' is 2 bytes; "a" + "я" * 36 is 73 bytes. The
    // 72-byte bcrypt cut lands on the trailing (continuation) byte of
    // 'я' #35 (byte 72 = 0x8F), so truncateForBcrypt must back up to
    // byte 71 — a starter byte — yielding the 71-byte prefix
    // "a" + "я" * 35. The truncated input must be valid UTF-8 and round-trip.
    val password = "a" + "я" * 36
    val encoded = encoder.encode(password)
    assert(encoder.matches(password, encoded))
    // same first 73 bytes (truncates to the same 71-byte prefix) — must still match
    assert(encoder.matches("a" + "я" * 36 + "b" * 50, encoded))
    // different first byte, different truncated prefix — must not match
    assert(!encoder.matches("b" + "я" * 36, encoded))

  test("isBcrypt detects all variants"):
    assert(PasswordEncoderImpl.isBcrypt("$2a$10$abcdefghijklmnopqrstuvwxABCDEFGHIJKLMN"))
    assert(PasswordEncoderImpl.isBcrypt("$2b$10$abcdefghijklmnopqrstuvwxABCDEFGHIJKLMN"))
    assert(PasswordEncoderImpl.isBcrypt("$2y$10$abcdefghijklmnopqrstuvwxABCDEFGHIJKLMN"))
    assert(!PasswordEncoderImpl.isBcrypt("$2x$10$abcdefghijklmnopqrstuvwxABCDEFGHIJKLMN"))
    assert(!PasswordEncoderImpl.isBcrypt("not-a-bcrypt-hash"))
