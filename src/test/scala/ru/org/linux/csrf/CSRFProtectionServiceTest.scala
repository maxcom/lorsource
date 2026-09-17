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

package ru.org.linux.csrf

import munit.FunSuite
import org.springframework.mock.web.MockHttpServletRequest

import java.util.Base64

class CSRFProtectionServiceTest extends FunSuite:
  private val Token = Base64.getEncoder.encodeToString(Array.tabulate[Byte](16)(_.toByte))
  private val OtherToken = Base64.getEncoder.encodeToString(Array.tabulate[Byte](16)(i => (i + 1).toByte))

  private def request(cookieValue: String, paramValue: String): MockHttpServletRequest =
    val request = new MockHttpServletRequest

    request.setAttribute(CSRFProtectionService.CSRF_ATTRIBUTE, cookieValue)
    request.addParameter(CSRFProtectionService.CSRF_INPUT_NAME, paramValue)

    request

  test("acceptsMatchingToken"):
    assert(CSRFProtectionService.checkCSRF(request(Token, Token)))

  test("rejectsMismatchedToken"):
    assert(!CSRFProtectionService.checkCSRF(request(Token, OtherToken)))

  test("rejectsMissingCookie"):
    val request = new MockHttpServletRequest

    request.addParameter(CSRFProtectionService.CSRF_INPUT_NAME, Token)

    assert(!CSRFProtectionService.checkCSRF(request))

  test("rejectsMissingInput"):
    val request = new MockHttpServletRequest

    request.setAttribute(CSRFProtectionService.CSRF_ATTRIBUTE, Token)

    assert(!CSRFProtectionService.checkCSRF(request))

  test("rejectsInvalidBase64Input"):
    assert(!CSRFProtectionService.checkCSRF(request(Token, "!!!")))

  test("rejectsInvalidBase64Cookie"):
    assert(!CSRFProtectionService.checkCSRF(request("***", Token)))

  test("trimsSurroundingWhitespace"):
    assert(CSRFProtectionService.checkCSRF(request(s" $Token ", s"\t$Token\n")))
