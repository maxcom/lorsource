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
package ru.org.linux.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * Тесты для {@link StringUtil}.
 */
public class StringUtilTest {
  @Test
  public void processTitle() {
    // given

    //when
    String actualResult = StringUtil.processTitle("one -- two --- three -- four-- five --six --");

    // then
    Assert.assertEquals("one&nbsp;&mdash; two --- three&nbsp;&mdash; four-- five --six --", actualResult);
  }


  @Test
  public void makeTitle() {
    // given

    //when
    String actualResult = StringUtil.makeTitle("\"Test of \"quotes '' \"in quotes\" in title\"\"");

    // then
    Assert.assertEquals("&laquo;Test of &bdquo;quotes &quot; &bdquo;in quotes&ldquo; in title&ldquo;&raquo;", actualResult);
  }
}
