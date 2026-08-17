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

package ru.org.linux.util.markdown

import munit.FunSuite
import org.apache.commons.httpclient.URI
import org.mockito.Mockito.{mock, when}
import ru.org.linux.comment.CommentDao
import ru.org.linux.spring.SiteConfig
import ru.org.linux.topic.TopicDao
import ru.org.linux.user.UserService
import ru.org.linux.util.formatter.ToHtmlFormatter

class MarkdownFormatterTest extends FunSuite:
  private val Text1 =
    """|# First header 
      |
      |## Second Header
      |
      |```sql
      |select id from table1;
      |```
      |
      |Вот такой должно получиться
      |
      |И это тоже должно работать""".stripMargin

  private val Text1Result =
    """|<h1>First header</h1>
      |<h2>Second Header</h2>
      |<div class="code"><pre><code class="language-sql">select id from table1;
      |</code></pre>
      |</div>
      |<p>Вот такой должно получиться</p>
      |<p>И это тоже должно работать</p>
      |""".stripMargin

  private lazy val markdownFormatter = initFormatter()

  private def initFormatter(): MarkdownFormatter =
    val siteConfig = mock(classOf[SiteConfig])
    val mainURI = new URI("http://www.linux.org.ru/", true, "UTF-8")
    val secureURI = new URI("https://www.linux.org.ru/", true, "UTF-8")

    val topicDao = mock(classOf[TopicDao])
    val commentDao = mock(classOf[CommentDao])

    when(siteConfig.getMainURI).thenReturn(mainURI)
    when(siteConfig.getSecureURI).thenReturn(secureURI)

    new FlexmarkMarkdownFormatter(siteConfig, topicDao, commentDao, mock(classOf[UserService]),
      new ToHtmlFormatter)

  test("testMarkdownFormatter"):
    assertEquals(Text1Result, markdownFormatter.renderToHtml(Text1, false))

  test("testLinkText"):
    assertEquals("https://www.linux.org.ru/",
      markdownFormatter.renderToText("https://www.linux.org.ru/"))

    assertEquals("test https://www.linux.org.ru/",
      markdownFormatter.renderToText("[test](https://www.linux.org.ru/)"))

    assertEquals("X".repeat(100) + "test https://www.linux.org.ru/ 1234",
      markdownFormatter.renderToText("X".repeat(100) + "[test](https://www.linux.org.ru/) 1234"))

    assertEquals("@ (linux.org.ru) https://www.linux.org.ru/",
      markdownFormatter.renderToText("[@](https://www.linux.org.ru/)"))

    assertEquals("@ (---) http://#$#@$@QW",
      markdownFormatter.renderToText("[@](http://#$#@$@QW)"))

  // упоминание пользователя через @ должно подсвечиваться, даже если оно
  // стоит в начале строки внутри абзаца (после мягкого переноса \n или \r\n).
  // См. https://127.0.0.1:8080/forum/linux-org-ru/1948702?cid=1949491
  test("testMentionAfterSoftLineBreak"):
    val lf = "@Aceler\n@Aceler"
    val crlf = "@Aceler\r\n@Aceler\r\n@Aceler"

    val renderedLf = markdownFormatter.renderToHtml(lf, false)
    val renderedCrlf = markdownFormatter.renderToHtml(crlf, false)

    // несуществующий в моке пользователь рендерится в <s>@nick</s>;
    // каждое обнаруженное упоминание даёт такой блок
    val countLf = countOccurrences(renderedLf, "<s>@Aceler</s>")
    val countCrlf = countOccurrences(renderedCrlf, "<s>@Aceler</s>")

    assertEquals(2, countLf)
    assertEquals(3, countCrlf)

  private def countOccurrences(haystack: String, needle: String): Int =
    var count = 0
    var idx = 0
    while { idx = haystack.indexOf(needle, idx); idx != -1 } do
      count += 1
      idx += needle.length
    count