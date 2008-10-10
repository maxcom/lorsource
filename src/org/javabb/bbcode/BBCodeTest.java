package org.javabb.bbcode;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class BBCodeTest {
  private static final String LINE_BREAK_TEST = "test\ntest\n\ntest";
  private static final String LINE_BREAK_RESULT = "test\ntest<br>test";
  
  private static final String TAG_ESCAPE_TEST = "<br>";
  private static final String TAG_ESCAPE_RESULT = "&lt;br&gt;";

  @Test
  public void testLineBreak() {
    BBCodeProcessor proc = new BBCodeProcessor();

    String result = proc.preparePostText(LINE_BREAK_TEST);

    assertEquals(LINE_BREAK_RESULT, result);
  }

  @Test
  public void testTagExcape() {
    BBCodeProcessor proc = new BBCodeProcessor();

    String result = proc.preparePostText(TAG_ESCAPE_TEST);

    assertEquals(TAG_ESCAPE_RESULT, result);
  }
}
