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

  }
}
