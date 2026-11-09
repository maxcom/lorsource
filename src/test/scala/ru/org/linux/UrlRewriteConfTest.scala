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
package ru.org.linux

import munit.FunSuite
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}
import org.tuckey.web.filters.urlrewrite.{Conf, UrlRewriter}

import java.io.File
import scala.jdk.CollectionConverters.*

/** Loads the production urlrewrite.xml through tuckey's UrlRewriter and checks the trailing-slash rules: they must not
  * emit a scheme-relative redirect for paths starting with '/' or '\' and must keep normalizing everything else.
  * UrlRewriteFilter answers before the Spring Security chain, so a bad Location here bypasses StrictHttpFirewall
  * entirely.
  */
class UrlRewriteConfTest extends FunSuite:
  private lazy val rewriter =
    val conf = new Conf(new File("src/main/webapp/WEB-INF/urlrewrite.xml").toURI.toURL)

    assert(conf.isOk, s"urlrewrite.xml failed to load: ${conf.getErrors.asScala.mkString("; ")}")

    new UrlRewriter(conf)

  private def rewrite(requestUri: String): Option[String] =
    val request = new MockHttpServletRequest("GET", requestUri)
    val response = new MockHttpServletResponse()

    Option(rewriter.processRequest(request, response)).map(_.getTarget)

  test("first path segment backslash does not produce scheme-relative redirect") {
    // UrlRewriteFilter percent-decodes the request URI before matching: /%5Cevil.com is /\evil.com,
    // and browsers resolve Location: /\evil.com as https://evil.com/ (scheme-relative)
    assertEquals(rewrite("/%5Cevil.com"), None)
    assertEquals(rewrite("/%5C%5Cevil.com"), None)
    assertEquals(rewrite("/%5Cevil.com/"), None)
  }

  test("first path segment extra slash does not produce scheme-relative redirect") {
    assertEquals(rewrite("//evil.com/"), None)
  }

  test("control characters in the path do not produce a redirect") {
    // UrlRewriteFilter percent-decodes the request URI before matching, and browsers strip tab/CR/LF
    // from URLs (WHATWG URL standard), so Location /\t//evil.com resolves to //evil.com (scheme-relative)
    assertEquals(rewrite("/%09//evil.com/"), None)
    assertEquals(rewrite("/%09/evil.com/"), None)
    assertEquals(rewrite("/%0A//evil.com/"), None)
    assertEquals(rewrite("/%0D//evil.com/"), None)
    assertEquals(rewrite("/x%09y//evil.com/"), None)
    assertEquals(rewrite("/%7F//evil.com/"), None)
  }

  test("encoded space is still normalized (not a control char)") {
    assertEquals(rewrite("/foo%20bar/"), Some("/foo bar"))
  }

  test("trailing slash is still normalized") {
    assertEquals(rewrite("/foo/"), Some("/foo"))
    assertEquals(rewrite("/view-message.jsp/"), Some("/view-message.jsp"))
  }

  test("section roots and user lists still get trailing slash") {
    assertEquals(rewrite("/forum"), Some("/forum/"))
    assertEquals(rewrite("/news/debian"), Some("/news/debian/"))
    assertEquals(rewrite("/people/maxcom"), Some("/people/maxcom/"))
    assertEquals(rewrite("/tags"), Some("/tags/"))
  }
end UrlRewriteConfTest
