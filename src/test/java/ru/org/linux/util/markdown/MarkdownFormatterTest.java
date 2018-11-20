package ru.org.linux.util.markdown;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

  private MarkdownFormatter markdownFormatter = new FlexmarkMarkdownFormatter();

  @Test
  public void testMarkdownFormatter() {
    assertEquals(TEXT1_RESULT, markdownFormatter.renderToHtml(TEXT1));
  }

}
