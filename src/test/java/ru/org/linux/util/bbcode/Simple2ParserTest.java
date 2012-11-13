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

import org.apache.commons.httpclient.URI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.org.linux.spring.Configuration;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserNotFoundException;
import ru.org.linux.util.formatter.ToHtmlFormatter;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.org.linux.util.bbcode.tags.QuoteTag.citeFooter;
import static ru.org.linux.util.bbcode.tags.QuoteTag.citeHeader;

public class Simple2ParserTest {

  LorCodeService lorCodeService;
  Configuration configuration;
  ToHtmlFormatter toHtmlFormatter;
  UserDao userDao;
  User maxcom; // Администратор
  User JB;     // Модератор
  User isden;  // Заблокированный пользователь
  String mainUrl;
  URI mainURI;
  String url;


  @Before
  public void init() throws Exception {
    maxcom = mock(User.class);
    JB = mock(User.class);
    isden = mock(User.class);

    when(maxcom.isBlocked()).thenReturn(false);
    when(JB.isBlocked()).thenReturn(false);
    when(isden.isBlocked()).thenReturn(true);
    when(maxcom.getNick()).thenReturn("maxcom");
    when(JB.getNick()).thenReturn("JB");
    when(isden.getNick()).thenReturn("isden");

    userDao = mock(UserDao.class);
    when(userDao.getUser("maxcom")).thenReturn(maxcom);
    when(userDao.getUser("JB")).thenReturn(JB);
    when(userDao.getUser("isden")).thenReturn(isden);
    when(userDao.getUser("hizel")).thenThrow(new UserNotFoundException("hizel"));

    mainUrl = "http://127.0.0.1:8080/";
    mainURI = new URI(mainUrl, true, "UTF-8");
    configuration = mock(Configuration.class);
    when(configuration.getMainURI()).thenReturn(mainURI);
    when(configuration.getMainUrl()).thenReturn(mainUrl);

    toHtmlFormatter = new ToHtmlFormatter();
    toHtmlFormatter.setConfiguration(configuration);


    lorCodeService = new LorCodeService();
    lorCodeService.setUserDao(userDao);
    lorCodeService.setConfiguration(configuration);
    lorCodeService.setToHtmlFormatter(toHtmlFormatter);

    url = "http://127.0.0.1:8080/forum/talks/22464";
  }

  @Test
  public void brTest() {
    Assert.assertEquals("<p><br></p>", lorCodeService.parseComment("[BR]", false, false));
  }

  @Test
  public void boldTest() {
    Assert.assertEquals("<p><b>hello world</b></p>", lorCodeService.parseComment("[B]hello world[/b]", false, false));
  }

  @Test
  public void italicTest() {
    Assert.assertEquals("<p><i>hello world</i></p>", lorCodeService.parseComment("[I]hello world[/i]", false, false));
  }

  @Test
  public void strikeoutTest() {
    Assert.assertEquals("<p><s>hello world</s></p>", lorCodeService.parseComment("[S]hello world[/s]", false, false));
  }

  @Test
  public void emphasisTest() {
    Assert.assertEquals("<p><strong>hello world</strong></p>", lorCodeService.parseComment("[STRONG]hello world[/strong]", false, false));
  }

  @Test
  public void quoteTest() {
    Assert.assertEquals(lorCodeService.parseComment("[quote]hello world[/QUOTE]", false, false),
        citeHeader + "<p>hello world</p>" + citeFooter);
  }

  @Test
  public void quoteParamTest() {
    Assert.assertEquals(lorCodeService.parseComment("[QUOTE=maxcom]hello world[/quote]", false, false),
        citeHeader + "<p><cite>maxcom</cite></p><p>hello world</p>"+citeFooter);
  }

  @Test
  public void quoteCleanTest() {
    Assert.assertEquals("", lorCodeService.parseComment("[quote][/QUOTE]", false, false));
  }


  @Test
  public void urlTest() {
    Assert.assertEquals("<p><a href=\"http://linux.org.ru\">http://linux.org.ru</a></p>", lorCodeService.parseComment("[URL]http://linux.org.ru[/url]", false, false));
  }

  @Test
  public void urlTestNoFollow() {
    Assert.assertEquals(
            "<p><a href=\"http://linux.org.ru\" rel=nofollow>http://linux.org.ru</a></p>",
            lorCodeService.parseComment("[URL]http://linux.org.ru[/url]", false, true)
    );
    Assert.assertEquals(
            "<p><a href=\"http://127.0.0.1:8080\">http://127.0.0.1:8080</a></p>",
            lorCodeService.parseComment("[URL]http://127.0.0.1:8080[/url]", false, true)
    );
  }

  @Test
  public void autoUrlTestNoFollow() {
    Assert.assertEquals(
            "<p><a href=\"http://linux.org.ru/\" rel=nofollow>http://linux.org.ru/</a></p>",
            lorCodeService.parseComment("http://linux.org.ru/", false, true)
    );

    Assert.assertEquals(
            "<p><a href=\"http://127.0.0.1:8080/\">127.0.0.1:8080/</a></p>",
            lorCodeService.parseComment("http://127.0.0.1:8080/", false, true)
    );
  }

  @Test
  public void urlParamTest() {
    Assert.assertEquals("<p><a href=\"http://linux.org.ru\">linux</a></p>", lorCodeService.parseComment("[url=http://linux.org.ru]linux[/URL]", false, false));
  }

  @Test
  public void urlParamWithTagTest() {
    Assert.assertEquals("<p><a href=\"http://linux.org.ru\"><b>l</b>inux</a></p>", lorCodeService.parseComment("[URL=http://linux.org.ru][b]l[/b]inux[/url]", false, false));
  }

  @Test
  public void urlParamWithTagTest2() {
    Assert.assertEquals("<p><a href=\"http://linux.org.ru\"><b>linux</b></a></p>", lorCodeService.parseComment("[URL=http://linux.org.ru][b]linux[/b][/url]", false, false));
  }

  @Test
  public void listTest() {
    Assert.assertEquals("<ul><li>one</li><li>two</li><li>three</li></ul>", lorCodeService.parseComment("[LIST][*]one[*]two[*]three[/list]", false, false));
    assertEquals(
        "<ul><li>one\n" +
            "</li><li>two\n" +
            "</li><li>three\n" +
            "</li></ul>",
        lorCodeService.parseComment("[list]\n[*]one\n[*]two\n[*]three\n[/LIST]", false, false));
    assertEquals(
        "<ul><li>one\n" +
            "</li><li>two\n" +
            "</li><li>three\n" +
            "</li></ul>",
        lorCodeService.parseTopic("[list]\n[*]one\n[*]two\n[*]three\n[/LIST]", false));
  }

  @Test
  public void codeTest() {
    Assert.assertEquals("<div class=\"code\"><pre class=\"no-highlight\"><code>[list][*]one[*]two[*]three[/LIST]</code></pre></div>",
        lorCodeService.parseComment("[CODE][list][*]one[*]two[*]three[/LIST][/code]", false, false));
    Assert.assertEquals("<div class=\"code\"><pre class=\"no-highlight\"><code>simple code</code></pre></div>",
        lorCodeService.parseComment("[code]\nsimple code[/CODE]", false, false));
    Assert.assertEquals("<div class=\"code\"><pre class=\"no-highlight\"><code>[LIST][*]one[*]two[*]three[/list]</code></pre></div>",
        lorCodeService.parseComment("[code]\n[LIST][*]one[*]two[*]three[/list][/CODE]", false, false));
  }

  @Test
  public void codeCleanTest() {
    Assert.assertEquals("", lorCodeService.parseComment("[code][/CODE]", false, false));
  }

  @Test
  public void codeKnowTest() {
    Assert.assertEquals("<div class=\"code\"><pre class=\"language-cpp\"><code>#include &lt;stdio.h&gt;</code></pre></div>",
            lorCodeService.parseComment("[CODE=cxx]#include <stdio.h>[/code]", false, false));
  }

  @Test
  public void codeUnKnowTest() {
    Assert.assertEquals("<div class=\"code\"><pre class=\"no-highlight\"><code>#include &lt;stdio.h&gt;</code></pre></div>",
            lorCodeService.parseComment("[CODE=foo]#include <stdio.h>[/code]", false, false));
  }

  @Test
  public void overflow1Test() {
    Assert.assertEquals("<p>ololo</p>" + citeHeader +"<p><i>hz</i></p>" + citeFooter,
            lorCodeService.parseComment("ololo[QUOTE][i]hz[/I][/quote]", false, false));
  }

  @Test
  public void preTest() {
    Assert.assertEquals(
        "<pre>Тег используем мы этот,\n" +
            "Чтобы строки разделять,\n" +
            "Если вдруг стихи захочем\n" +
            "Здесь, на ЛОРе, запощать.\n\n" +
            "Ну а строфы разделяем\n" +
            "Как привыкли уж давно!</pre>"
        , lorCodeService.parseComment(
        "[pre]Тег используем мы этот,\n" +
            "Чтобы строки разделять,\n" +
            "Если вдруг стихи захочем\n" +
            "Здесь, на ЛОРе, запощать.\n\n" +
            "Ну а строфы разделяем\n" +
            "Как привыкли уж давно![/PRE]"
        , false, false));

  }

  @Test
  public void spacesTest() {
    Assert.assertEquals("<p>some text</p><p> some again text <a href=\"http://example.com\">example</a> example</p>",
        lorCodeService.parseComment("some text\n\n some again text [URL=http://example.com]example[/url] example", false, false));
  }

  @Test
  public void userTest() {


    Assert.assertEquals("<p><span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><a style=\"text-decoration: none\" href=\"http://127.0.0.1:8080/people/maxcom/profile\">maxcom</a></span></p>",
        lorCodeService.parseComment("[user]maxcom[/USER]", false, false));
    Assert.assertEquals("<p><span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><s><a style=\"text-decoration: none\" href=\"http://127.0.0.1:8080/people/isden/profile\">isden</a></s></span></p>",
        lorCodeService.parseComment("[USER]isden[/USER]", false, false));
    Assert.assertEquals("<p><s>hizel</s></p>",
        lorCodeService.parseComment("[user]hizel[/USER]", false, false));
  }

  @Test
  public void parserResultTest() {
    String msg = "[user]hizel[/user][USER]JB[/user][user]maxcom[/USER]";
    Set<User> replier = lorCodeService.getReplierFromMessage(msg);
    String html = lorCodeService.parseComment(msg, true, false);

    Assert.assertTrue(replier.contains(maxcom));
    Assert.assertTrue(replier.contains(JB));
    Assert.assertFalse(replier.contains(isden));
    Assert.assertEquals("<p><s>hizel</s><span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><a style=\"text-decoration: none\" href=\"https://127.0.0.1:8080/people/JB/profile\">JB</a></span><span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><a style=\"text-decoration: none\" href=\"https://127.0.0.1:8080/people/maxcom/profile\">maxcom</a></span></p>", html);
  }

  @Test
  public void cutTest() {
    Assert.assertEquals("<p>test</p>",
        lorCodeService.parseComment("[cut]test[/CUT]", false, false));
    Assert.assertEquals("<p>( <a href=\"http://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>",
        lorCodeService.parseTopicWithMinimizedCut("[CUT]test[/cut]", url, false));
    Assert.assertEquals("<p>( <a href=\"https://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>",
        lorCodeService.parseTopicWithMinimizedCut("[CUT]test[/CUT]", url, true));
    Assert.assertEquals("<div id=\"cut0\"><p>test</p></div>",
        lorCodeService.parseTopic("[CUT]test[/cut]", false));
  }

  @Test
  public void cut2Test() {
    Assert.assertEquals("<p>test</p>",
        lorCodeService.parseComment("[CUT]\n\ntest[/cut]", false, false));
    Assert.assertEquals("<p>( <a href=\"http://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>",
        lorCodeService.parseTopicWithMinimizedCut("[cut]\n\ntest[/CUT]", url, false));
    Assert.assertEquals("<p>( <a href=\"https://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>",
        lorCodeService.parseTopicWithMinimizedCut("[cut]\n\ntest[/CUT]", url, true));
    Assert.assertEquals("<div id=\"cut0\"><p>test</p></div>",
        lorCodeService.parseTopic("[cut]\n\ntest[/CUT]", false));
  }

  @Test
  public void cut3Test() {
    Assert.assertEquals("<p>some text</p><div id=\"cut0\"><ul><li>one</li><li><p>two</p></li></ul></div>",
        lorCodeService.parseTopic("some text\n\n[CUT]\n\n[list][*]one\n\n[*]\n\ntwo[/cut]", false));
    Assert.assertEquals("<p>some text</p><p>( <a href=\"http://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>",
        lorCodeService.parseTopicWithMinimizedCut("some text\n\n[cut]\n\n[LIST][*]one\n\n[*]\n\ntwo[/CUT]", url, false));
    Assert.assertEquals("<p>some text</p><p>( <a href=\"https://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>",
        lorCodeService.parseTopicWithMinimizedCut("some text\n\n[cut]\n\n[LIST][*]one\n\n[*]\n\ntwo[/CUT]", url, true));
  }

  @Test
  public void cut4Test() {
    Assert.assertEquals("<div id=\"cut0\"><p>test</p></div><div id=\"cut1\"><p>test</p></div>",
        lorCodeService.parseTopic("[cut]\n\ntest[/CUT][cut]test[/cut]", false));
  }

  @Test
  public void appleTest() {
    Assert.assertEquals(citeHeader + "<p> Apple ][</p>" + citeFooter + "<p> текст</p>",
        lorCodeService.parseComment("[quote] Apple ][[/QUOTE] текст", false, false));
  }

  @Test
  public void urlParameterQuotesTest() {
    assertEquals("<p><a href=\"http://www.example.com\">example</a></p>",
        lorCodeService.parseComment("[url=\"http://www.example.com]example[/URL]", false, false));
    assertEquals("<p><a href=\"http://www.example.com\">example</a></p>",
        lorCodeService.parseComment("[url=\"http://www.example.com\"]example[/url]", false, false));
    assertEquals("<p><a href=\"http://www.example.com\">example</a></p>",
        lorCodeService.parseComment("[URL='http://www.example.com']example[/url]", false, false));
    assertEquals("<p><a href=\"http://www.example.com\">example</a></p>",
        lorCodeService.parseComment("[URL='http://www.example.com]example[/url]", false, false));
  }

  @Test
  public void cutWithParameterTest() {
    assertEquals("<p>( <a href=\"https://127.0.0.1:8080/forum/talks/22464#cut0\">нечитать!</a> )</p>",
        lorCodeService.parseTopicWithMinimizedCut("[CUT=нечитать!]\n\ntest[/cut]", url, true));
  }

  @Test
  public void autoLinksInList() {
    assertEquals("<ul><li><a href=\"http://www.example.com\">http://www.example.com</a></li><li>sure</li><li>profit!</li></ul>",
        lorCodeService.parseComment("[list][*]www.example.com[*]sure[*]profit![/LIST]", false, false));
  }

  @Test
  public void quoteQuoteQuote() {
    assertEquals(citeHeader + "<p>прювет!</p>" + citeFooter,
        lorCodeService.parseComment("[quote][quote][quote][QUOTE][quote][quote][quote][quote][quote][quote][quote][quote][quote][QUOTE][quote][quote][quote][quote]прювет![/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/QUOTE]", false, false));
  }

  @Test
  public void testMDash() {
    assertEquals("test&nbsp;&mdash; test",
        toHtmlFormatter.format("test -- test", false, false));
    assertEquals("<p>test&nbsp;&mdash; test</p>",
        lorCodeService.parseComment("test -- test", false, false));
    assertEquals("<p>test<div class=\"code\"><pre class=\"no-highlight\"><code> -- </code></pre></div>test</p>",
        lorCodeService.parseComment("test[code] -- [/code]test", false, false));
    assertEquals("<p><a href=\"http://www.linux.org.ru/\">http://www.linux.org.ru/</a>&nbsp;&mdash; русская информация об ос линукс</p>",
        lorCodeService.parseComment("http://www.linux.org.ru/ -- русская информация об ос линукс", false, false));
  }

}