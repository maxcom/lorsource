/*
 * Copyright 1998-2018 Linux.org.ru
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
import ru.org.linux.spring.SiteConfig;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Created by bvn13 on 15.11.2018.
 */
public class MarkdownFormatterTest {

  private static final String TEXT1 = "# First header \n" +
          "\n" +
          "## Second Header\n" +
          "\n" +
          "```sql\n" +
          "select id from table1;\n" +
          "```\n" +
          "\n" +
          "Вот такой должно получиться\n" +
          "\n" +
          "И это тоже должно работать";
  private static final String TEXT1_RESULT = "<h1>First header</h1>\n" +
          "<h2>Second Header</h2>\n" +
          "<pre><code class=\"language-sql\">select id from table1;\n" +
          "</code></pre>\n" +
          "<p>Вот такой должно получиться</p>\n" +
          "<p>И это тоже должно работать</p>\n";

  private SiteConfig siteConfig;

  @Before
  public void init() throws Exception {
    siteConfig = mock(SiteConfig.class);
    URI mainURI = new URI("http://www.linux.org.ru/", true, "UTF-8");
    URI secureURI = new URI("https://www.linux.org.ru/", true, "UTF-8");

    Mockito.when(siteConfig.getMainURI()).thenReturn(mainURI);
    Mockito.when(siteConfig.getSecureURI()).thenReturn(secureURI);
  }

  private final MarkdownFormatter markdownFormatter = new FlexmarkMarkdownFormatter(siteConfig);

  @Test
  public void testMarkdownFormatter() {
    assertEquals(TEXT1_RESULT, markdownFormatter.renderToHtml(TEXT1, false));
  }

}
