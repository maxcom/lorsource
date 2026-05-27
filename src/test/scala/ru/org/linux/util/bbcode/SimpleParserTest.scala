/*
 * Copyright 1998-2026 Linux.org.ru
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
package ru.org.linux.util.bbcode

import munit.FunSuite
import org.apache.commons.httpclient.URI
import org.mockito.Mockito.{mock, when}
import ru.org.linux.spring.SiteConfig
import ru.org.linux.user.UserService
import ru.org.linux.util.bbcode.tags.QuoteTag.{citeFooter, citeHeader}
import ru.org.linux.util.formatter.ToHtmlFormatter

class SimpleParserTest extends FunSuite:
  private lazy val (lorCodeService, url) = initServices()

  private def initServices(): (LorCodeService, String) =
    val mainUrl = "http://127.0.0.1:8080/"
    val mainURI = new URI(mainUrl, true, "UTF-8")
    val siteConfig = mock(classOf[SiteConfig])
    when(siteConfig.getMainURI).thenReturn(mainURI)

    val toHtmlFormatter = new ToHtmlFormatter
    toHtmlFormatter.setSiteConfig(siteConfig)

    val lcs = new LorCodeService(mock(classOf[UserService]), toHtmlFormatter)
    (lcs, "http://127.0.0.1:8080/forum/talks/22464")

  test("brTest") {
    assertEquals(lorCodeService.parseComment("[br]", false, LorCodeService.Plain), "<p><br></p>")
  }

  test("boldTest") {
    assertEquals(
      lorCodeService.parseComment("[b]hello world[/b]", false, LorCodeService.Plain),
      "<p><b>hello world</b></p>")
  }

  test("italicTest") {
    assertEquals(
      lorCodeService.parseComment("[i]hello world[/i]", false, LorCodeService.Plain),
      "<p><i>hello world</i></p>")
  }

  test("strikeoutTest") {
    assertEquals(
      lorCodeService.parseComment("[s]hello world[/s]", false, LorCodeService.Plain),
      "<p><s>hello world</s></p>")
  }

  test("emphasisTest") {
    assertEquals(
      lorCodeService.parseComment("[strong]hello world[/strong]", false, LorCodeService.Plain),
      "<p><strong>hello world</strong></p>")
  }

  test("quoteTest") {
    assertEquals(
      lorCodeService.parseComment("[quote]hello world[/quote]", false, LorCodeService.Plain),
      citeHeader + "<p>hello world</p>" + citeFooter)
  }

  test("quoteTextTest") {
    assertEquals(lorCodeService.extractPlain("[quote]hello world[/quote]", LorCodeService.Plain), "«hello world»")
  }

  test("quoteParamTest") {
    assertEquals(
      lorCodeService.parseComment("[quote=maxcom]hello world[/quote]", false, LorCodeService.Plain),
      citeHeader + "<p><cite>maxcom</cite></p><p>hello world</p>" + citeFooter
    )
  }

  test("quoteCleanTest") {
    assertEquals(lorCodeService.parseComment("[quote][/quote]", false, LorCodeService.Plain), "")
  }

  test("urlTest") {
    assertEquals(
      lorCodeService.parseComment("[url]http://linux.org.ru[/url]", false, LorCodeService.Plain),
      "<p><a href=\"http://linux.org.ru\">http://linux.org.ru</a></p>"
    )
  }

  test("paragraphSpacesTest") {
    assertEquals(
      lorCodeService.parseComment(
        "[url]http://linux.org.ru[/url] [url]http://linux.org.ru[/url]",
        false,
        LorCodeService.Plain),
      "<p><a href=\"http://linux.org.ru\">http://linux.org.ru</a> <a href=\"http://linux.org.ru\">http://linux.org.ru</a></p>"
    )
  }

  test("urlParamTest") {
    assertEquals(
      lorCodeService.parseComment("[url=http://linux.org.ru]linux[/url]", false, LorCodeService.Plain),
      "<p><a href=\"http://linux.org.ru\">linux</a></p>")
  }

  test("urlParamWithTagTest") {
    assertEquals(
      lorCodeService.parseComment("[url=http://linux.org.ru][b]l[/b]inux[/url]", false, LorCodeService.Plain),
      "<p><a href=\"http://linux.org.ru\"><b>l</b>inux</a></p>"
    )
  }

  test("urlParamWithTagTest2") {
    assertEquals(
      lorCodeService.parseComment("[url=http://linux.org.ru][b]linux[/b][/url]", false, LorCodeService.Plain),
      "<p><a href=\"http://linux.org.ru\"><b>linux</b></a></p>"
    )
  }

  test("listTest") {
    assertEquals(
      lorCodeService.parseComment("[list][*]one[*]two[*]three[/list]", false, LorCodeService.Plain),
      "<ul><li>one</li><li>two</li><li>three</li></ul>")
    assertEquals(
      lorCodeService.parseComment("[list]\n[*]one\n[*]two\n[*]three\n[/list]", false, LorCodeService.Plain),
      "<ul><li>one\n</li><li>two\n</li><li>three\n</li></ul>"
    )
    assertEquals(
      lorCodeService.parseTopic("[list]\n[*]one\n[*]two\n[*]three\n[/list]", false, LorCodeService.Plain),
      "<ul><li>one\n</li><li>two\n</li><li>three\n</li></ul>"
    )
  }

  test("overflow1Test") {
    assertEquals(
      lorCodeService.parseComment("ololo[quote][i]hz[/i][/quote]", false, LorCodeService.Plain),
      "<p>ololo</p>" + citeHeader + "<p><i>hz</i></p>" + citeFooter)
  }

  test("preTest") {
    assertEquals(
      lorCodeService.parseComment(
        """[pre]Тег используем мы этот,
          |Чтобы строки разделять,
          |Если вдруг стихи захочем
          |Здесь, на ЛОРе, запощать.
          |
          |Ну а строфы разделяем
          |Как привыкли уж давно![/pre]""".stripMargin,
        false,
        LorCodeService.Plain
      ),
      """<pre>Тег используем мы этот,
        |Чтобы строки разделять,
        |Если вдруг стихи захочем
        |Здесь, на ЛОРе, запощать.
        |
        |Ну а строфы разделяем
        |Как привыкли уж давно!</pre>""".stripMargin
    )
  }

  test("spacesTest") {
    assertEquals(
      lorCodeService.parseComment(
        "some text\n\n some again text [url=http://example.com]example[/url] example",
        false,
        LorCodeService.Plain),
      "<p>some text</p><p> some again text <a href=\"http://example.com\">example</a> example</p>"
    )
  }

  test("cutTest") {
    assertEquals(lorCodeService.parseComment("[cut]test[/cut]", false, LorCodeService.Plain), "<p>test</p>")
    assertEquals(
      lorCodeService.parseTopicWithMinimizedCut("[cut]test[/cut]", url, false, LorCodeService.Plain),
      "<p>( <a href=\"http://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>"
    )
    assertEquals(
      lorCodeService.parseTopicWithMinimizedCut("[cut]test[/cut]", url, false, LorCodeService.Plain),
      "<p>( <a href=\"http://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>"
    )
    assertEquals(
      lorCodeService.parseTopic("[cut]test[/cut]", false, LorCodeService.Plain),
      "<div id=\"cut0\"><p>test</p></div>")
  }

  test("cut2Test") {
    assertEquals(lorCodeService.parseComment("[cut]\n\ntest[/cut]", false, LorCodeService.Plain), "<p>test</p>")
    assertEquals(
      lorCodeService.parseTopicWithMinimizedCut("[cut]\n\ntest[/cut]", url, false, LorCodeService.Plain),
      "<p>( <a href=\"http://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>"
    )
    assertEquals(
      lorCodeService.parseTopicWithMinimizedCut("[cut]\n\ntest[/cut]", url, false, LorCodeService.Plain),
      "<p>( <a href=\"http://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>"
    )
    assertEquals(
      lorCodeService.parseTopic("[cut]\n\ntest[/cut]", false, LorCodeService.Plain),
      "<div id=\"cut0\"><p>test</p></div>")
  }

  test("cut3Test") {
    assertEquals(
      lorCodeService.parseTopic("some text\n\n[cut]\n\n[list][*]one\n\n[*]\n\ntwo[/cut]", false, LorCodeService.Plain),
      "<p>some text</p><div id=\"cut0\"><ul><li>one</li><li><p>two</p></li></ul></div>"
    )
    assertEquals(
      lorCodeService.parseTopicWithMinimizedCut(
        "some text\n\n[cut]\n\n[list][*]one\n\n[*]\n\ntwo[/cut]",
        url,
        false,
        LorCodeService.Plain),
      "<p>some text</p><p>( <a href=\"http://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>"
    )
    assertEquals(
      lorCodeService.parseTopicWithMinimizedCut(
        "some text\n\n[cut]\n\n[list][*]one\n\n[*]\n\ntwo[/cut]",
        url,
        false,
        LorCodeService.Plain),
      "<p>some text</p><p>( <a href=\"http://127.0.0.1:8080/forum/talks/22464#cut0\">читать дальше...</a> )</p>"
    )
  }

  test("cut4Test") {
    assertEquals(
      lorCodeService.parseTopic("[cut]\n\ntest[/cut][cut]test[/cut]", false, LorCodeService.Plain),
      "<div id=\"cut0\"><p>test</p></div><div id=\"cut1\"><p>test</p></div>"
    )
  }

  test("appleTest") {
    assertEquals(
      lorCodeService.parseComment("[quote] Apple ][[/quote] текст", false, LorCodeService.Plain),
      citeHeader + "<p> Apple ][</p>" + citeFooter + "<p> текст</p>")
  }

  test("urlParameterQuotesTest") {
    assertEquals(
      lorCodeService.parseComment("[url=\"http://www.example.com]example[/url]", false, LorCodeService.Plain),
      "<p><a href=\"http://www.example.com\">example</a></p>"
    )
    assertEquals(
      lorCodeService.parseComment("[url=\"http://www.example.com\"]example[/url]", false, LorCodeService.Plain),
      "<p><a href=\"http://www.example.com\">example</a></p>"
    )
    assertEquals(
      lorCodeService.parseComment("[url='http://www.example.com']example[/url]", false, LorCodeService.Plain),
      "<p><a href=\"http://www.example.com\">example</a></p>"
    )
    assertEquals(
      lorCodeService.parseComment("[url='http://www.example.com]example[/url]", false, LorCodeService.Plain),
      "<p><a href=\"http://www.example.com\">example</a></p>"
    )
  }

  test("cutWithParameterTest") {
    assertEquals(
      lorCodeService.parseTopicWithMinimizedCut("[cut=нечитать!]\n\ntest[/cut]", url, false, LorCodeService.Plain),
      "<p>( <a href=\"http://127.0.0.1:8080/forum/talks/22464#cut0\">нечитать!</a> )</p>"
    )
  }

  test("autoLinksInList") {
    assertEquals(
      lorCodeService.parseComment("[list][*]www.example.com[*]sure[*]profit![/list]", false, LorCodeService.Plain),
      "<ul><li><a href=\"http://www.example.com\">http://www.example.com</a></li><li>sure</li><li>profit!</li></ul>"
    )
  }

  test("quoteQuoteQuote") {
    assertEquals(
      lorCodeService.parseComment(
        "[quote][quote][quote][quote][quote][quote][quote][quote][quote][quote][quote][quote][quote][quote][quote][quote][quote][quote]прювет![/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote][/quote]",
        false,
        LorCodeService.Plain
      ),
      citeHeader + "<p>прювет!</p>" + citeFooter
    )
  }

  test("escapeDoubleBrackets") {
    assertEquals(lorCodeService.parseComment("[[doNotTag]]", false, LorCodeService.Plain), "<p>[[doNotTag]]</p>")
    assertEquals(lorCodeService.parseComment("[[/doNotTag]]", false, LorCodeService.Plain), "<p>[[/doNotTag]]</p>")
    assertEquals(lorCodeService.parseComment("[[b]]", false, LorCodeService.Plain), "<p>[b]</p>")
    assertEquals(lorCodeService.parseComment("[[/b]]", false, LorCodeService.Plain), "<p>[/b]</p>")

    assertEquals(
      lorCodeService.parseComment("[code][[doNotTag]][/code]", false, LorCodeService.Plain),
      "<div class=\"code\"><pre class=\"no-highlight\"><code>[[doNotTag]]</code></pre></div>"
    )
    assertEquals(
      lorCodeService.parseComment("[code][[/doNotTag]][/code]", false, LorCodeService.Plain),
      "<div class=\"code\"><pre class=\"no-highlight\"><code>[[/doNotTag]]</code></pre></div>"
    )
    assertEquals(
      lorCodeService.parseComment("[code][[b]][/code]", false, LorCodeService.Plain),
      "<div class=\"code\"><pre class=\"no-highlight\"><code>[[b]]</code></pre></div>"
    )
    assertEquals(
      lorCodeService.parseComment("[code][[/b]][/code]", false, LorCodeService.Plain),
      "<div class=\"code\"><pre class=\"no-highlight\"><code>[[/b]]</code></pre></div>"
    )

    assertEquals(lorCodeService.parseComment("[[[doNotTag]]]", false, LorCodeService.Plain), "<p>[[[doNotTag]]]</p>")
    assertEquals(lorCodeService.parseComment("[[[/doNotTag]]]", false, LorCodeService.Plain), "<p>[[[/doNotTag]]]</p>")
    assertEquals(lorCodeService.parseComment("[[[b]]]", false, LorCodeService.Plain), "<p>[[b]]</p>")
    assertEquals(lorCodeService.parseComment("[[[/b]]]", false, LorCodeService.Plain), "<p>[[/b]]</p>")

    assertEquals(
      lorCodeService.parseComment("[code][[b]][/code][[b]]", false, LorCodeService.Plain),
      "<div class=\"code\"><pre class=\"no-highlight\"><code>[[b]]</code></pre></div><p>[b]</p>"
    )
    assertEquals(
      lorCodeService.parseComment("[code][[/b]][/code][[b]]", false, LorCodeService.Plain),
      "<div class=\"code\"><pre class=\"no-highlight\"><code>[[/b]]</code></pre></div><p>[b]</p>"
    )

    assertEquals(
      lorCodeService.parseComment("[code][[code]][/code]", false, LorCodeService.Plain),
      "<div class=\"code\"><pre class=\"no-highlight\"><code>[[code]]</code></pre></div>"
    )
    assertEquals(
      lorCodeService.parseComment("[code][[/code]][/code]", false, LorCodeService.Plain),
      "<div class=\"code\"><pre class=\"no-highlight\"><code>[[/code]]</code></pre></div>"
    )
  }
