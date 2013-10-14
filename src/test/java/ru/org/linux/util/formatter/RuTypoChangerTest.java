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
package ru.org.linux.util.formatter;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * тесты для {@link RuTypoChanger}
 */

@RunWith(value = Parameterized.class)
public class RuTypoChangerTest {
  private final String inputString;
  private final String expectedResult;

  private static final RuTypoChanger typoChanger = new RuTypoChanger();

  public RuTypoChangerTest(String inputString, String expectedResult) {
    this.inputString = inputString;
    this.expectedResult = expectedResult;
  }


  @Test
  public void checkQuotesDecorator() {
    // given
    RuTypoChanger ruTypoChanger = new RuTypoChanger();

    // when
    String actualResult = ruTypoChanger.format(inputString);

    // then
    Assert.assertEquals(expectedResult, actualResult);
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    Object[][] data = new Object[][]{
            {"две кавычки вместе: \"\"", "две кавычки вместе: &#171;&#187;"},
            {"пробел между кавычками: \" \"", "пробел между кавычками: &quot; &quot;"},
            {"\"слово\" в кавычках", "&#171;слово&#187; в кавычках"},
            {"\"фраза в кавычках\"", "&#171;фраза в кавычках&#187;"},
            {"\" слово \" с пробелами", "&quot; слово &quot; с пробелами"},
            {"\" фраза с пробелами \"", "&quot; фраза с пробелами &quot;"},
            {"\" фраза с пробелом  в начале\"", "&quot; фраза с пробелом  в начале&quot;"},
            {"\"фраза с пробелом  в конце \"", "&#171;фраза с пробелом  в конце &#187;"},
            {"\"вложенные кавычки \"в конце\"\"", "&#171;вложенные кавычки &#8222;в конце&#8220;&#187;"},
            {"\"\"вложенные кавычки\" в начале\"", "&#171;&#8222;вложенные кавычки&#8220; в начале&#187;"},
            {"\"\"\"\"\"\"\"\"много непарных кавычек в начале\"", "&#171;&#8222;&#8222;&#8222;&#8222;&#8222;&#8222;&#8222;много непарных кавычек в начале&#8220;"},
            {"\"много непарных кавычек в конце\"\"\"\"\"\"\"\"", "&#171;много непарных кавычек в конце&#187;&quot;&quot;&quot;&quot;&quot;&quot;&quot;"},
            {"\"Ты криворукий жабокодер\"™.", "&#171;Ты криворукий жабокодер&#187;™."},
            {"(\"текст в кавычках в скобках\")", "(&#171;текст в кавычках в скобках&#187;)"},
            {"\"волки\"-\"палки\"", "&#171;волки&#187;-&#171;палки&#187;"},
            {"\"волки\"-\"палки\"", "&#171;волки&#187;-&#171;палки&#187;"},
            {"\"Test1 \"Test2 мяу-мяу?\"\"", "&#171;Test1 &#8222;Test2 мяу-мяу?&#8220;&#187;"},
    };
    return Arrays.asList(data);
  }

}
