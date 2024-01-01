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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import ru.org.linux.util.bbcode.nodes.RootNode;

import java.util.Arrays;
import java.util.Collection;

/**
 * Тесты для {@link Parser}.
 */
@RunWith(value = Parameterized.class)
public class ParserTest {
  private final String inputString;
  private final String expectedResult;

  Parser parser;

  RootNode rootNode;

  public ParserTest(String inputString, String expectedResult) {
    this.expectedResult = expectedResult;
    this.inputString = inputString;
  }

  @Before
  public void setUp() {
    parser = new Parser(new DefaultParserParameters());
    rootNode = new RootNode(new DefaultParserParameters());
  }

  @Test
  public void parse() {
    // given

    // when
    parser.parseRoot(rootNode, inputString);

    // then
    String actualResult = rootNode.renderXHtml();
    Assert.assertEquals(expectedResult, actualResult);
  }

  @Parameters
  public static Collection<Object[]> data() {
    Object[][] data = new Object[][]{
            {"[list][*]fdfdddfd[/list][[raw]]", "<ul><li>fdfdddfd</li></ul><p>[[raw]]</p>"},
            {"[list][*]fdfdddfd[list][[raw]]", "<ul><li>fdfdddfd[[raw]]</li></ul>"},
            {
                "[code][list][*]fdfdddfd[list][[raw]][/code][/code]",
                "<div class=\"code\"><pre class=\"no-highlight\"><code>[list][*]fdfdddfd[list][[raw]]</code></pre></div>"
            },
    };
    return Arrays.asList(data);
  }
}
