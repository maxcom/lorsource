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
import jakarta.servlet.http.Cookie
import org.mockito.Mockito.{mock, when}
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.{User, UserDetailsService}
import ru.org.linux.user.UserDao

import java.security.MessageDigest
import java.util.Base64

class GenerationBasedTokenRememberMeServicesTest extends FunSuite:
  private val Key = "test-key"
  private val Username = "testuser"
  private val PasswordHash = "bcrypt-hash"
  private val Expiry = System.currentTimeMillis() + 86400000L

  private val userDao = mock(classOf[UserDao])
  when(userDao.getTokenGeneration(Username)).thenReturn(0)

  private val userDetailsService: UserDetailsService =
    username =>
      assertEquals(username, Username)
      User.withUsername(username).password(PasswordHash).roles("USER").build()

  private val services = new GenerationBasedTokenRememberMeServices(Key, userDetailsService, userDao)

  services.setCookieName("remember_me")

  private def signature(algorithm: String, expiry: Long): String =
    val digest = MessageDigest.getInstance(algorithm)

    digest.digest(s"$Username:$expiry:$PasswordHash:$Key".getBytes).map(b => f"$b%02x").mkString

  private def autoLogin(parts: String*): (Authentication, MockHttpServletResponse) =
    val request = new MockHttpServletRequest

    request.setCookies(new Cookie("remember_me", Base64.getEncoder.encodeToString(parts.mkString(":").getBytes)))

    val response = new MockHttpServletResponse

    services.autoLogin(request, response) -> response

  private def assertCancelled(response: MockHttpServletResponse): Unit =
    val cancelled = response.getCookie("remember_me")

    assert(cancelled != null, "expected remember_me cookie to be cancelled")
    assertEquals(cancelled.getMaxAge, 0)

  test("accepts valid SHA-256 cookie"):
    val (auth, response) = autoLogin(Username, Expiry.toString, "SHA256", signature("SHA-256", Expiry))

    assert(auth != null)
    assertEquals(auth.getName, Username)
    assert(response.getCookie("remember_me") == null)

  test("rejects legacy MD5 four-token cookie"):
    val (auth, response) = autoLogin(Username, Expiry.toString, "MD5", signature("MD5", Expiry))

    assert(auth == null)
    assertCancelled(response)

  test("rejects legacy three-token cookie"):
    val (auth, response) = autoLogin(Username, Expiry.toString, signature("MD5", Expiry))

    assert(auth == null)
    assertCancelled(response)

  test("rejects unknown algorithm without throwing"):
    val (auth, response) = autoLogin(Username, Expiry.toString, "FOO", signature("SHA-256", Expiry))

    assert(auth == null)
    assertCancelled(response)

end GenerationBasedTokenRememberMeServicesTest
