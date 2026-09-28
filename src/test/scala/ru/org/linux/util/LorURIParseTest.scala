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
package ru.org.linux.util

import munit.FunSuite

import java.net.URISyntaxException

class LorURIParseTest extends FunSuite:
  test("simpleUrl"):
    val uri = LorURI.parse("http://example.com/path?page=2#anchor")

    assertEquals("http", uri.getScheme)
    assertEquals("example.com", uri.getHost)
    assertEquals(-1, uri.getPort)
    assertEquals("/path", uri.getPath)
    assertEquals("page=2", uri.getQuery)
    assertEquals("anchor", uri.getFragment)
    assertEquals("http://example.com/path?page=2#anchor", uri.escaped)

  test("portAndUserInfo"):
    val uri = LorURI.parse("https://user:pass@example.com:8080/p")

    assertEquals("https", uri.getScheme)
    assertEquals("example.com", uri.getHost)
    assertEquals(8080, uri.getPort)

  test("emptyPort"):
    val uri = LorURI.parse("http://example.com:/path")

    assertEquals("example.com", uri.getHost)
    assertEquals(-1, uri.getPort)

  test("portOutOfRangeRejected"):
    intercept[LorURIException]:
      LorURI.parse("http://example.com:70000/path")

  test("portOverflowRejected"):
    intercept[LorURIException]:
      LorURI.parse("http://example.com:2147483648/path")

  test("registryPortOutOfRangeRejected"):
    intercept[LorURIException]:
      LorURI.parse("http://foo_bar.host:70000/path")

  test("registryPortOverflowRejected"):
    intercept[LorURIException]:
      LorURI.parse("http://foo_bar.host:2147483648/path")

  test("spaceIsEncoded"):
    val uri = LorURI.parse("http://example.com/path with space")

    assertEquals("http://example.com/path%20with%20space", uri.escaped)
    assertEquals("http://example.com/path with space", uri.unescaped)

  test("quotesAndAngleBracketsAreEncoded"):
    val uri = LorURI.parse("http://example.com/a\"b<c>d")

    assertEquals("http://example.com/a%22b%3Cc%3Ed", uri.escaped)
    assertEquals("http://example.com/a\"b<c>d", uri.unescaped)

  test("lonePercentIsEncoded"):
    val uri = LorURI.parse("http://example.com/100%")

    assertEquals("http://example.com/100%25", uri.escaped)
    assertEquals("http://example.com/100%", uri.unescaped)

  test("validEscapesNotDoubleEncoded"):
    val uri = LorURI.parse("http://example.com/a%2Bb?q=c%2B%2B")

    assertEquals("http://example.com/a%2Bb?q=c%2B%2B", uri.escaped)
    assertEquals("a+b", uri.getPath.stripPrefix("/"))
    assertEquals("q=c++", uri.getQuery)

  test("pipeInFragmentIsEncoded"):
    val uri = LorURI.parse("http://example.com/?sl=en#ru|en|осёл")

    assertEquals("http://example.com/?sl=en#ru%7Cen%7C%D0%BE%D1%81%D1%91%D0%BB", uri.escaped)
    assertEquals("ru|en|осёл", uri.getFragment)

  test("bracketsKeptInQueryButEncodedInPath"):
    val inQuery = LorURI.parse("http://example.com/index.php?filter[]=all")
    assertEquals("http://example.com/index.php?filter[]=all", inQuery.escaped)
    assertEquals("filter[]=all", inQuery.getQuery)

    val inPath = LorURI.parse("http://example.com/a[b]c")
    assertEquals("http://example.com/a%5Bb%5Dc", inPath.escaped)
    assertEquals("a[b]c", inPath.getPath.stripPrefix("/"))

  test("nonAsciiPathAndQuery"):
    val uri = LorURI.parse("http://ru.wikipedia.org/wiki/Литература?негр=эфиоп")

    assertEquals(
      "http://ru.wikipedia.org/wiki/%D0%9B%D0%B8%D1%82%D0%B5%D1%80%D0%B0%D1%82%D1%83%D1%80%D0%B0?%D0%BD%D0%B5%D0%B3%D1%80=%D1%8D%D1%84%D0%B8%D0%BE%D0%BF",
      uri.escaped
    )
    assertEquals("http://ru.wikipedia.org/wiki/Литература?негр=эфиоп", uri.unescaped)

  test("plusIsNotSpace"):
    val uri = LorURI.parse("http://www.linux.org.ru/view-news.jsp?tag=c++")

    assertEquals("tag=c++", uri.getQuery)
    assertEquals("http://www.linux.org.ru/view-news.jsp?tag=c++", uri.escaped)

  test("invalidUtf8EscapesGiveReplacementChar"):
    val uri = LorURI.parse("http://www.ozon.ru/?text=%D8%E8%EB%E4%F2")

    assertEquals(true, uri.unescaped.contains('\ufffd'))
    assertEquals("http://www.ozon.ru/?text=%D8%E8%EB%E4%F2", uri.escaped)

  test("underscoreHostIsExtractedFromAuthority"):
    val uri = LorURI.parse("http://foo_bar.host/path")

    assertEquals("foo_bar.host", uri.getHost)

  test("nonAsciiHostIsExtractedFromAuthority"):
    val uri = LorURI.parse("http://пример.рф/x")

    assertEquals("пример.рф", uri.getHost)
    assertEquals("http://%D0%BF%D1%80%D0%B8%D0%BC%D0%B5%D1%80.%D1%80%D1%84/x", uri.escaped)

  test("ipv6Host"):
    val uri = LorURI.parse("http://[::1]:8080/x")

    assertEquals("[::1]", uri.getHost)
    assertEquals(8080, uri.getPort)
    assertEquals("http://[::1]:8080/x", uri.escaped)

  test("schemeAndHostCasePreserved"):
    val uri = LorURI.parse("HTTP://WWW.Example.COM/Path")

    assertEquals("HTTP", uri.getScheme)
    assertEquals("WWW.Example.COM", uri.getHost)

  test("emptyAnchorPreserved"):
    val uri = LorURI.parse("http://example.com/#")

    assertEquals("", uri.getFragment)
    assertEquals("http://example.com/#", uri.escaped)

  test("noSchemeRejected"):
    intercept[LorURIException]:
      LorURI.parse("//example.com/path")

  test("noHostRejected"):
    intercept[LorURIException]:
      LorURI.parse("mailto:user@example.com")

  test("relativeRejected"):
    intercept[LorURIException]:
      LorURI.parse("some crap")

  test("emptyRejected"):
    intercept[LorURIException]:
      LorURI.parse("")

  test("nullRejected"):
    intercept[LorURIException]:
      LorURI.parse(null)

  test("digitSchemeRejected"):
    intercept[LorURIException]:
      LorURI.parse("127.0.0.1:8080/news/debian/123")

  test("withFragment"):
    val uri = LorURI.parse("http://example.com/path?a=b#old")
    val replaced = uri.withFragment("cut42")

    assertEquals("http://example.com/path?a=b#cut42", replaced.escaped)
    assertEquals("cut42", replaced.getFragment)
    assertEquals("old", uri.getFragment)

  test("create"):
    val uri = LorURI.create("https", "www.linux.org.ru", 8085, "/forum/talks/123", "cid=456", null)

    assertEquals("https://www.linux.org.ru:8085/forum/talks/123?cid=456", uri.escaped)

  test("createWithoutPortAndQuery"):
    val uri = LorURI.create("https", "www.linux.org.ru", -1, "/forum/talks/123", null, null)

    assertEquals("https://www.linux.org.ru/forum/talks/123", uri.escaped)

  test("createEncodesNonAsciiPath"):
    val uri = LorURI.create("https", "www.linux.org.ru", -1, "/wiki/осёл", null, null)

    assertEquals("https://www.linux.org.ru/wiki/%D0%BE%D1%81%D1%91%D0%BB", uri.escaped)

  test("createChainsCause"):
    val e = intercept[LorURIException]:
      LorURI.create("1x", "example.com", -1, "/", null, null)

    assert(e.getCause.isInstanceOf[URISyntaxException])
