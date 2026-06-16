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

  test("isBcrypt detects all variants"):
    assert(PasswordEncoderImpl.isBcrypt("$2a$10$abcdefghijklmnopqrstuvwxABCDEFGHIJKLMN"))
    assert(PasswordEncoderImpl.isBcrypt("$2b$10$abcdefghijklmnopqrstuvwxABCDEFGHIJKLMN"))
    assert(PasswordEncoderImpl.isBcrypt("$2y$10$abcdefghijklmnopqrstuvwxABCDEFGHIJKLMN"))
    assert(!PasswordEncoderImpl.isBcrypt("$2x$10$abcdefghijklmnopqrstuvwxABCDEFGHIJKLMN"))
    assert(!PasswordEncoderImpl.isBcrypt("not-a-bcrypt-hash"))
