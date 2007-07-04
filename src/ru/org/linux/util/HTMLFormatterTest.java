package ru.org.linux.util;

import junit.framework.TestCase;

public class HTMLFormatterTest extends TestCase {
  private static String TEXT1 = "Here is www.linux.org.ru, have fun! :-)";
  private static String RESULT1 = "Here is <a href=\"http://www.linux.org.ru\">www.linux.org.ru</a>, have fun! :-)";

  private static String TEXT2 = "Here is http://linux.org.ru, have fun! :-)";
  private static String RESULT2 = "Here is <a href=\"http://linux.org.ru\">http://linux.org.ru</a>, have fun! :-)";

  private static String TEXT3 = "Long url: http://www.linux.org.ru/profile/maxcom/view-message.jsp?msgid=1993651";
  private static String RESULT3 = "Long url: <a href=\"http://www.linux.org.ru/profile/maxcom/view-message.jsp?msgid=1993651\">http://www.linux....</a>";

  private static String TEXT4 = "Forced wrapping: 12345678901234567890123456789";
  private static String RESULT4 = "Forced wrapping: 1234567890123456789 0123456789";

  private static String TEXT6 = "123&nbsp;4";

  private static String TEXT7 = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
  private static String RESULT7 = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";

  private static String TEXT8 = "Long url: http://www.linux.org.ru/profile/maxcom/view-message.jsp?msgid=1993651&a=b";
  private static String RESULT8 = "Long url: <a href=\"http://www.linux.org.ru/profile/maxcom/view-message.jsp?msgid=1993651&amp;a=b\">http://www.linux....</a>";

  public void testURLHighlight() throws UtilException {
    HTMLFormatter formatter = new HTMLFormatter(TEXT1);

    formatter.enableUrlHighLightMode();

    assertEquals(formatter.process(), RESULT1);
  }

  public void testURLHighlight2() throws UtilException {
    HTMLFormatter formatter = new HTMLFormatter(TEXT2);

    formatter.enableUrlHighLightMode();

    assertEquals(formatter.process(), RESULT2);
  }

  public void testURLHighlight3() throws UtilException {
    HTMLFormatter formatter = new HTMLFormatter(TEXT3);

    formatter.enableUrlHighLightMode();
    formatter.setMaxLength(20);

    assertEquals(formatter.process(), RESULT3);
  }

  public void testURLHighlight4() throws UtilException {
    HTMLFormatter formatter = new HTMLFormatter(TEXT8);

    formatter.enableUrlHighLightMode();
    formatter.setMaxLength(20);

    assertEquals(formatter.process(), RESULT8);
  }

  public void testWrap1() throws UtilException {
    HTMLFormatter formatter = new HTMLFormatter(TEXT4);

    formatter.setMaxLength(20);

    assertEquals(RESULT4, formatter.process());
  }

  public void testCountSGML1() throws UtilException {
    assertEquals(HTMLFormatter.countCharacters(TEXT4), TEXT4.length());
  }

  public void testCountSGML2() throws UtilException {
    assertEquals(5, HTMLFormatter.countCharacters(TEXT6));
  }

  public void testWrapSGML() throws UtilException {
    assertEquals(TEXT7, HTMLFormatter.wrapLongLine(TEXT7, 35, " "));
  }

  public void testWrapSGML2() throws UtilException {
    assertEquals(RESULT7, HTMLFormatter.wrapLongLine(TEXT7, 20, " "));
  }
}
