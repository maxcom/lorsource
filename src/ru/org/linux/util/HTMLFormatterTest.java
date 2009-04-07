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

package ru.org.linux.util;

import junit.framework.TestCase;

public class HTMLFormatterTest extends TestCase {
  private static final String TEXT1 = "Here is www.linux.org.ru, have fun! :-)";
  private static final String RESULT1 = "Here is <a href=\"http://www.linux.org.ru\">www.linux.org.ru</a>, have fun! :-)";

  private static final String TEXT2 = "Here is http://linux.org.ru, have fun! :-)";
  private static final String RESULT2 = "Here is <a href=\"http://linux.org.ru\">http://linux.org.ru</a>, have fun! :-)";

  private static final String TEXT3 = "Long url: http://www.linux.org.ru/profile/maxcom/view-message.jsp?msgid=1993651";
  private static final String RESULT3 = "Long url: <a href=\"http://www.linux.org.ru/profile/maxcom/view-message.jsp?msgid=1993651\">http://www.linux....</a>";

  private static final String TEXT4 = "Forced wrapping: 12345678901234567890123456789";
  private static final String RESULT4 = "Forced wrapping: 1234567890123456789 0123456789";

  private static final String TEXT6 = "123&nbsp;4";

  private static final String TEXT7 = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
  private static final String RESULT7 = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";

  private static final String TEXT8 = "Long url: http://www.linux.org.ru/profile/maxcom/view-message.jsp?msgid=1993651&a=b";
  private static final String RESULT8 = "Long url: <a href=\"http://www.linux.org.ru/profile/maxcom/view-message.jsp?msgid=1993651&amp;a=b\">http://www.linux....</a>";

  private static final String QUOTING1 = "> 1";
  private static final String RESULT_QUOTING1 = "<i>&gt; 1</i>";

  private static final String QUOTING2 = "> 1\n2";
  private static final String RESULT_QUOTING2 = "<i>&gt; 1\n2</i>";

  private static final String QUOTING3 = "> 1\n2\n\n3";
  private static final String RESULT_QUOTING3 = "<i>&gt; 1\n2\n</i><p>\n3";

  private static final String TEXT9 = "(http://ru.wikipedia.org/wiki/Blah_(blah))";
  private static final String RESULT9 = "(<a href=\"http://ru.wikipedia.org/wiki/Blah_(blah)\">http://ru.wikipedia.org/wiki/Blah_(blah)</a>)";

  private static final String GUARANTEED_CRASH = "\"http://www.google.com/\"";

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

  public void testURLHighlight5() throws UtilException {
    HTMLFormatter formatter = new HTMLFormatter(TEXT9);

    formatter.enableUrlHighLightMode();

    assertEquals(formatter.process(), RESULT9);
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

  public void testQuiting1() throws UtilException {
    HTMLFormatter formatter = new HTMLFormatter(QUOTING1);

    formatter.enableTexNewLineMode();
    formatter.enableQuoting();

    assertEquals(RESULT_QUOTING1, formatter.process());
  }

  public void testQuiting2() throws UtilException {
    HTMLFormatter formatter = new HTMLFormatter(QUOTING2);

    formatter.enableTexNewLineMode();
    formatter.enableQuoting();

    assertEquals(RESULT_QUOTING2, formatter.process());
  }

  public void testQuiting3() throws UtilException {
    HTMLFormatter formatter = new HTMLFormatter(QUOTING3);

    formatter.enableTexNewLineMode();
    formatter.enableQuoting();

    assertEquals(RESULT_QUOTING3, formatter.process());
  }

  public void testEntityCrash(){
    HTMLFormatter formatter = new HTMLFormatter(GUARANTEED_CRASH);
    formatter.enablePreformatMode();
    formatter.enableUrlHighLightMode();
    try{
      //BANG!!!!
      String r = formatter.process();
      assertEquals("<pre>&quot;<a href=\"http://www.google.com/\">http://www.google.com/</a>&quot;</pre>", r);
    }catch (StringIndexOutOfBoundsException e){
      fail("It seems, it should not happen?");
    }
  }
}
