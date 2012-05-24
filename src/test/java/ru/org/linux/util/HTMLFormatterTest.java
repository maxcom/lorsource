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

package ru.org.linux.util;

import org.apache.commons.httpclient.URI;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import ru.org.linux.comment.Comment;
import ru.org.linux.comment.CommentDao;
import ru.org.linux.group.Group;
import ru.org.linux.spring.Configuration;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.util.bbcode.LorCodeService;
import ru.org.linux.util.formatter.ToHtmlFormatter;
import ru.org.linux.util.formatter.ToLorCodeFormatter;
import ru.org.linux.util.formatter.ToLorCodeTexFormatter;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.org.linux.util.bbcode.tags.QuoteTag.citeFooter;
import static ru.org.linux.util.bbcode.tags.QuoteTag.citeHeader;

public class HTMLFormatterTest {
  private static final String TEXT1 = "Here is www.linux.org.ru, have fun! :-)";
  private static final String RESULT1 = "Here is <a href=\"http://www.linux.org.ru\">www.linux.org.ru</a>, have fun! :-)";

  private static final String TEXT2 = "Here is http://linux.org.ru, have fun! :-)";
  private static final String RESULT2 = "Here is <a href=\"http://linux.org.ru\">http://linux.org.ru</a>, have fun! :-)";

  private static final String TEXT3 = "Long url: http://www.linux.org.ru/profile/maxcom/view-message.jsp?msgid=1993651";
  private static final String RESULT3 = "Long url: <a href=\"http://www.linux.org.ru/profile/maxcom/view-message.jsp?msgid=1993651\">www.linux.org.ru/pro...</a>";

  private static final String TEXT8 = "Long url: http://www.linux.org.ru/profile/maxcom/view-message.jsp?msgid=1993651&a=b";
  private static final String RESULT8 = "Long url: <a href=\"http://www.linux.org.ru/profile/maxcom/view-message.jsp?msgid=1993651&amp;a=b\">www.linux.org.ru/pro...</a>";

  private static final String TEXT9 = "(http://ru.wikipedia.org/wiki/Blah_(blah))";
  private static final String RESULT9 = "(<a href=\"http://ru.wikipedia.org/wiki/Blah_(blah)\">http://ru.wikipedia.org/wiki/Blah_(blah)</a>)";
  
  private static final String TEXT10 = "Twitter url: https://twitter.com/#!/l_o_r";
  private static final String RESULT10 = "Twitter url: <a href=\"https://twitter.com/#!/l_o_r\">https://twitter.com/#!/l_o_r</a>";

  private static final String TEXT11 = "Long url: http://www.google.com.ua/search?client=opera&rls=en&q=InsireData&sourceid=opera&ie=utf-8&oe=utf-8&channel=suggest#sclient=psy-ab&hl=uk&client=opera&hs=kZt&rls=en&channel=suggest&source=hp&q=InsireData+lisp&pbx=1&oq=InsireData+lisp&aq=f&aqi=&aql=1&gs_sm=e&gs_upl=3936l5946l0l6137l13l9l3l0l0l0l253l1481l0.6.2l10l0&bav=on.2,or.r_gc.r_pw.,cf.osb&fp=35c95703241399bd&biw=1271&bih=694";
  private static final String RESULT11 = "Long url: <a href=\"http://www.google.com.ua/search?client=opera&amp;rls=en&amp;q=InsireData&amp;sourceid=opera&amp;ie=utf-8&amp;oe=utf-8&amp;channel=suggest#sclient=psy-ab&amp;hl=uk&amp;client=opera&amp;hs=kZt&amp;rls=en&amp;channel=suggest&amp;source=hp&amp;q=InsireData+lisp&amp;pbx=1&amp;oq=InsireData+lisp&amp;aq=f&amp;aqi=&amp;aql=1&amp;gs_sm=e&amp;gs_upl=3936l5946l0l6137l13l9l3l0l0l0l253l1481l0.6.2l10l0&amp;bav=on.2,or.r_gc.r_pw.,cf.osb&amp;fp=35c95703241399bd&amp;biw=1271&amp;bih=694\">http://www.google.com.ua/search?client=opera&amp;rls=en&amp;q=InsireData&amp;sou...</a>";

  private static final String TEXT12 = "with login: ftp://olo:olor@o.example.org/olo/2a-ep4ce22/2a-ep4ce22_introduction.pdf";
  private static final String RESULT12 = "with login: <a href=\"ftp://olo:olor@o.example.org/olo/2a-ep4ce22/2a-ep4ce22_introduction.pdf\">ftp://olo:olor@o.example.org/olo/2a-ep4ce22/2a-ep4ce22_introduction.pdf</a>";

  private static final String TEXT13 = "with login: ftp://olor@o.example.org/olo/2a-ep4ce22/2a-ep4ce22_introduction.pdf";
  private static final String RESULT13 = "with login: <a href=\"ftp://olor@o.example.org/olo/2a-ep4ce22/2a-ep4ce22_introduction.pdf\">ftp://olor@o.example.org/olo/2a-ep4ce22/2a-ep4ce22_introduction.pdf</a>";
  
  private static final String TEXT14 = "with first one symbol: http://a.test.com";
  private static final String RESULT14 = "with first one symbol: <a href=\"http://a.test.com\">http://a.test.com</a>";

  private static final String TEXT15 = "with www: www.test.com";
  private static final String RESULT15 = "with www: <a href=\"http://www.test.com\">http://www.test.com</a>";

  private static final String TEXT16 = "with ftp: ftp.test.com";
  private static final String RESULT16 = "with ftp: <a href=\"ftp://ftp.test.com\">ftp://ftp.test.com</a>";
  
  private static final String TEXT17 = "http://translate.google.com/?sl=en&tl=ru#ru|en|%D0%BE%D1%81%D1%91%D0%BB";
  private static final String RESULT17 = "<a href=\"http://translate.google.com/?sl=en&amp;tl=ru#ru|en|%D0%BE%D1%81%D1%91%D0%BB\">http://translate.google.com/?sl=en&amp;tl=ru#ru|en|осёл</a>";
  
  private static final String TEXT17_2 = "http://translate.google.com/?sl=en&amp;tl=ru#ru|en|осёл";
  private static final String RESULT17_2 = "<a href=\"http://translate.google.com/?sl=en&amp;tl=ru#ru%7Cen%7C%D0%BE%D1%81%D1%91%D0%BB\">http://translate.google.com/?sl=en&amp;tl=ru#ru|en|осёл</a>";
  
  private static final String TEXT18 = "http://smartphonebenchmarks.com/index.php?filter_model[]=all&filter_cpu[]=Qualcomm+Snapdragon+MSM8255&filter_cpu[]=Texas+Instrument+OMAP+3610";
  private static final String RESULT18 = "<a href=\"http://smartphonebenchmarks.com/index.php?filter_model%5B%5D=all&amp;filter_cpu%5B%5D=Qualcomm+Snapdragon+MSM8255&amp;filter_cpu%5B%5D=Texas+Instrument+OMAP+3610\">http://smartphonebenchmarks.com/index.php?filter_model[]=all&amp;filter_cpu[]=Qu...</a>";


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
  private LorCodeService lorCodeService;
  private ToLorCodeTexFormatter toLorCodeTexFormatter;

  @Before
  public void init() throws Exception {
    lorCodeService = new LorCodeService();

    URI mainURI = new URI("http://www.linux.org.ru/", true, "UTF-8");

    TopicDao messageDao = mock(TopicDao.class);
    Topic message1 = mock(Topic.class);
    Group group1 = mock(Group.class);
    Topic message2 = mock(Topic.class);
    Group group2 = mock(Group.class);
    Topic message3 = mock(Topic.class);
    Group group3 = mock(Group.class);
    Topic message12 = mock(Topic.class);
    Group group12 = mock(Group.class);
    Topic message15 = mock(Topic.class);
    Group group15 = mock(Group.class);
    Topic messageHistory = mock(Topic.class);
    Group groupHistory = mock(Group.class);
    CommentDao commentDao = mock(CommentDao.class);

    Comment comment = mock(Comment.class);

    when(message1.getTitle()).thenReturn("привет1");
    when(message2.getTitle()).thenReturn("привет2");
    when(message3.getTitle()).thenReturn("привет3");
    when(message12.getTitle()).thenReturn("привет12");
    when(message15.getTitle()).thenReturn("привет15");
    when(messageHistory.getTitle()).thenReturn("привет история");
    when(group1.getUrl()).thenReturn("/news/debian/");
    when(group2.getUrl()).thenReturn("/forum/talks/");
    when(group3.getUrl()).thenReturn("/forum/general/");
    when(group12.getUrl()).thenReturn("/forum/security/");
    when(group15.getUrl()).thenReturn("/forum/linux-org-ru/");
    when(groupHistory.getUrl()).thenReturn("/news/kernel/");
    when(messageDao.getGroup(message1)).thenReturn(group1);
    when(messageDao.getGroup(message2)).thenReturn(group2);
    when(messageDao.getGroup(message3)).thenReturn(group3);
    when(messageDao.getGroup(message12)).thenReturn(group12);
    when(messageDao.getGroup(message15)).thenReturn(group15);
    when(messageDao.getGroup(messageHistory)).thenReturn(groupHistory);
    when(messageDao.getById(6753486)).thenReturn(message1);
    when(messageDao.getById(6893165)).thenReturn(message2);
    when(messageDao.getById(6890857)).thenReturn(message3);
    when(messageDao.getById(1948661)).thenReturn(message12);
    when(messageDao.getById(6944260)).thenReturn(message15);
    when(messageDao.getById(6992532)).thenReturn(messageHistory);
    when(commentDao.getById(6892917)).thenReturn(comment);
    when(commentDao.getById(1948675)).thenReturn(comment);
    when(commentDao.getById(6944831)).thenReturn(comment);

    Configuration configuration = mock(Configuration.class);

    when(configuration.getMainURI()).thenReturn(mainURI);

    toHtmlFormatter = new ToHtmlFormatter();
    toHtmlFormatter.setConfiguration(configuration);
    toHtmlFormatter.setMessageDao(messageDao);
    toHtmlFormatter.setCommentDao(commentDao);

    toHtmlFormatter20 = new ToHtmlFormatter();
    toHtmlFormatter20.setConfiguration(configuration);
    toHtmlFormatter20.setMessageDao(messageDao);
    toHtmlFormatter20.setMaxLength(20);
    toHtmlFormatter20.setCommentDao(commentDao);

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
    assertEquals(RESULT11, toHtmlFormatter.format(TEXT11, false));
    assertEquals(RESULT12, toHtmlFormatter.format(TEXT12, false));
    assertEquals(RESULT13, toHtmlFormatter.format(TEXT13, false));
    assertEquals(RESULT14, toHtmlFormatter.format(TEXT14, false));
    assertEquals(RESULT15, toHtmlFormatter.format(TEXT15, false));
    assertEquals(RESULT16, toHtmlFormatter.format(TEXT16, false));
    assertEquals(RESULT17, toHtmlFormatter.format(TEXT17, false));
    assertEquals(RESULT17_2, toHtmlFormatter.format(TEXT17_2, false));
    assertEquals(RESULT18, toHtmlFormatter.format(TEXT18, false));
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
  public void testURLs() {
    String url1 = "http://www.linux.org.ru/forum/general/6890857/page2?lastmod=1319022386177#comment-6892917";
    assertEquals("<a href=\"http://www.linux.org.ru/forum/general/6890857?cid=6892917\" title=\"привет3\">www.linux.org.ru/forum/general/6890857/page2?lastmod=1319022386177#comment-68929...</a>",
        toHtmlFormatter.format(url1,false));
    String url3 = "http://www.linux.org.ru/jump-message.jsp?msgid=1948661&cid=1948675";
    assertEquals("<a href=\"http://www.linux.org.ru/forum/security/1948661?cid=1948675\" title=\"привет12\">www.linux.org.ru/jump-message.jsp?msgid=1948661&amp;cid=1948675</a>",
        toHtmlFormatter.format(url3,false));
    String url15 = "https://www.linux.org.ru/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-6944831";
    assertEquals("<a href=\"http://www.linux.org.ru/forum/linux-org-ru/6944260?cid=6944831\" title=\"привет15\">www.linux.org.ru/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-...</a>",
        toHtmlFormatter.format(url15, false));
    String urlHistory = "http://www.linux.org.ru/news/kernel/6992532/history";
    assertEquals("<a href=\"https://www.linux.org.ru/news/kernel/6992532/history\">www.linux.org.ru/news/kernel/6992532/history</a>",
        toHtmlFormatter.format(urlHistory, true));
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

  @Test
  public void testToLorCodeFormatter2() {
    String[] text = {
        ">one\n",
        ">one\n>one\n",
        ">>one\n>teo\n",
        "due>>one\n>teo\n>>neo\nwuf?\nok",
        "due\n>>one\n>teo\n>>neo\nwuf?\nok",
        ">one\n\n\n\n>one",
    };
    String[] bb_tex = {
        "[quote]one[/quote]",
        "[quote]one[br]one[/quote]",
        "[quote][quote]one[br][/quote]teo[/quote]",
        "due>>one\n[quote]teo[br][quote]neo[br][/quote][/quote]wuf?\nok",
        "due\n[quote][quote]one[br][/quote]teo[br][quote]neo[br][/quote][/quote]wuf?\nok",
        "[quote]one[br][/quote]\n\n[quote]one[/quote]",
    };
    String[] bb = {
        "[quote]one[/quote]",
        "[quote]one[br]one[/quote]",
        "[quote][quote]one[br][/quote]teo[/quote]",
        "due>>one[br][quote]teo[br][quote]neo[br][/quote][/quote]wuf?[br]ok",
        "due[br][quote][quote]one[br][/quote]teo[br][quote]neo[br][/quote][/quote]wuf?[br]ok",
        "[quote]one[br][/quote][br][br][quote]one[/quote]",
    };


    String[] html_tex = {
        citeHeader + "<p>one</p>" + citeFooter,
        citeHeader + "<p>one<br>one</p>" + citeFooter,
        citeHeader + citeHeader + "<p>one<br></p>" + citeFooter + "<p>teo</p>" + citeFooter,
        "<p>due&gt;&gt;one\n</p>" + citeHeader +"<p>teo<br></p>" + citeHeader +"<p>neo<br></p>" + citeFooter +  citeFooter + "<p>wuf?\nok</p>",
        "<p>due\n</p>" + citeHeader + citeHeader +"<p>one<br></p>" + citeFooter + "<p>teo<br></p>" + citeHeader + "<p>neo<br></p>" + citeFooter + citeFooter + "<p>wuf?\nok</p>",
        citeHeader + "<p>one<br></p>" + citeFooter + citeHeader + "<p>one</p>" + citeFooter,
    };

    String[] html = {
        citeHeader + "<p>one</p>" + citeFooter,
        citeHeader + "<p>one<br>one</p>" + citeFooter,
        citeHeader + citeHeader + "<p>one<br></p>" + citeFooter +"<p>teo</p>" + citeFooter,
        "<p>due&gt;&gt;one<br></p>" + citeHeader + "<p>teo<br></p>" + citeHeader + "<p>neo<br></p>" + citeFooter + citeFooter + "<p>wuf?<br>ok</p>",
        "<p>due<br></p>" + citeHeader + citeHeader + "<p>one<br></p>"+ citeFooter + "<p>teo<br></p>" + citeHeader + "<p>neo<br></p>" + citeFooter + citeFooter + "<p>wuf?<br>ok</p>",
        citeHeader + "<p>one<br></p>" + citeFooter + "<p><br><br></p>" + citeHeader + "<p>one</p>" + citeFooter,
    };

    for(int i = 0; i<text.length; i++){
      String entry = text[i];
      assertEquals(bb_tex[i], toLorCodeTexFormatter.format(entry));
      assertEquals(html_tex[i], lorCodeService.parseComment(toLorCodeTexFormatter.format(entry), false));

      assertEquals(bb[i], toLorCodeFormatter.format(entry, true));
      assertEquals(html[i], lorCodeService.parseComment(toLorCodeFormatter.format(entry, true), false));
    }
  }

  @Test
  public void inCodeEscape() {
    assertEquals(
        "<div class=\"code\"><pre class=\"no-highlight\"><code>&amp;#9618;</code></pre></div>",
        lorCodeService.parseTopic("[code]&#9618;[/code]", false)
    );
    assertEquals(
        "<p>&#9618;</p>",
        lorCodeService.parseTopic("&#9618;", false)
    );
  }

  
  @Test
  public void listTest2() {
    String a = "[list]\n[*]one\n[*]two\n[*]three\n[/list]";
    assertEquals(
        "[list][br][*]one[br][*]two[br][*]three[br][/list]",
        toLorCodeFormatter.format(a, true)
    );
    assertEquals(
        "[list][br][*]one[br][*]two[br][*]three[br][/list]",
        toLorCodeFormatter.format(a, false)
    );
    assertEquals(
        "[list]\n[*]one\n[*]two\n[*]three\n[/list]",
        toLorCodeTexFormatter.format(a)
    );

    // toLorCodeFormatter.format(a, true)
    String b = toLorCodeFormatter.format(a, true);
    assertEquals(
        "<p><br></p><ul><li>one<br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseComment(b, false)
    );
    assertEquals(
        "<p><br></p><ul><li>one<br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseComment(b, true)
    );
    assertEquals(
        "<p><br></p><ul><li>one<br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseTopic(b, false)
    );
    assertEquals(
        "<p><br></p><ul><li>one<br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseTopic(b, true)
    );

    // toLorCodeFormatter.format(a, false)
    b = toLorCodeFormatter.format(a, false);
    assertEquals(
        "<p><br></p><ul><li>one<br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseComment(b, false)
    );
    assertEquals(
        "<p><br></p><ul><li>one<br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseComment(b, true)
    );
    assertEquals(
        "<p><br></p><ul><li>one<br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseTopic(b, false)
    );
    assertEquals(
        "<p><br></p><ul><li>one<br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseTopic(b, true)
    );

    // toLorCodeTexFormatter.format(a, true)
    b = toLorCodeTexFormatter.format(a);
    assertEquals(
        "<ul><li>one\n</li><li>two\n</li><li>three\n</li></ul>",
        lorCodeService.parseComment(b, false)
    );
    assertEquals(
        "<ul><li>one\n</li><li>two\n</li><li>three\n</li></ul>",
        lorCodeService.parseComment(b, true)
    );
    assertEquals(
        "<ul><li>one\n</li><li>two\n</li><li>three\n</li></ul>",
        lorCodeService.parseTopic(b, false)
    );
    assertEquals(
        "<ul><li>one\n</li><li>two\n</li><li>three\n</li></ul>",
        lorCodeService.parseTopic(b, true)
    );

    // toLorCodeTexFormatter.format(a, false)
    b = toLorCodeTexFormatter.format(a);
    assertEquals(
        "<ul><li>one\n</li><li>two\n</li><li>three\n</li></ul>",
        lorCodeService.parseComment(b, false)
    );
    assertEquals(
        "<ul><li>one\n</li><li>two\n</li><li>three\n</li></ul>",
        lorCodeService.parseComment(b, true)
    );
    assertEquals(
        "<ul><li>one\n</li><li>two\n</li><li>three\n</li></ul>",
        lorCodeService.parseTopic(b, false)
    );
    assertEquals(
        "<ul><li>one\n</li><li>two\n</li><li>three\n</li></ul>",
        lorCodeService.parseTopic(b, true)
    );

  }

  @Test
  public void listTest3() {
    String a = "[list]\n[*]one\n\n[*]two\n[*]three\n[/list]";
    assertEquals(
        "[list][br][*]one[br][br][*]two[br][*]three[br][/list]",
        toLorCodeFormatter.format(a, true)
    );
    assertEquals(
        "[list][br][*]one[br][br][*]two[br][*]three[br][/list]",
        toLorCodeFormatter.format(a, false)
    );
    assertEquals(
        "[list]\n[*]one\n\n[*]two\n[*]three\n[/list]",
        toLorCodeTexFormatter.format(a)
    );

    // toLorCodeFormatter.format(a, true)
    String b = toLorCodeFormatter.format(a, true);
    assertEquals(
        "<p><br></p><ul><li>one<br><br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseComment(b, false)
    );
    assertEquals(
        "<p><br></p><ul><li>one<br><br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseComment(b, true)
    );
    assertEquals(
        "<p><br></p><ul><li>one<br><br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseTopic(b, false)
    );
    assertEquals(
        "<p><br></p><ul><li>one<br><br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseTopic(b, true)
    );

    // toLorCodeFormatter.format(a, false)
    b = toLorCodeFormatter.format(a, false);
    assertEquals(
        "<p><br></p><ul><li>one<br><br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseComment(b, false)
    );
    assertEquals(
        "<p><br></p><ul><li>one<br><br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseComment(b, true)
    );
    assertEquals(
        "<p><br></p><ul><li>one<br><br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseTopic(b, false)
    );
    assertEquals(
        "<p><br></p><ul><li>one<br><br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseTopic(b, true)
    );

    // toLorCodeTexFormatter.format(a, true)
    b = toLorCodeTexFormatter.format(a);
    assertEquals(
        "<ul><li>one</li><li>two\n</li><li>three\n</li></ul>",
        lorCodeService.parseComment(b, false)
    );
    assertEquals(
        "<ul><li>one</li><li>two\n</li><li>three\n</li></ul>",
        lorCodeService.parseComment(b, true)
    );
    assertEquals(
        "<ul><li>one</li><li>two\n</li><li>three\n</li></ul>",
        lorCodeService.parseTopic(b, false)
    );
    assertEquals(
        "<ul><li>one</li><li>two\n</li><li>three\n</li></ul>",
        lorCodeService.parseTopic(b, true)
    );
  }

  @Test
  public void listTest4() {
    String a = "[list]\n[*]one\n\ncrap\n[*]two\n[*]three\n[/list]";
    assertEquals(
        "[list][br][*]one[br][br]crap[br][*]two[br][*]three[br][/list]",
        toLorCodeFormatter.format(a, true)
    );
    assertEquals(
        "[list][br][*]one[br][br]crap[br][*]two[br][*]three[br][/list]",
        toLorCodeFormatter.format(a, false)
    );
    assertEquals(
        "[list]\n[*]one\n\ncrap\n[*]two\n[*]three\n[/list]",
        toLorCodeTexFormatter.format(a)
    );

    // toLorCodeFormatter.format(a, true)
    String b = toLorCodeFormatter.format(a, true);
    assertEquals(
        "<p><br></p><ul><li>one<br><br>crap<br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseComment(b, false)
    );
    assertEquals(
        "<p><br></p><ul><li>one<br><br>crap<br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseComment(b, true)
    );
    assertEquals(
        "<p><br></p><ul><li>one<br><br>crap<br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseTopic(b, false)
    );
    assertEquals(
        "<p><br></p><ul><li>one<br><br>crap<br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseTopic(b, true)
    );

    // toLorCodeFormatter.format(a, false)
    b = toLorCodeFormatter.format(a, false);
    assertEquals(
        "<p><br></p><ul><li>one<br><br>crap<br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseComment(b, false)
    );
    assertEquals(
        "<p><br></p><ul><li>one<br><br>crap<br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseComment(b, true)
    );
    assertEquals(
        "<p><br></p><ul><li>one<br><br>crap<br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseTopic(b, false)
    );
    assertEquals(
        "<p><br></p><ul><li>one<br><br>crap<br></li><li>two<br></li><li>three<br></li></ul>",
        lorCodeService.parseTopic(b, true)
    );

    // toLorCodeTexFormatter.format(a, true)
    b = toLorCodeTexFormatter.format(a);
    assertEquals(
        "<ul><li>one<p>crap\n</p></li><li>two\n</li><li>three\n</li></ul>",
        lorCodeService.parseComment(b, false)
    );
    assertEquals(
        "<ul><li>one<p>crap\n</p></li><li>two\n</li><li>three\n</li></ul>",
        lorCodeService.parseComment(b, true)
    );
    assertEquals(
        "<ul><li>one<p>crap\n</p></li><li>two\n</li><li>three\n</li></ul>",
        lorCodeService.parseTopic(b, false)
    );
    assertEquals(
        "<ul><li>one<p>crap\n</p></li><li>two\n</li><li>three\n</li></ul>",
        lorCodeService.parseTopic(b, true)
    );
  }

  @Test
  public void testOg() {
    assertEquals(
        "hello",
        lorCodeService.parseForOgDescription("hello")
    );

    assertEquals(
        "one crap two three",
        lorCodeService.parseForOgDescription("[list]\n" +
            "[*]one\n" +
            "\n" +
            "crap\n" +
            "[*]two\n" +
            "[*]three\n" +
            "[/list]")
    );
    assertEquals(
        "due one teo neo wuf?\nok",
        lorCodeService.parseForOgDescription("due\n[quote][quote]one[br][/quote]teo[br][quote]neo[br][/quote][/quote]wuf?\nok")
    );
    assertEquals(
        "",
        lorCodeService.parseForOgDescription("[code]&#9618;[/code]")
    );
    String txt = "many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]";
    assertEquals(
        "many many  texxt many many  texxt many many  texxt many many  texxt many many  texxt many many  texxt many many  texxt many many  texxt many many  texxt many many  texxt many many  texxt many many  texxt many many  texxt many many  texxt many many  t...",
        lorCodeService.parseForOgDescription(txt)
    );
    assertEquals(
        250+3,
        lorCodeService.parseForOgDescription(txt).length()
    );
  }

}
