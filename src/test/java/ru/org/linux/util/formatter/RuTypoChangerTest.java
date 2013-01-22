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

import junit.framework.Assert;
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

  @Test
  public void checkQuotesDecoratorWithReset() {
    // given
    typoChanger.reset();

    // when
    String actualResult = typoChanger.format(inputString);

    // then
    Assert.assertEquals(expectedResult, actualResult);
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    Object[][] data = new Object[][]{
            {"две кавычки вместе: \"\"", "две кавычки вместе: &laquo;&raquo;"},
            {"пробел между кавычками: \" \"", "пробел между кавычками: &quot; &quot;"},
            {"\"слово\" в кавычках", "&laquo;слово&raquo; в кавычках"},
            {"\"фраза в кавычках\"", "&laquo;фраза в кавычках&raquo;"},
            {"\" слово \" с пробелами", "&quot; слово &quot; с пробелами"},
            {"\" фраза с пробелами \"", "&quot; фраза с пробелами &quot;"},
            {"\" фраза с пробелом  в начале\"", "&quot; фраза с пробелом  в начале&quot;"},
            {"\"фраза с пробелом  в конце \"", "&laquo;фраза с пробелом  в конце &raquo;"},
            {"\"вложенные кавычки \"в конце\"\"", "&laquo;вложенные кавычки &bdquo;в конце&ldquo;&raquo;"},
            {"\"\"вложенные кавычки\" в начале\"", "&laquo;&bdquo;вложенные кавычки&ldquo; в начале&raquo;"},
            {"\"\"\"\"\"\"\"\"много непарных кавычек в начале\"", "&laquo;&bdquo;&bdquo;&bdquo;&bdquo;&bdquo;&bdquo;&bdquo;много непарных кавычек в начале&ldquo;"},
            {"\"много непарных кавычек в конце\"\"\"\"\"\"\"\"", "&laquo;много непарных кавычек в конце&raquo;&quot;&quot;&quot;&quot;&quot;&quot;&quot;"},
            {"\"Ты криворукий жабокодер\"™.", "&laquo;Ты криворукий жабокодер&raquo;™."},
            {"(\"текст в кавычках в скобках\")", "(&laquo;текст в кавычках в скобках&raquo;)"},
            {"\"волки\"-\"палки\"", "&laquo;волки&raquo;-&laquo;палки&raquo;"},
            {"\"волки\"-\"палки\"", "&laquo;волки&raquo;-&laquo;палки&raquo;"},
            {"\"Test1 \"Test2 мяу-мяу?\"\"", "&laquo;Test1 &bdquo;Test2 мяу-мяу?&ldquo;&raquo;"},
    };
    return Arrays.asList(data);
  }

}
