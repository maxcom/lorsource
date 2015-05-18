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

package ru.org.linux.util.bbcode;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ru.org.linux.util.bbcode.Parser.DEFAULT_PARSER;

public class CodeTagTest {
  private String parse(String text) {
    return DEFAULT_PARSER.parseRoot(DEFAULT_PARSER.createRootNode(), text).renderXHtml();
  }

  @Test
  public void codeTest() {
    assertEquals(
            "<div class=\"code\"><pre class=\"no-highlight\"><code>[list][*]one[*]two[*]three[/list]</code></pre></div>",
            parse("[code][list][*]one[*]two[*]three[/list][/code]"));

    assertEquals(
            "<div class=\"code\"><pre class=\"no-highlight\"><code>simple code</code></pre></div>",
            parse("[code]\nsimple code[/code]"));

    assertEquals(
            "<div class=\"code\"><pre class=\"no-highlight\"><code>[list][*]one[*]two[*]three[/list]</code></pre></div>",
            parse("[code]\n[list][*]one[*]two[*]three[/list][/code]"));
  }

  @Test
  public void codeSpacesTest() {
    assertEquals(
            "<div class=\"code\"><pre class=\"no-highlight\"><code>[url]test[/url] [url]test[/url]</code></pre></div>",
            parse("[code][url]test[/url] [url]test[/url][/code]"));
  }

  @Test
  public void codeCleanTest() {
    assertEquals("", parse("[code][/code]"));
  }

  @Test
  public void codeKnowTest() {
    assertEquals("<div class=\"code\"><pre class=\"language-cpp\"><code>#include &lt;stdio.h&gt;</code></pre></div>",
            parse("[code=cxx]#include <stdio.h>[/code]"));
  }

  @Test
  public void codeUnKnowTest() {
    assertEquals("<div class=\"code\"><pre class=\"no-highlight\"><code>#include &lt;stdio.h&gt;</code></pre></div>",
            parse("[code=foo]#include <stdio.h>[/code]"));
  }
}
