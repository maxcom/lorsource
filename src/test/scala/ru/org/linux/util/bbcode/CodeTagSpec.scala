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

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import ru.org.linux.util.bbcode.Parser.DEFAULT_PARSER

@RunWith(classOf[JUnitRunner])
class CodeTagSpec extends Specification {
  private def parse(text: String): String = {
    DEFAULT_PARSER.parseRoot(DEFAULT_PARSER.createRootNode, text).renderXHtml
  }

  "parse and renderXhtml" should {
    "preserve lorcode" in {
      parse("[code][list][*]one[*]two[*]three[/list][/code]") must be equalTo
        """<div class="code"><pre class="no-highlight"><code>[list][*]one[*]two[*]three[/list]</code></pre></div>"""
    }

    // https://www.linux.org.ru/forum/lor-source/11611691
    "preserve lorcode in case of extra braces" in {
      parse("[code][i]][/code]") must be equalTo
        """<div class="code"><pre class="no-highlight"><code>[i]]</code></pre></div>"""
    }.pendingUntilFixed

    "remove leading \\n" in {
      parse("[code]\nsimple code[/code]") must be equalTo
        """<div class="code"><pre class="no-highlight"><code>simple code</code></pre></div>"""
    }

    "remove leading \\n before lorcode tags" in {
      parse("[code]\n[list][*]one[*]two[*]three[/list][/code]") must be equalTo
        """<div class="code"><pre class="no-highlight"><code>[list][*]one[*]two[*]three[/list]</code></pre></div>"""
    }

    "preserve spaces between lorcode tags" in {
      parse("[code][url]test[/url] [url]test[/url][/code]") must be equalTo
        """<div class="code"><pre class="no-highlight"><code>[url]test[/url] [url]test[/url]</code></pre></div>"""
    }

    "skip empty block" in {
      parse("[code][/code]") must be empty
    }

    "write css class for known lang" in {
      parse("[code=cxx]#include <stdio.h>[/code]") must be equalTo
        """<div class="code"><pre class="language-cpp"><code>#include &lt;stdio.h&gt;</code></pre></div>"""
    }

    "do not write css class for unknown lang" in {
      parse("[code=foo]#include <stdio.h>[/code]") must be equalTo
        """<div class="code"><pre class="no-highlight"><code>#include &lt;stdio.h&gt;</code></pre></div>"""
    }
  }
}