/*
 * Copyright 1998-2015 Linux.org.ru
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

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.org.linux.util.bbcode.Parser.DEFAULT_PARSER

class CodeTagTest {
  private def parse(text: String): String = {
    DEFAULT_PARSER.parseRoot(DEFAULT_PARSER.createRootNode, text).renderXHtml
  }

  @Test
  def codeTest():Unit = {
    assertEquals(
      """<div class="code"><pre class="no-highlight"><code>[list][*]one[*]two[*]three[/list]</code></pre></div>""",
      parse("[code][list][*]one[*]two[*]three[/list][/code]"))

    assertEquals("""<div class="code"><pre class="no-highlight"><code>simple code</code></pre></div>""",
      parse("[code]\nsimple code[/code]"))
    assertEquals("""<div class="code"><pre class="no-highlight"><code>[list][*]one[*]two[*]three[/list]</code></pre></div>""",
      parse("[code]\n[list][*]one[*]two[*]three[/list][/code]"))
  }

  @Test
  def codeSpacesTest():Unit = {
    assertEquals("""<div class="code"><pre class="no-highlight"><code>[url]test[/url] [url]test[/url]</code></pre></div>""",
      parse("[code][url]test[/url] [url]test[/url][/code]"))
  }

  @Test
  def codeCleanTest():Unit = {
    assertEquals("", parse("[code][/code]"))
  }

  @Test
  def codeKnowTest():Unit = {
    assertEquals("""<div class="code"><pre class="language-cpp"><code>#include &lt;stdio.h&gt;</code></pre></div>""",
      parse("[code=cxx]#include <stdio.h>[/code]"))
  }

  @Test
  def codeUnKnowTest():Unit = {
    assertEquals("""<div class="code"><pre class="no-highlight"><code>#include &lt;stdio.h&gt;</code></pre></div>""",
      parse("[code=foo]#include <stdio.h>[/code]"))
  }
}