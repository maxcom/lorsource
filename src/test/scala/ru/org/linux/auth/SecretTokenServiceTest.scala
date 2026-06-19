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

  test("getActivationCode returns HMAC-SHA256 of nick:email with secret key"):
    val service = makeService()
    assertEquals(
      service.getActivationCode(hizelNick, hizelEmail),
      "96be8717add38ca3cee78426d652dff228e0c2a8a5620c40b522e6b69d7fd54f"
    )

  test("verifyActivationCode returns true for valid code"):
    val service = makeService()
    assert(service.verifyActivationCode(
      hizelNick, hizelEmail,
      "96be8717add38ca3cee78426d652dff228e0c2a8a5620c40b522e6b69d7fd54f"
    ))

  test("verifyActivationCode returns false for invalid code"):
    val service = makeService()
    assert(!service.verifyActivationCode(hizelNick, hizelEmail, "invalidcode"))

  test("verifyActivationCode returns false for wrong nick"):
    val service = makeService()
    assert(!service.verifyActivationCode("wrongnick", hizelEmail,
      "96be8717add38ca3cee78426d652dff228e0c2a8a5620c40b522e6b69d7fd54f"))

  test("verifyActivationCode returns false for wrong email"):
    val service = makeService()
    assert(!service.verifyActivationCode(hizelNick, "wrong@example.com",
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