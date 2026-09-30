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

class LoginControllerTest extends FunSuite:
  test("safeRedirectUrl keeps same-site relative paths") {
    assertEquals(LoginController.safeRedirectUrl("/"), "/")
    assertEquals(LoginController.safeRedirectUrl("/news/"), "/news/")
    assertEquals(LoginController.safeRedirectUrl("/people/foo/profile"), "/people/foo/profile")
    assertEquals(LoginController.safeRedirectUrl("/forum/general?lastmod=true"), "/forum/general?lastmod=true")
    // percent-encoded control characters are not stripped by browsers, so they stay same-origin
    assertEquals(LoginController.safeRedirectUrl("/%09/evil.com"), "/%09/evil.com")
  }

  test("safeRedirectUrl rejects protocol-relative urls") {
    assertEquals(LoginController.safeRedirectUrl("//evil.com"), "/")
    assertEquals(LoginController.safeRedirectUrl("//evil.com/path"), "/")
  }

  test("safeRedirectUrl rejects backslash protocol-relative bypass") {
    assertEquals(LoginController.safeRedirectUrl("/\\evil.com"), "/")
    assertEquals(LoginController.safeRedirectUrl("/\\/evil.com"), "/")
  }

  test("safeRedirectUrl rejects tab/newline stripping bypass and other control characters") {
    // browsers strip tab/CR/LF from URLs before parsing, so "/<TAB>/evil.com" becomes "//evil.com"
    assertEquals(LoginController.safeRedirectUrl("/\t/evil.com"), "/")
    assertEquals(LoginController.safeRedirectUrl("/\n/evil.com"), "/")
    assertEquals(LoginController.safeRedirectUrl("/\r/evil.com"), "/")
    assertEquals(LoginController.safeRedirectUrl("/\t\t/evil.com"), "/")
    // raw control characters are never legitimate in a redirect target
    assertEquals(LoginController.safeRedirectUrl("/\u0000/evil.com"), "/")
    assertEquals(LoginController.safeRedirectUrl("/\u000c/evil.com"), "/")
    assertEquals(LoginController.safeRedirectUrl("/\u007f/evil.com"), "/")
    assertEquals(LoginController.safeRedirectUrl("/news\t"), "/")
  }

  test("safeRedirectUrl rejects absolute urls") {
    assertEquals(LoginController.safeRedirectUrl("https://evil.com"), "/")
    assertEquals(LoginController.safeRedirectUrl("http://evil.com"), "/")
    assertEquals(LoginController.safeRedirectUrl("javascript:alert(1)"), "/")
  }

  test("safeRedirectUrl rejects empty, null and non-path values") {
    assertEquals(LoginController.safeRedirectUrl(""), "/")
    assertEquals(LoginController.safeRedirectUrl(null), "/")
    assertEquals(LoginController.safeRedirectUrl("news/"), "/")
    assertEquals(LoginController.safeRedirectUrl("~/news/"), "/")
  }
