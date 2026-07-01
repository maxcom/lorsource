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
import ru.org.linux.spring.SiteConfig

import java.sql.Timestamp
import java.time.Instant
import java.util.Properties

class SecretTokenServiceTest extends FunSuite:
  private def makeService(secret: String = "secret"): SecretTokenService =
    val properties = new Properties()
    properties.setProperty("MainUrl", "http://test-lor:8080/")
    properties.setProperty("SecureUrl", "http://test-lor:8080/")
    properties.setProperty("Secret", secret)
    new SecretTokenService(new SiteConfig(properties))

  private val hizelNick = "hizel"
  private val hizelEmail = "hz@vyborg.ru"
  private val hizelRegdate = new Timestamp(1700000000000L)

  test("getActivationCode returns HMAC-SHA256 of nick:email:regdate:activate with secret key"):
    val service = makeService()
    assertEquals(
      service.getActivationCode(hizelNick, hizelEmail, hizelRegdate),
      "594bfb6f8672ba80804821cbfcdb99ffed12132afe6808ae2100d7538a4ca3f0"
    )

  test("verifyActivationCode returns true for valid code"):
    val service = makeService()
    assert(service.verifyActivationCode(
      hizelNick, hizelEmail, hizelRegdate,
      "594bfb6f8672ba80804821cbfcdb99ffed12132afe6808ae2100d7538a4ca3f0"
    ))

  test("verifyActivationCode returns false for invalid code"):
    val service = makeService()
    assert(!service.verifyActivationCode(hizelNick, hizelEmail, hizelRegdate, "invalidcode"))

  test("verifyActivationCode returns false for wrong nick"):
    val service = makeService()
    assert(!service.verifyActivationCode("wrongnick", hizelEmail, hizelRegdate,
      "594bfb6f8672ba80804821cbfcdb99ffed12132afe6808ae2100d7538a4ca3f0"))

  test("verifyActivationCode returns false for wrong email"):
    val service = makeService()
    assert(!service.verifyActivationCode(hizelNick, "wrong@example.com", hizelRegdate,
      "594bfb6f8672ba80804821cbfcdb99ffed12132afe6808ae2100d7538a4ca3f0"))

  test("verifyActivationCode returns false for wrong regdate"):
    val service = makeService()
    val otherRegdate = new Timestamp(1700000000001L)
    assert(!service.verifyActivationCode(hizelNick, hizelEmail, otherRegdate,
      "594bfb6f8672ba80804821cbfcdb99ffed12132afe6808ae2100d7538a4ca3f0"))

  test("getActivationCode produces different codes for different regdate"):
    val service = makeService()
    val otherRegdate = new Timestamp(1700000000001L)
    val code1 = service.getActivationCode(hizelNick, hizelEmail, hizelRegdate)
    val code2 = service.getActivationCode(hizelNick, hizelEmail, otherRegdate)
    assert(code1 != code2)

  test("getActivationCode produces same code for same regdate"):
    val service = makeService()
    val code1 = service.getActivationCode(hizelNick, hizelEmail, hizelRegdate)
    val code2 = service.getActivationCode(hizelNick, hizelEmail, new Timestamp(hizelRegdate.getTime))
    assertEquals(code1, code2)

  test("verifyActivationCode returns false for legacy code without regdate"):
    val service = makeService()
    assert(!service.verifyActivationCode(hizelNick, hizelEmail, hizelRegdate,
      "96be8717add38ca3cee78426d652dff228e0c2a8a5620c40b522e6b69d7fd54f"))

  test("getResetCode and verifyResetCode are consistent"):
    val service = makeService()
    val tm = new Timestamp(1700000000000L)
    val code = service.getResetCode(hizelNick, hizelEmail, tm)
    assert(service.verifyResetCode(hizelNick, hizelEmail, tm, code))

  test("verifyResetCode returns false for wrong code"):
    val service = makeService()
    val tm = new Timestamp(1700000000000L)
    assert(!service.verifyResetCode(hizelNick, hizelEmail, tm, "wrongcode"))

  test("verifyResetCode returns false for wrong timestamp"):
    val service = makeService()
    val tm = new Timestamp(1700000000000L)
    val code = service.getResetCode(hizelNick, hizelEmail, tm)
    val otherTm = new Timestamp(1700000000001L)
    assert(!service.verifyResetCode(hizelNick, hizelEmail, otherTm, code))

  test("makeRegisterPermit and checkRegisterPermit roundtrip"):
    val service = makeService()
    val now = Instant.ofEpochMilli(1700000000000L)
    val permit = service.makeRegisterPermit(now)
    assert(service.checkRegisterPermit(permit, now))

  test("checkRegisterPermit returns true for valid non-expired permit"):
    val service = makeService()
    val createdAt = Instant.ofEpochMilli(1700000000000L)
    val checkedAt = createdAt.plusMillis(3_599_999L)
    val permit = service.makeRegisterPermit(createdAt)
    assert(service.checkRegisterPermit(permit, checkedAt))

  test("checkRegisterPermit returns false for expired permit"):
    val service = makeService()
    val createdAt = Instant.ofEpochMilli(1700000000000L)
    val checkedAt = createdAt.plusMillis(3_600_001L)
    val permit = service.makeRegisterPermit(createdAt)
    assert(!service.checkRegisterPermit(permit, checkedAt))

  test("checkRegisterPermit returns false for garbage input"):
    val service = makeService()
    assert(!service.checkRegisterPermit("garbage"))

  test("checkRegisterPermit returns false for empty string"):
    val service = makeService()
    assert(!service.checkRegisterPermit(""))

  test("checkRegisterPermit returns false for permit from different secret"):
    val service1 = makeService("secret1")
    val service2 = makeService("secret2")
    val now = Instant.ofEpochMilli(1700000000000L)
    val permit = service1.makeRegisterPermit(now)
    assert(!service2.checkRegisterPermit(permit, now))