/*
 * Copyright 1998-2010 Linux.org.ru
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

import org.apache.commons.httpclient.URI;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import ru.org.linux.site.Group;
import ru.org.linux.site.Message;
import ru.org.linux.spring.Configuration;
import ru.org.linux.spring.dao.MessageDao;
import ru.org.linux.util.formatter.ToHtmlFormatter;
import ru.org.linux.util.formatter.ToLorCodeFormatter;
import ru.org.linux.util.formatter.ToLorCodeTexFormatter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HTMLFormatterTest {
  private static final String TEXT1 = "Here is www.linux.org.ru, have fun! :-)";
  private static final String RESULT1 = "Here is <a href=\"http://www.linux.org.ru\">www.linux.org.ru</a>, have fun! :-)";

  private static final String TEXT2 = "Here is http://linux.org.ru, have fun! :-)";
  private static final String RESULT2 = "Here is <a href=\"http://linux.org.ru\">http://linux.org.ru</a>, have fun! :-)";

  private static final String TEXT3 = "Long url: http://www.linux.org.ru/profile/maxcom/view-message.jsp?msgid=1993651";
  private static final String RESULT3 = "Long url: <a href=\"http://www.linux.org.ru/profile/maxcom/view-message.jsp?msgid=1993651\">www.linux.org.ru/pro...</a>";

  private static final String TEXT8 = "Long url: http://www.linux.org.ru/profile/maxcom/view-message.jsp?msgid=1993651&a=b";
  private static final String RESULT8 = "Long url: <a href=\"http://www.linux.org.ru/profile/maxcom/view-message.jsp?msgid=1993651&amp;a=b\">www.linux.org.ru/pro...</a>";

  private static final String QUOTING1 = "> 1";
  private static final String RESULT_QUOTING1 = "\n[i]> 1[/i]";
  private static final String RESULT_QUOTING1_NOQUOTING = "> 1";

  private static final String QUOTING2 = "> 1\n2";
  private static final String RESULT_QUOTING2 = "\n[i]> 1\n2[/i]";

  private static final String QUOTING3 = "> 1\n2\n\n3";
  private static final String RESULT_QUOTING3 = "\n[i]> 1\n2\n[/i]\n\n3";

  private static final String TEXT9 = "(http://ru.wikipedia.org/wiki/Blah_(blah))";
  private static final String RESULT9 = "(<a href=\"http://ru.wikipedia.org/wiki/Blah_(blah)\">http://ru.wikipedia.org/wiki/Blah_(blah)</a>)";
  
  private static final String TEXT10 = "Twitter url: https://twitter.com/#!/l_o_r";
  private static final String RESULT10 = "Twitter url: <a href=\"https://twitter.com/#!/l_o_r\">https://twitter.com/#!/l_o_r</a>";

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

  private ToHtmlFormatter toHtmlFormatter;
  private ToHtmlFormatter toHtmlFormatter20;
  private ToLorCodeFormatter toLorCodeFormatter;
  private ToLorCodeTexFormatter toLorCodeTexFormatter;
  private Configuration configuration;
  private URI mainURI;
  private MessageDao messageDao;
  private Message message1;
  private Group group1;
  private Message message2;
  private Group group2;
  private Message message3;
  private Group group3;
  private Message message12;
  private Group group12;


  @Before
  public void init() throws Exception {
    mainURI = new URI("http://www.linux.org.ru/",true, "UTF-8");

    messageDao = mock(MessageDao.class);
    message1 = mock(Message.class);
    group1 = mock(Group.class);
    message2 = mock(Message.class);
    group2 = mock(Group.class);
    message3 = mock(Message.class);
    group3 = mock(Group.class);
    message12 = mock(Message.class);
    group12 = mock(Group.class);

    when(message1.getTitle()).thenReturn("привет1");
    when(message2.getTitle()).thenReturn("привет2");
    when(message3.getTitle()).thenReturn("привет3");
    when(message12.getTitle()).thenReturn("привет12");
    when(group1.getUrl()).thenReturn("/news/debian/");
    when(group2.getUrl()).thenReturn("/forum/talks/");
    when(group3.getUrl()).thenReturn("/forum/general/");
    when(group12.getUrl()).thenReturn("/forum/security/");
    when(messageDao.getGroup(message1)).thenReturn(group1);
    when(messageDao.getGroup(message2)).thenReturn(group2);
    when(messageDao.getGroup(message3)).thenReturn(group3);
    when(messageDao.getGroup(message12)).thenReturn(group12);
    when(messageDao.getById(6753486)).thenReturn(message1);
    when(messageDao.getById(6893165)).thenReturn(message2);
    when(messageDao.getById(6890857)).thenReturn(message3);
    when(messageDao.getById(1948661)).thenReturn(message12);

    configuration = mock(Configuration.class);

    when(configuration.getMainURI()).thenReturn(mainURI);

    toHtmlFormatter = new ToHtmlFormatter();
    toHtmlFormatter.setConfiguration(configuration);
    toHtmlFormatter.setMessageDao(messageDao);

    toHtmlFormatter20 = new ToHtmlFormatter();
    toHtmlFormatter20.setConfiguration(configuration);
    toHtmlFormatter20.setMessageDao(messageDao);
    toHtmlFormatter20.setMaxLength(20);

    toLorCodeTexFormatter = new ToLorCodeTexFormatter();
    toLorCodeFormatter = new ToLorCodeFormatter();
  }

  @Test
  public void testToHtmlFormatter() {
    assertEquals(RESULT1, toHtmlFormatter.format(TEXT1, false));
    assertEquals(RESULT2, toHtmlFormatter.format(TEXT2, false));
    assertEquals(RESULT3, toHtmlFormatter20.format(TEXT3, false));
    assertEquals(RESULT8, toHtmlFormatter20.format(TEXT8, false));
    assertEquals(RESULT9, toHtmlFormatter.format(TEXT9, false));
    assertEquals(RESULT10, toHtmlFormatter.format(TEXT10, false));
    assertTrue(toHtmlFormatter.format(LINK_WITH_UNDERSCORE, false).endsWith("</a>"));
    assertTrue(toHtmlFormatter.format(LINK_WITH_PARAM_ONLY, false).endsWith("</a>"));
    assertTrue(toHtmlFormatter.format(RFC1738, false).endsWith("</a>"));
    assertTrue(toHtmlFormatter.format(CYR_LINK, false).endsWith("</a>"));
    assertTrue(toHtmlFormatter.format(GOOGLE_CACHE, false).endsWith("</a>"));
    assertTrue(toHtmlFormatter.format(URL_WITH_AT, false).endsWith("</a>"));
    assertTrue(toHtmlFormatter.format(Latin1Supplement, false).endsWith("</a>"));
    assertTrue(toHtmlFormatter.format(greek, false).endsWith("</a>"));
    assertTrue(toHtmlFormatter.format(QP, false).endsWith("</a>"));
    assertTrue(toHtmlFormatter.format(EMPTY_ANCHOR, false).endsWith("</a>"));
    assertTrue(toHtmlFormatter.format(SLASH_AFTER_AMP, false).endsWith("</a>"));
  }

  @Test
  public void testToLorCodeTexFormatter() {
    assertEquals(RESULT_QUOTING1, toLorCodeTexFormatter.format(QUOTING1, true));
    assertEquals(RESULT_QUOTING1_NOQUOTING, toLorCodeTexFormatter.format(QUOTING1, false));
    assertEquals(RESULT_QUOTING2, toLorCodeTexFormatter.format(QUOTING2, true));
    assertEquals(RESULT_QUOTING3, toLorCodeTexFormatter.format(QUOTING3, true));

    assertEquals("\n[i]>test\n[/i]\n\ntest", toLorCodeTexFormatter.format(">test\n\ntest", true)); // 4
    assertEquals("test\n\ntest\ntest", toLorCodeTexFormatter.format("test\n\ntest\ntest", true)); // 1
    assertEquals("test\n\n[i]\n>test[/i]", toLorCodeTexFormatter.format("test\n\n>test", true)); // 7
    assertEquals("test &", toLorCodeTexFormatter.format("test &", true)); // 8
    assertEquals("test[br]\ntest", toLorCodeFormatter.format("test\r\ntest", true)); // 9
    assertEquals("test[br]\ntest", toLorCodeFormatter.format("test\ntest", true)); // 10
    assertEquals("[i]>test[/i][br]\ntest", toLorCodeFormatter.format(">test\ntest", true)); // 11
    assertEquals("[i]>test[/i][i][br]\n>test[/i]", toLorCodeFormatter.format(">test\n>test", true)); // 12
  }

  @Test
  public void testURLs() {
    String url1 = "http://www.linux.org.ru/forum/general/6890857/page2?lastmod=1319022386177#comment-6892917";
    String url3 = "http://www.linux.org.ru/jump-message.jsp?msgid=1948661&cid=1948675";
    assertEquals("<a href=\"http://www.linux.org.ru/forum/general/6890857?cid=6892917\" title=\"привет3\">www.linux.org.ru/forum/general/6890857/page2?lastmod=1319022386177#comment-68929...</a>",
        toHtmlFormatter.format(url1,false));
    assertEquals("<a href=\"http://www.linux.org.ru/forum/security/1948661?cid=1948675\" title=\"привет12\">www.linux.org.ru/jump-message.jsp?msgid=1948661&amp;cid=1948675</a>",
        toHtmlFormatter.format(url3,false));
  }


  @Test
  public void testCrash() {
    try {
      assertEquals("&quot;<a href=\"http://www.google.com/&quot;\">http://www.google.com/&quot;</a>",
          toHtmlFormatter.format(GUARANTEED_CRASH, false));
    } catch (Exception e) {
      fail("It seems, it should not happen?");
    }
  }

  @Test
  public void testHTMLEscape() {
    String str = "This is an entity &#1999;";
    String s = StringUtil.escapeHtml(str);
    assertThat("String should remaint unescaped", s, CoreMatchers.equalTo(str));

    str = "a&b";
    s = StringUtil.escapeHtml(str);
    assertThat("Ampersand should be escaped", s, CoreMatchers.equalTo("a&amp;b"));

    assertEquals("&lt;script&gt;", StringUtil.escapeHtml("<script>"));
    assertEquals("&nbsp;", StringUtil.escapeHtml("&nbsp;"));
    assertEquals("&#41;&#41;&#41;", StringUtil.escapeHtml("&#41;&#41;&#41;"));
  }
}
