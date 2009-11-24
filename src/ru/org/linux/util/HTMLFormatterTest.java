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

import org.hamcrest.CoreMatchers;
import static org.junit.Assert.*;
import org.junit.Test;

public class HTMLFormatterTest {
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
  private static final String RESULT_QUOTING1_NOQUOTING = "&gt; 1";

  private static final String QUOTING2 = "> 1\n2";
  private static final String RESULT_QUOTING2 = "<i>&gt; 1\n2</i>";

  private static final String QUOTING3 = "> 1\n2\n\n3";
  private static final String RESULT_QUOTING3 = "<i>&gt; 1\n2\n</i><p>\n3";

  private static final String TEXT9 = "(http://ru.wikipedia.org/wiki/Blah_(blah))";
  private static final String RESULT9 = "(<a href=\"http://ru.wikipedia.org/wiki/Blah_(blah)\">http://ru.wikipedia.org/wiki/Blah_(blah)</a>)";

  private static final String GUARANTEED_CRASH = "\"http://www.google.com/\"";

  private static final String LINK_WITH_UNDERSCORE = "http://www.phoronix.com/scan.php?page=article&item=intel_core_i7&num=1";
  private static final String LINK_WITH_PARAM_ONLY = "http://www.phoronix.com/scan.php?page=article#anchor";
  private static final String RFC1738 = "http://www.phoronix.com/scan.php?page=article&item=intel_core_i7&Мама_мыла_раму&$-_.+!*'(,)=$-_.+!*'(),#anchor";
  private static final String CYR_LINK = "http://ru.wikipedia.org/wiki/Литературный_'негр'(Fran\u00C7ais\u0152uvre_\u05d0)?негр=эфиоп&эфиоп";
  private static final String GOOGLE_CACHE = "http://74.125.95.132/search?q=cache:fTsc8ze3IxIJ:forum.springsource.org/showthread.php%3Ft%3D53418+spring+security+openid&cd=1&hl=en&ct=clnk&gl=us";

  private static final String URL_WITH_AT = "http://www.mail-archive.com/samba@lists.samba.org/msg58308.html";
  private static final String Latin1Supplement = "http://de.wikipedia.org/wiki/Großes_ß#Unicode";
  private static final String greek = "http://el.wikipedia.org/wiki/άλλες";
  private static final String QP = "http://www.ozon.ru/?context=search&text=%D8%E8%EB%E4%F2";
  private static final String EMPTY_ANCHOR = "http://www.google.com/#";
  private static final String SLASH_AFTER_AMP = "http://extensions.joomla.org/extensions/communities-&-groupware/ratings-&-reviews/5483/details";

  @Test
  public void testURLHighlight() throws UtilException {
    HTMLFormatter formatter = new HTMLFormatter(TEXT1);

    formatter.enableUrlHighLightMode();

    assertEquals(RESULT1, formatter.process());
  }

  @Test
  public void testURLHighlight2() throws UtilException {
    HTMLFormatter formatter = new HTMLFormatter(TEXT2);

    formatter.enableUrlHighLightMode();

    assertEquals(RESULT2, formatter.process());
  }

  @Test
  public void testURLHighlight3() throws UtilException {
    HTMLFormatter formatter = new HTMLFormatter(TEXT3);

    formatter.enableUrlHighLightMode();
    formatter.setMaxLength(20);

    assertEquals(RESULT3, formatter.process());
  }

  @Test
  public void testURLHighlight4() throws UtilException {
    HTMLFormatter formatter = new HTMLFormatter(TEXT8);

    formatter.enableUrlHighLightMode();
    formatter.setMaxLength(20);

    assertEquals(RESULT8, formatter.process());
  }

  @Test
  public void testURLHighlight5() throws UtilException {
    HTMLFormatter formatter = new HTMLFormatter(TEXT9);

    formatter.enableUrlHighLightMode();

    assertEquals(RESULT9, formatter.process());
  }

  @Test
  public void testWrap1() throws UtilException {
    HTMLFormatter formatter = new HTMLFormatter(TEXT4);

    formatter.setMaxLength(20);

    assertEquals(RESULT4, formatter.process());
  }

  @Test
  public void testCountSGML1() throws UtilException {
    assertEquals(HTMLFormatter.countCharacters(TEXT4), TEXT4.length());
  }

  @Test
  public void testCountSGML2() throws UtilException {
    assertEquals(5, HTMLFormatter.countCharacters(TEXT6));
  }

  @Test
  public void testWrapSGML() throws UtilException {
    assertEquals(TEXT7, HTMLFormatter.wrapLongLine(TEXT7, 35, " "));
  }

  @Test
  public void testWrapSGML2() throws UtilException {
    assertEquals(RESULT7, HTMLFormatter.wrapLongLine(TEXT7, 20, " "));
  }

  @Test
  public void testQuiting1() throws UtilException {
    HTMLFormatter formatter = new HTMLFormatter(QUOTING1);

    formatter.enableTexNewLineMode();
    formatter.enableQuoting();

    assertEquals(RESULT_QUOTING1, formatter.process());
  }

  @Test
  public void testNoQuiting() throws UtilException {
    HTMLFormatter formatter = new HTMLFormatter(QUOTING1);

    formatter.enableTexNewLineMode();

    assertEquals(RESULT_QUOTING1_NOQUOTING, formatter.process());
  }

  @Test
  public void testQuiting2() throws UtilException {
    HTMLFormatter formatter = new HTMLFormatter(QUOTING2);

    formatter.enableTexNewLineMode();
    formatter.enableQuoting();

    assertEquals(RESULT_QUOTING2, formatter.process());
  }

  @Test
  public void testQuiting3() throws UtilException {
    HTMLFormatter formatter = new HTMLFormatter(QUOTING3);

    formatter.enableTexNewLineMode();
    formatter.enableQuoting();

    assertEquals(RESULT_QUOTING3, formatter.process());
  }

  @Test
  public void testEntityCrash() {
    HTMLFormatter formatter = new HTMLFormatter(GUARANTEED_CRASH);
    formatter.enableUrlHighLightMode();
    try {
      String r = formatter.process();
      assertEquals("&quot;<a href=\"http://www.google.com/\">http://www.google.com/</a>&quot;", r);
    } catch (StringIndexOutOfBoundsException e) {
      fail("It seems, it should not happen?");
    }
  }

  @Test
  public void testUndescore() {
    HTMLFormatter formatter = new HTMLFormatter(LINK_WITH_UNDERSCORE);
    formatter.enableUrlHighLightMode();
    String s = formatter.process();
    assertTrue("Whole text must be formatted as link: " + s, s.endsWith("</a>"));
  }

  @Test
  public void testWithParamOnly() {
    HTMLFormatter formatter = new HTMLFormatter(LINK_WITH_PARAM_ONLY);
    formatter.enableUrlHighLightMode();
    String s = formatter.process();
    assertTrue("Whole text must be formatted as link: " + s, s.endsWith("</a>"));
  }

  @Test
  public void testWithCyrillic() {
    HTMLFormatter formatter = new HTMLFormatter(RFC1738);
    formatter.enableUrlHighLightMode();
    String s = formatter.process();
    assertTrue("Whole text must be formatted as link: " + s, s.endsWith("</a>"));
  }

  @Test
  public void testNlSubstition() {
    String s = HTMLFormatter.nl2br("This is a line\nwith break inside it");
    Integer i = s.indexOf("<br>");
    assertThat("Newline is changed to <br>", i, CoreMatchers.not(-1));
  }

  @Test
  public void testStringEscape() {
    String str = "This is an entity &#1999;";
    String s = HTMLFormatter.htmlSpecialChars(str);
    assertThat("String should remaint unescaped", s, CoreMatchers.equalTo(str));
  }

  @Test
  public void testAmpEscape() {
    String str = "a&b";
    String s = HTMLFormatter.htmlSpecialChars(str);
    assertThat("Ampersand should be escaped", s, CoreMatchers.equalTo("a&amp;b"));
  }

  @Test
  public void testParaSubstition() {
    String str = "this is a line\n\r\n\rwith some\n\nlinebreaks in it";
    String s = HTMLFormatter.texnl2br(str, false, false);
    Integer i = s.indexOf("<p>");
    assertThat("Newlines is changed to <p>", i, CoreMatchers.not(-1));
    Integer b = s.indexOf("<p>", i + 3);
    assertThat("Wait, there should be two paras", b, CoreMatchers.not(-1));
  }

  @Test
  public void testCyrillicLink() {
    HTMLFormatter formatter = new HTMLFormatter(CYR_LINK);
    formatter.enableUrlHighLightMode();
    String s = formatter.process();
    assertTrue("All text should be inside link", s.endsWith("</a>"));
  }

  @Test
  public void testGoogleCache() {
    HTMLFormatter formatter = new HTMLFormatter(GOOGLE_CACHE);
    formatter.enableUrlHighLightMode();
    String s = formatter.process();
    assertTrue("All text should be inside link", s.endsWith("</a>"));
  }

  @Test
  public void testURLWithAt() {
    HTMLFormatter formatter = new HTMLFormatter(URL_WITH_AT);
    formatter.enableUrlHighLightMode();
    String s = formatter.process();
    assertTrue("All text should be inside link", s.endsWith("</a>"));
  }

  @Test
  public void testLatin1Supplement() {
    HTMLFormatter formatter = new HTMLFormatter(Latin1Supplement);
    formatter.enableUrlHighLightMode();
    String s2 = formatter.process();
    assertTrue("All text should be inside link", s2.endsWith("</a>"));
  }

  @Test
  public void testGreek() {
    HTMLFormatter formatter = new HTMLFormatter(greek);
    formatter.enableUrlHighLightMode();
    String s2 = formatter.process();
    assertTrue("All text should be inside link", s2.endsWith("</a>"));
  }

  @Test
  public void testQP(){
    HTMLFormatter formatter = new HTMLFormatter(QP);
    formatter.enableUrlHighLightMode();
    String s2 = formatter.process();
    assertTrue("All text should be inside link", s2.endsWith("</a>"));
  }

  @Test
  public void testEmptyAnchor(){
    HTMLFormatter formatter = new HTMLFormatter(EMPTY_ANCHOR);
    formatter.enableUrlHighLightMode();
    String s2 = formatter.process();
    assertTrue("All text should be inside link", s2.endsWith("</a>"));
  }

  @Test
  public void testSlashAfterAmp(){
    HTMLFormatter formatter = new HTMLFormatter(SLASH_AFTER_AMP);
    formatter.enableUrlHighLightMode();
    String s2 = formatter.process();
    assertTrue("All text should be inside link", s2.endsWith("</a>"));
    int blankIndex = s2.indexOf(' ');
    int lastIndex = s2.lastIndexOf(' ');
    assertThat("No whitespace inside link", blankIndex, CoreMatchers.equalTo(lastIndex));
    //whitespace shoud separate href attribute. no other ws should occur
  }

  @Test
  public void testBBCode1() {
    HTMLFormatter f = new HTMLFormatter("test\n\ntest\ntest");
    f.setOutputLorcode(true);
    f.enableQuoting();
    f.enableTexNewLineMode();

    assertEquals("test\n\ntest\ntest", f.process());
  }

  @Test
  public void testBBCode2() {
    HTMLFormatter f = new HTMLFormatter("www.linux.org.ru");
    f.setOutputLorcode(true);
    f.enableQuoting();
    f.enableUrlHighLightMode();

    assertEquals("[url=http://www.linux.org.ru]www.linux.org.ru[/url]", f.process());
  }

  @Test
  public void testBBCode3() {
    HTMLFormatter f = new HTMLFormatter("http://www.linux.org.ru/");
    f.setOutputLorcode(true);
    f.enableQuoting();
    f.enableUrlHighLightMode();

    assertEquals("[url=http://www.linux.org.ru/]http://www.linux.org.ru/[/url]", f.process());
  }

  @Test
  public void testBBCode4() {
    HTMLFormatter f = new HTMLFormatter(">test\n\ntest");
    f.setOutputLorcode(true);
    f.enableQuoting();
    f.enableUrlHighLightMode();
    f.enableTexNewLineMode();

    assertEquals("\n[i]>test\n[/i]\n\ntest", f.process());
  }

  @Test
  public void testBBCode5() {
    HTMLFormatter f = new HTMLFormatter("<>");
    f.setOutputLorcode(true);
    f.enableQuoting();
    f.enableUrlHighLightMode();
    f.enableTexNewLineMode();

    assertEquals("<>", f.process());
  }

  @Test
  public void testBBCode6() {
    HTMLFormatter f = new HTMLFormatter("test http://www.linux.org.ru/jump-message.jsp?msgid=4238459&cid=4240245 test");
    f.setOutputLorcode(true);
    f.enableQuoting();
    f.enableUrlHighLightMode();
    f.enableTexNewLineMode();

    assertEquals("test [url=http://www.linux.org.ru/jump-message.jsp?msgid=4238459&cid=4240245]http://www.linux.org.ru/jump-message.jsp?msgid=4238459&cid=4240245[/url] test", f.process());
  }

  @Test
  public void testBBCode7() {
    HTMLFormatter f = new HTMLFormatter("test\n\n>test");
    f.setOutputLorcode(true);
    f.enableQuoting();
    f.enableUrlHighLightMode();
    f.enableTexNewLineMode();

    assertEquals("test\n\n[i]\n>test[/i]", f.process());
  }

  @Test
  public void testBBCode8() {
    HTMLFormatter f = new HTMLFormatter("test &");
    f.setOutputLorcode(true);
    f.enableQuoting();
    f.enableUrlHighLightMode();
    f.enableTexNewLineMode();

    assertEquals("test &", f.process());
  }

  @Test
  public void testBBCode9() {
    HTMLFormatter f = new HTMLFormatter("test\r\ntest");
    f.setOutputLorcode(true);
    f.enableUrlHighLightMode();
    f.enableNewLineMode();

    assertEquals("test[br]\ntest", f.process());
  }

  @Test
  public void testBBCode10() {
    HTMLFormatter f = new HTMLFormatter("test\ntest");
    f.setOutputLorcode(true);
    f.enableNewLineMode();
    f.enableUrlHighLightMode();
    f.enableQuoting();

    assertEquals("test[br]\ntest", f.process());
  }

  @Test
  public void testBBCode11() {
    HTMLFormatter f = new HTMLFormatter(">test\ntest");
    f.setOutputLorcode(true);
    f.enableNewLineMode();
    f.enableUrlHighLightMode();
    f.enableQuoting();

    assertEquals("[i]>test[/i][br]\ntest", f.process());
  }

}
