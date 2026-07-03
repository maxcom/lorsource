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

package ru.org.linux.util.markdown;

import org.apache.commons.httpclient.URI;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.org.linux.comment.CommentDao;
import ru.org.linux.spring.SiteConfig;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.user.UserService;
import ru.org.linux.util.formatter.ToHtmlFormatter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class MarkdownFormatterTest {
  private static final String TEXT1 = """
          # First header\s

          ## Second Header

          ```sql
          select id from table1;
          ```

          Вот такой должно получиться

          И это тоже должно работать""";
  private static final String TEXT1_RESULT = """
          <h1>First header</h1>
          <h2>Second Header</h2>
          <div class="code"><pre><code class="language-sql">select id from table1;
          </code></pre>
          </div>
          <p>Вот такой должно получиться</p>
          <p>И это тоже должно работать</p>
          """;

  private MarkdownFormatter markdownFormatter;

  @Before
  public void init() throws Exception {
    SiteConfig siteConfig = mock(SiteConfig.class);
    URI mainURI = new URI("http://www.linux.org.ru/", true, "UTF-8");
    URI secureURI = new URI("https://www.linux.org.ru/", true, "UTF-8");

    TopicDao topicDao = mock(TopicDao.class);
    CommentDao commentDao = mock(CommentDao.class);

    Mockito.when(siteConfig.getMainURI()).thenReturn(mainURI);
    Mockito.when(siteConfig.getSecureURI()).thenReturn(secureURI);

    markdownFormatter = new FlexmarkMarkdownFormatter(siteConfig, topicDao, commentDao, mock(UserService.class),
            new ToHtmlFormatter());
  }


  @Test
  public void testMarkdownFormatter() {
    assertEquals(TEXT1_RESULT, markdownFormatter.renderToHtml(TEXT1, false));
  }

  @Test
  public void testLinkText() {
    assertEquals("https://www.linux.org.ru/",
            markdownFormatter.renderToText("https://www.linux.org.ru/"));

    assertEquals("test https://www.linux.org.ru/",
            markdownFormatter.renderToText("[test](https://www.linux.org.ru/)"));

    assertEquals( "X".repeat(100) + "test https://www.linux.org.ru/ 1234",
            markdownFormatter.renderToText("X".repeat(100) + "[test](https://www.linux.org.ru/) 1234"));

    assertEquals("@ (linux.org.ru) https://www.linux.org.ru/",
            markdownFormatter.renderToText("[@](https://www.linux.org.ru/)"));

    assertEquals("@ (---) http://#$#@$@QW",
            markdownFormatter.renderToText("[@](http://#$#@$@QW)"));
  }

  // упоминание пользователя через @ должно подсвечиваться, даже если оно
  // стоит в начале строки внутри абзаца (после мягкого переноса \n или \r\n).
  // См. https://127.0.0.1:8080/forum/linux-org-ru/1948702?cid=1949491
  @Test
  public void testMentionAfterSoftLineBreak() {
    String lf = "@Aceler\n@Aceler";
    String crlf = "@Aceler\r\n@Aceler\r\n@Aceler";

    String renderedLf = markdownFormatter.renderToHtml(lf, false);
    String renderedCrlf = markdownFormatter.renderToHtml(crlf, false);

    // несуществующий в моке пользователь рендерится в <s>@nick</s>;
    // каждое обнаруженное упоминание даёт такой блок
    int countLf = countOccurrences(renderedLf, "<s>@Aceler</s>");
    int countCrlf = countOccurrences(renderedCrlf, "<s>@Aceler</s>");

    assertEquals(2, countLf);
    assertEquals(3, countCrlf);
  }

  private static int countOccurrences(String haystack, String needle) {
    int count = 0;
    int idx = 0;
    while ((idx = haystack.indexOf(needle, idx)) != -1) {
      count++;
      idx += needle.length();
    }
    return count;
  }
}
