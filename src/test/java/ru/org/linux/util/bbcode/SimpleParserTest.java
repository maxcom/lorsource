/*
 * Copyright 1998-2024 Linux.org.ru
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
import org.junit.Before;
import org.junit.Test;
import ru.org.linux.spring.SiteConfig;
import ru.org.linux.user.UserService;
import ru.org.linux.util.formatter.ToHtmlFormatter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.org.linux.util.bbcode.tags.QuoteTag.citeFooter;
import static ru.org.linux.util.bbcode.tags.QuoteTag.citeHeader;

public class SimpleParserTest {
  private LorCodeService lorCodeService;
  private String url;

  @Before
  public void init() throws Exception {
    String mainUrl = "http://127.0.0.1:8080/";
    URI mainURI = new URI(mainUrl, true, "UTF-8");
    SiteConfig siteConfig = mock(SiteConfig.class);
    when(siteConfig.getMainURI()).thenReturn(mainURI);

    ToHtmlFormatter toHtmlFormatter = new ToHtmlFormatter();
    toHtmlFormatter.setSiteConfig(siteConfig);

    lorCodeService = new LorCodeService(mock(UserService.class), toHtmlFormatter);

    url = "http://127.0.0.1:8080/forum/talks/22464";
  }

  @Test
  public void brTest() {
    assertEquals("<p><br></p>", lorCodeService.parseComment("[br]", false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void boldTest() {
    assertEquals("<p><b>hello world</b></p>", lorCodeService.parseComment("[b]hello world[/b]", false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void italicTest() {
    assertEquals("<p><i>hello world</i></p>", lorCodeService.parseComment("[i]hello world[/i]", false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void strikeoutTest() {
    assertEquals("<p><s>hello world</s></p>", lorCodeService.parseComment("[s]hello world[/s]", false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void emphasisTest() {
    assertEquals("<p><strong>hello world</strong></p>", lorCodeService.parseComment("[strong]hello world[/strong]", false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void quoteTest() {
    assertEquals(citeHeader + "<p>hello world</p>" + citeFooter,
            lorCodeService.parseComment("[quote]hello world[/quote]", false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void quoteTextTest() {
    assertEquals("«hello world»",
            lorCodeService.extractPlain("[quote]hello world[/quote]", LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void quoteParamTest() {
    assertEquals(citeHeader + "<p><cite>maxcom</cite></p><p>hello world</p>"+citeFooter,
            lorCodeService.parseComment("[quote=maxcom]hello world[/quote]", false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void quoteCleanTest() {
    assertEquals("", lorCodeService.parseComment("[quote][/quote]", false, LorCodeService.Plain$.MODULE$));
  }


  @Test
  public void urlTest() {
    assertEquals("<p><a href=\"http://linux.org.ru\">http://linux.org.ru</a></p>",
            lorCodeService.parseComment("[url]http://linux.org.ru[/url]", false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void paragraphSpacesTest() {
    assertEquals("<p><a href=\"http://linux.org.ru\">http://linux.org.ru</a> <a href=\"http://linux.org.ru\">http://linux.org.ru</a></p>",
            lorCodeService.parseComment("[url]http://linux.org.ru[/url] [url]http://linux.org.ru[/url]", false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void urlParamTest() {
    assertEquals("<p><a href=\"http://linux.org.ru\">linux</a></p>", lorCodeService.parseComment("[url=http://linux.org.ru]linux[/url]", false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void urlParamWithTagTest() {
    assertEquals("<p><a href=\"http://linux.org.ru\"><b>l</b>inux</a></p>", lorCodeService.parseComment("[url=http://linux.org.ru][b]l[/b]inux[/url]", false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void urlParamWithTagTest2() {
    assertEquals("<p><a href=\"http://linux.org.ru\"><b>linux</b></a></p>", lorCodeService.parseComment("[url=http://linux.org.ru][b]linux[/b][/url]", false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void listTest() {
    assertEquals("<ul><li>one</li><li>two</li><li>three</li></ul>", lorCodeService.parseComment("[list][*]one[*]two[*]three[/list]", false, LorCodeService.Plain$.MODULE$));
    assertEquals(
            """
                    <ul><li>one
                    </li><li>two
                    </li><li>three
                    </li></ul>""",
        lorCodeService.parseComment("[list]\n[*]one\n[*]two\n[*]three\n[/list]", false, LorCodeService.Plain$.MODULE$));
    assertEquals(
            """
                    <ul><li>one
                    </li><li>two
                    </li><li>three
                    </li></ul>""",
        lorCodeService.parseTopic("[list]\n[*]one\n[*]two\n[*]three\n[/list]", false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void overflow1Test() {
    assertEquals("<p>ololo</p>" + citeHeader +"<p><i>hz</i></p>" + citeFooter,
            lorCodeService.parseComment("ololo[quote][i]hz[/i][/quote]", false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void preTest() {
    assertEquals(
            """
                    <pre>Тег используем мы этот,
                    Чтобы строки разделять,
                    Если вдруг стихи захочем
                    Здесь, на ЛОРе, запощать.
                    
                    Ну а строфы разделяем
                    Как привыкли уж давно!</pre>"""
        , lorCodeService.parseComment(
                    """
                            [pre]Тег используем мы этот,
                            Чтобы строки разделять,
                            Если вдруг стихи захочем
                            Здесь, на ЛОРе, запощать.
                            
                            Ну а строфы разделяем
                            Как привыкли уж давно![/pre]"""
        , false, LorCodeService.Plain$.MODULE$));

  }

  @Test
  public void spacesTest() {
    assertEquals("<p>some text</p><p> some again text <a href=\"http://example.com\">example</a> example</p>",
        lorCodeService.parseComment("some text\n\n some again text [url=http://example.com]example[/url] example", false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void cutTest() {
    assertEquals("<p>test</p>",
        lorCodeService.parseComment("[cut]test[/cut]", false, LorCodeService.Plain$.MODULE$));
    assertEquals("<p>( <a href=\"http://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>",
        lorCodeService.parseTopicWithMinimizedCut("[cut]test[/cut]", url, false, LorCodeService.Plain$.MODULE$));
    assertEquals("<p>( <a href=\"http://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>",
        lorCodeService.parseTopicWithMinimizedCut("[cut]test[/cut]", url, false, LorCodeService.Plain$.MODULE$));
    assertEquals("<div id=\"cut0\"><p>test</p></div>",
        lorCodeService.parseTopic("[cut]test[/cut]", false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void cut2Test() {
    assertEquals("<p>test</p>",
        lorCodeService.parseComment("[cut]\n\ntest[/cut]", false, LorCodeService.Plain$.MODULE$));
    assertEquals("<p>( <a href=\"http://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>",
        lorCodeService.parseTopicWithMinimizedCut("[cut]\n\ntest[/cut]", url, false, LorCodeService.Plain$.MODULE$));
    assertEquals("<p>( <a href=\"http://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>",
        lorCodeService.parseTopicWithMinimizedCut("[cut]\n\ntest[/cut]", url, false, LorCodeService.Plain$.MODULE$));
    assertEquals("<div id=\"cut0\"><p>test</p></div>",
        lorCodeService.parseTopic("[cut]\n\ntest[/cut]", false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void cut3Test() {
    assertEquals("<p>some text</p><div id=\"cut0\"><ul><li>one</li><li><p>two</p></li></ul></div>",
        lorCodeService.parseTopic("some text\n\n[cut]\n\n[list][*]one\n\n[*]\n\ntwo[/cut]", false, LorCodeService.Plain$.MODULE$));
    assertEquals("<p>some text</p><p>( <a href=\"http://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>",
        lorCodeService.parseTopicWithMinimizedCut("some text\n\n[cut]\n\n[list][*]one\n\n[*]\n\ntwo[/cut]", url, false, LorCodeService.Plain$.MODULE$));
    assertEquals("<p>some text</p><p>( <a href=\"http://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>",
        lorCodeService.parseTopicWithMinimizedCut("some text\n\n[cut]\n\n[list][*]one\n\n[*]\n\ntwo[/cut]", url, false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void cut4Test() {
    assertEquals("<div id=\"cut0\"><p>test</p></div><div id=\"cut1\"><p>test</p></div>",
        lorCodeService.parseTopic("[cut]\n\ntest[/cut][cut]test[/cut]", false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void appleTest() {
    assertEquals(citeHeader + "<p> Apple ][</p>" + citeFooter + "<p> текст</p>",
        lorCodeService.parseComment("[quote] Apple ][[/quote] текст", false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void urlParameterQuotesTest() {
    assertEquals("<p><a href=\"http://www.example.com\">example</a></p>",
        lorCodeService.parseComment("[url=\"http://www.example.com]example[/url]", false, LorCodeService.Plain$.MODULE$));
    assertEquals("<p><a href=\"http://www.example.com\">example</a></p>",
        lorCodeService.parseComment("[url=\"http://www.example.com\"]example[/url]", false, LorCodeService.Plain$.MODULE$));
    assertEquals("<p><a href=\"http://www.example.com\">example</a></p>",
        lorCodeService.parseComment("[url='http://www.example.com']example[/url]", false, LorCodeService.Plain$.MODULE$));
    assertEquals("<p><a href=\"http://www.example.com\">example</a></p>",
        lorCodeService.parseComment("[url='http://www.example.com]example[/url]", false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void cutWithParameterTest() {
    assertEquals("<p>( <a href=\"http://127.0.0.1:8080/forum/talks/22464#cut0\">нечитать!</a> )</p>",
        lorCodeService.parseTopicWithMinimizedCut("[cut=нечитать!]\n\ntest[/cut]", url, false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void autoLinksInList() {
    assertEquals("<ul><li><a href=\"http://www.example.com\">http://www.example.com</a></li><li>sure</li><li>profit!</li></ul>",
        lorCodeService.parseComment("[list][*]www.example.com[*]sure[*]profit![/list]", false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void quoteQuoteQuote() {
    assertEquals(citeHeader + "<p>прювет!</p>" + citeFooter,
        lorCodeService.parseComment("[quote][quote][quote][quote][quote][quote][quote][quote][quote][quote][quote][quote][quote][quote][quote][quote][quote][quote]прювет![/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote]", false, LorCodeService.Plain$.MODULE$));
  }

  @Test
  public void escapeDoubleBrackets() {
    assertEquals("<p>[[doNotTag]]</p>",
        lorCodeService.parseComment("[[doNotTag]]", false, LorCodeService.Plain$.MODULE$));
    assertEquals("<p>[[/doNotTag]]</p>",
        lorCodeService.parseComment("[[/doNotTag]]", false, LorCodeService.Plain$.MODULE$));
    assertEquals("<p>[b]</p>",
        lorCodeService.parseComment("[[b]]", false, LorCodeService.Plain$.MODULE$));
    assertEquals("<p>[/b]</p>",
        lorCodeService.parseComment("[[/b]]", false, LorCodeService.Plain$.MODULE$));


    assertEquals("<div class=\"code\"><pre class=\"no-highlight\"><code>[[doNotTag]]</code></pre></div>",
        lorCodeService.parseComment("[code][[doNotTag]][/code]", false, LorCodeService.Plain$.MODULE$));
    assertEquals("<div class=\"code\"><pre class=\"no-highlight\"><code>[[/doNotTag]]</code></pre></div>",
        lorCodeService.parseComment("[code][[/doNotTag]][/code]", false, LorCodeService.Plain$.MODULE$));
    assertEquals("<div class=\"code\"><pre class=\"no-highlight\"><code>[[b]]</code></pre></div>",
        lorCodeService.parseComment("[code][[b]][/code]", false, LorCodeService.Plain$.MODULE$));
    assertEquals("<div class=\"code\"><pre class=\"no-highlight\"><code>[[/b]]</code></pre></div>",
        lorCodeService.parseComment("[code][[/b]][/code]", false, LorCodeService.Plain$.MODULE$));

    assertEquals("<p>[[[doNotTag]]]</p>",
        lorCodeService.parseComment("[[[doNotTag]]]", false, LorCodeService.Plain$.MODULE$));
    assertEquals("<p>[[[/doNotTag]]]</p>",
        lorCodeService.parseComment("[[[/doNotTag]]]", false, LorCodeService.Plain$.MODULE$));
    assertEquals("<p>[[b]]</p>",
        lorCodeService.parseComment("[[[b]]]", false, LorCodeService.Plain$.MODULE$));
    assertEquals("<p>[[/b]]</p>",
        lorCodeService.parseComment("[[[/b]]]", false, LorCodeService.Plain$.MODULE$));

    assertEquals("<div class=\"code\"><pre class=\"no-highlight\"><code>[[b]]</code></pre></div><p>[b]</p>",
        lorCodeService.parseComment("[code][[b]][/code][[b]]", false, LorCodeService.Plain$.MODULE$));
    assertEquals("<div class=\"code\"><pre class=\"no-highlight\"><code>[[/b]]</code></pre></div><p>[b]</p>",
        lorCodeService.parseComment("[code][[/b]][/code][[b]]", false, LorCodeService.Plain$.MODULE$));

    assertEquals("<div class=\"code\"><pre class=\"no-highlight\"><code>[[code]]</code></pre></div>",
        lorCodeService.parseComment("[code][[code]][/code]", false, LorCodeService.Plain$.MODULE$));
    assertEquals("<div class=\"code\"><pre class=\"no-highlight\"><code>[[/code]]</code></pre></div>",
        lorCodeService.parseComment("[code][[/code]][/code]", false, LorCodeService.Plain$.MODULE$));
  }
}
