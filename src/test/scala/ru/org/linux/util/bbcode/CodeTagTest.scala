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

class CodeTagTest extends FunSuite {
  private def parse(text: String): String = {
    DEFAULT_PARSER.parseRoot(DEFAULT_PARSER.createRootNode, text).renderXHtml
  }

  test("preserve lorcode") {
    assertEquals(
      parse("[code][list][*]one[*]two[*]three[/list][/code]"),
      """<div class="code"><pre class="no-highlight"><code>[list][*]one[*]two[*]three[/list]</code></pre></div>"""
    )
  }

  test("preserve lorcode in case of extra braces") {
    assertEquals(
      parse("[code][i]][/code]"),
      """<div class="code"><pre class="no-highlight"><code>[i]]</code></pre></div>"""
    )
    assertEquals(
      parse("[code][[i][/code]"),
      """<div class="code"><pre class="no-highlight"><code>[[i]</code></pre></div>"""
    )
    assertEquals(
      parse("[code]Apple ][[/code]"),
      """<div class="code"><pre class="no-highlight"><code>Apple ][</code></pre></div>"""
    )
  }

  test("remove leading \\n") {
    assertEquals(
      parse("[code]\nsimple code[/code]"),
      """<div class="code"><pre class="no-highlight"><code>simple code</code></pre></div>"""
    )
  }

  test("remove leading \\n before lorcode tags") {
    assertEquals(
      parse("[code]\n[list][*]one[*]two[*]three[/list][/code]"),
      """<div class="code"><pre class="no-highlight"><code>[list][*]one[*]two[*]three[/list]</code></pre></div>"""
    )
  }

  test("preserve spaces between lorcode tags") {
    assertEquals(
      parse("[code][url]test[/url] [url]test[/url][/code]"),
      """<div class="code"><pre class="no-highlight"><code>[url]test[/url] [url]test[/url]</code></pre></div>"""
    )
  }

  test("skip empty block") {
    assertEquals(parse("[code][/code]"), "")
  }

  test("write css class for known lang") {
    assertEquals(
      parse("[code=cxx]#include <stdio.h>[/code]"),
      """<div class="code"><pre class="language-cpp"><code>#include &lt;stdio.h&gt;</code></pre></div>"""
    )
  }

  test("do not write css class for unknown lang") {
    assertEquals(
      parse("[code=foo]#include <stdio.h>[/code]"),
      """<div class="code"><pre class="no-highlight"><code>#include &lt;stdio.h&gt;</code></pre></div>"""
    )
  }
}