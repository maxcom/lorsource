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
package ru.org.linux.util.bbcode

import munit.FunSuite
import ru.org.linux.util.bbcode.Parser.DEFAULT_PARSER

class UrlWithParamTagTest extends FunSuite:
  private def parse(text: String): String = DEFAULT_PARSER.parseRoot(DEFAULT_PARSER.createRootNode, text).renderXHtml

  test("parse and renderXhtml escape quotes and markup") {
    assertEquals(
      parse("""[url=http://tts.com/"><b>a</b>]usrl[/url]"""),
      """<p><a href="http://tts.com/&quot;&gt;&lt;b&gt;a&lt;/b&gt;">usrl</a></p>""")
  }

  test("short link text gets domain appended") {
    assertEquals(
      parse("[url=https://www.linux.org.ru/]@[/url]"),
      """<p><a href="https://www.linux.org.ru/">@ (linux.org.ru)</a></p>""")
  }

  test("short link text with unparseable host gets placeholder") {
    assertEquals(
      parse("[url=http://example.invalid/]@[/url]"),
      """<p><a href="http://example.invalid/">@ (---)</a></p>""")
  }

  test("long link text does not get domain appended") {
    assertEquals(
      parse("[url=https://www.linux.org.ru/]linux[/url]"),
      """<p><a href="https://www.linux.org.ru/">linux</a></p>""")
  }

  test("short link text with invalid url renders as s without domain") {
    assertEquals(parse("[url=http://#$#@$@QW]@[/url]"), """<p><s title="http://#$#@$@QW">@</s></p>""")
  }

  test("whitespace link text escapes url in link body") {
    assertEquals(
      parse("""[url=http://example.com/<img/src=x/onerror=alert(1)>] [/url]"""),
      """<p><a href="http://example.com/&lt;img/src=x/onerror=alert(1)&gt;">http://example.com/&lt;img/src=x/onerror=alert(1)&gt;</a></p>"""
    )
  }

  test("empty link text escapes url in link body") {
    assertEquals(
      parse("""[url=http://example.com/<img/src=x/onerror=alert(1)>][/url]"""),
      """<p><a href="http://example.com/&lt;img/src=x/onerror=alert(1)&gt;">http://example.com/&lt;img/src=x/onerror=alert(1)&gt;</a></p>"""
    )
  }

  test("javascript scheme with empty link text is not rendered as link") {
    assertEquals(
      parse("[url=javascript:alert(1)] [/url]"),
      """<p><s title="javascript:alert(1)">javascript:alert(1)</s></p>"""
    )
  }
