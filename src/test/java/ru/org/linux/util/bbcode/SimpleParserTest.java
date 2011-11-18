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

package ru.org.linux.util.bbcode;

import junit.framework.Assert;
import org.apache.commons.httpclient.URI;
import org.junit.Before;
import org.junit.Test;
import ru.org.linux.site.User;
import ru.org.linux.site.UserNotFoundException;
import ru.org.linux.spring.Configuration;
import ru.org.linux.spring.dao.UserDao;
import ru.org.linux.util.formatter.ToHtmlFormatter;

import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SimpleParserTest {

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
    lorCodeService.userDao = userDao;
    lorCodeService.configuration = configuration;
    lorCodeService.toHtmlFormatter = toHtmlFormatter;

    url = "http://127.0.0.1:8080/forum/talks/22464";
  }

  @Test
  public void brTest() {
    Assert.assertEquals(lorCodeService.parseComment("[br]", false), "<p><br></p>");
  }

  @Test
  public void boldTest() {
    Assert.assertEquals(lorCodeService.parseComment("[b]hello world[/b]", false), "<p><b>hello world</b></p>");
  }

  @Test
  public void italicTest() {
    Assert.assertEquals(lorCodeService.parseComment("[i]hello world[/i]", false), "<p><i>hello world</i></p>");
  }

  @Test
  public void strikeoutTest() {
    Assert.assertEquals(lorCodeService.parseComment("[s]hello world[/s]", false), "<p><s>hello world</s></p>");
  }

  @Test
  public void emphasisTest() {
    Assert.assertEquals(lorCodeService.parseComment("[strong]hello world[/strong]", false), "<p><strong>hello world</strong></p>");
  }

  @Test
  public void quoteTest() {
    // TODO я нрипонял зачем <div> :-(
    Assert.assertEquals(lorCodeService.parseComment("[quote]hello world[/quote]", false), "<div class=\"quote\"><h3>Цитата</h3><p>hello world</p></div>");
  }

  @Test
  public void quoteParamTest() {
    // TODO я нрипонял зачем <div> :-(
    Assert.assertEquals(lorCodeService.parseComment("[quote=maxcom]hello world[/quote]", false), "<div class=\"quote\"><h3>maxcom</h3><p>hello world</p></div>");
  }

  @Test
  public void quoteCleanTest() {
    Assert.assertEquals(lorCodeService.parseComment("[quote][/quote]", false), "");
  }


  @Test
  public void urlTest() {
    Assert.assertEquals(lorCodeService.parseComment("[url]http://linux.org.ru[/url]", false), "<p><a href=\"http://linux.org.ru\">http://linux.org.ru</a></p>");
  }

  @Test
  public void urlParamTest() {
    Assert.assertEquals(lorCodeService.parseComment("[url=http://linux.org.ru]linux[/url]", false), "<p><a href=\"http://linux.org.ru\">linux</a></p>");
  }

  @Test
  public void urlParamWithTagTest() {
    Assert.assertEquals(lorCodeService.parseComment("[url=http://linux.org.ru][b]l[/b]inux[/url]", false), "<p><a href=\"http://linux.org.ru\"><b>l</b>inux</a></p>");
  }

  @Test
  public void urlParamWithTagTest2() {
    Assert.assertEquals(lorCodeService.parseComment("[url=http://linux.org.ru][b]linux[/b][/url]", false), "<p><a href=\"http://linux.org.ru\"><b>linux</b></a></p>");
  }

  @Test
  public void listTest() {
    Assert.assertEquals(lorCodeService.parseComment("[list][*]one[*]two[*]three[/list]", false), "<ul><li>one</li><li>two</li><li>three</li></ul>");
  }

  @Test
  public void codeTest() {
    Assert.assertEquals(lorCodeService.parseComment("[code][list][*]one[*]two[*]three[/list][/code]", false), "<div class=\"code\"><pre class=\"no-highlight\"><code>[list][*]one[*]two[*]three[/list]</code></pre></div>");
  }

  @Test
  public void codeCleanTest() {
    Assert.assertEquals("", lorCodeService.parseComment("[code][/code]", false));
  }

  @Test
  public void codeKnowTest() {
    Assert.assertEquals("<div class=\"code\"><pre class=\"language-cpp\"><code>#include &lt;stdio.h&gt;</code></pre></div>",
            lorCodeService.parseComment("[code=cxx]#include <stdio.h>[/code]", false));
  }

  @Test
  public void codeUnKnowTest() {
    Assert.assertEquals("<div class=\"code\"><pre class=\"no-highlight\"><code>#include &lt;stdio.h&gt;</code></pre></div>",
            lorCodeService.parseComment("[code=foo]#include <stdio.h>[/code]", false));
  }

  @Test
  public void overflow1Test() {
    Assert.assertEquals("<p>ololo</p><div class=\"quote\"><h3>Цитата</h3><p><i>hz</i></p></div>",
            lorCodeService.parseComment("ololo[quote][i]hz[/i][/quote]", false));
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
            "Как привыкли уж давно![/pre]"
        , false));

  }

  @Test
  public void spacesTest() {
    Assert.assertEquals("<p>some text</p><p> some again text <a href=\"http://example.com\">example</a> example</p>",
        lorCodeService.parseComment("some text\n\n some again text [url=http://example.com]example[/url] example", false));
  }

  @Test
  public void userTest() throws Exception{


    Assert.assertEquals("<p><span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><a style=\"text-decoration: none\" href=\"http://127.0.0.1:8080/people/maxcom/profile\">maxcom</a></span></p>",
        lorCodeService.parseComment("[user]maxcom[/user]", false));
    Assert.assertEquals("<p><span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><s><a style=\"text-decoration: none\" href=\"http://127.0.0.1:8080/people/isden/profile\">isden</a></s></span></p>",
        lorCodeService.parseComment("[user]isden[/user]", false));
    Assert.assertEquals("<p><s>hizel</s></p>",
        lorCodeService.parseComment("[user]hizel[/user]", false));
  }

  @Test
  public void parserResultTest() throws Exception {
    String msg = "[user]hizel[/user][user]JB[/user][user]maxcom[/user]";
    Set<User> replier = lorCodeService.getReplierFromMessage(msg);
    String html = lorCodeService.parseComment(msg, true);

    Assert.assertTrue(replier.contains(maxcom));
    Assert.assertTrue(replier.contains(JB));
    Assert.assertFalse(replier.contains(isden));
    Assert.assertEquals("<p><s>hizel</s><span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><a style=\"text-decoration: none\" href=\"https://127.0.0.1:8080/people/JB/profile\">JB</a></span><span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><a style=\"text-decoration: none\" href=\"https://127.0.0.1:8080/people/maxcom/profile\">maxcom</a></span></p>", html);
  }

  @Test
  public void cutTest() {
    Assert.assertEquals("<p>test</p>",
        lorCodeService.parseComment("[cut]test[/cut]", false));
    Assert.assertEquals("<p>( <a href=\"http://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>",
        lorCodeService.parseTopicWithMinimizedCut("[cut]test[/cut]", url, false));
    Assert.assertEquals("<p>( <a href=\"https://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>",
        lorCodeService.parseTopicWithMinimizedCut("[cut]test[/cut]", url, true));
    Assert.assertEquals("<div id=\"cut0\"><p>test</p></div>",
        lorCodeService.parseTopic("[cut]test[/cut]", false));
  }

  @Test
  public void cut2Test() {
    Assert.assertEquals("<p>test</p>",
        lorCodeService.parseComment("[cut]\n\ntest[/cut]", false));
    Assert.assertEquals("<p>( <a href=\"http://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>",
        lorCodeService.parseTopicWithMinimizedCut("[cut]\n\ntest[/cut]", url, false));
    Assert.assertEquals("<p>( <a href=\"https://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>",
        lorCodeService.parseTopicWithMinimizedCut("[cut]\n\ntest[/cut]", url, true));
    Assert.assertEquals("<div id=\"cut0\"><p>test</p></div>",
        lorCodeService.parseTopic("[cut]\n\ntest[/cut]", false));
  }

  @Test
  public void cut3Test() {
    Assert.assertEquals("<p>some text</p><div id=\"cut0\"><ul><li>one</li><li><p>two</p></li></ul></div>",
        lorCodeService.parseTopic("some text\n\n[cut]\n\n[list][*]one\n\n[*]\n\ntwo[/cut]", false));
    Assert.assertEquals("<p>some text</p><p>( <a href=\"http://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>",
        lorCodeService.parseTopicWithMinimizedCut("some text\n\n[cut]\n\n[list][*]one\n\n[*]\n\ntwo[/cut]", url, false));
    Assert.assertEquals("<p>some text</p><p>( <a href=\"https://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>",
        lorCodeService.parseTopicWithMinimizedCut("some text\n\n[cut]\n\n[list][*]one\n\n[*]\n\ntwo[/cut]", url, true));
  }

  @Test
  public void cut4Test() {
    Assert.assertEquals("<div id=\"cut0\"><p>test</p></div><div id=\"cut1\"><p>test</p></div>",
        lorCodeService.parseTopic("[cut]\n\ntest[/cut][cut]test[/cut]", false));
  }

  @Test
  public void appleTest() {
    Assert.assertEquals("<div class=\"quote\"><h3>Цитата</h3><p> Apple ][</p></div><p> текст</p>",
        lorCodeService.parseComment("[quote] Apple ][[/quote] текст", false));
  }

  @Test
  public void urlParameterQuotesTest() {
    assertEquals("<p><a href=\"http://www.example.com\">example</a></p>",
        lorCodeService.parseComment("[url=\"http://www.example.com]example[/url]", false));
    assertEquals("<p><a href=\"http://www.example.com\">example</a></p>",
        lorCodeService.parseComment("[url=http://www.example.com\"]example[/url]", false));
    assertEquals("<p><a href=\"http://www.example.com\">example</a></p>",
        lorCodeService.parseComment("[url=\"http://www.example.com\"]example[/url]", false));
  }

  @Test
  public void cutWithParameterTest() {
    assertEquals("<p>( <a href=\"https://127.0.0.1:8080/forum/talks/22464#cut0\">нечитать!</a> )</p>",
        lorCodeService.parseTopicWithMinimizedCut("[cut=нечитать!]\n\ntest[/cut]", url, true));
  }

  @Test
  public void autoLinksInList() {
    assertEquals("<ul><li><a href=\"http://www.example.com\">http://www.example.com</a></li><li>sure</li><li>profit!</li></ul>",
        lorCodeService.parseComment("[list][*]www.example.com[*]sure[*]profit![/list]", false));
  }


}
