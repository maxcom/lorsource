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
package ru.org.linux.util

import munit.FunSuite

/** Тесты для [[StringUtil]]. */
class StringUtilTest extends FunSuite:
  test("makeTitle mdash"):
    val actualResult = StringUtil.makeTitle("one -- two --- three -- four-- five --six --")
    assertEquals("one\u00A0— two --- three\u00A0— four-- five --six --", actualResult)

  test("makeTitle quotes"):
    val actualResult = StringUtil.makeTitle("\"Test of \"quotes '' \"in quotes\" in title\"\"")
    assertEquals("«Test of „quotes \" „in quotes“ in title“»", actualResult)

  test("makeTitle quotes and mdash together"):
    val actualResult = StringUtil.makeTitle("\"linux -- \"the kernel\"\"")
    assertEquals("«linux\u00A0— „the kernel“»", actualResult)

  test("makeTitle returns raw text without HTML entities"):
    // заголовки хранятся в исходном виде; экранирование — при отображении
    val actualResult = StringUtil.makeTitle("\"a\" <b> &amp; &#39; c")
    assertEquals("«a» <b> &amp; &#39; c", actualResult)

  test("makeTitle decodes legacy RuTypoChanger quote entities"):
    val actualResult = StringUtil.makeTitle("Сладкий вкус &#8220;свободного&#8221; кофе")
    assertEquals("Сладкий вкус “свободного” кофе", actualResult)

  test("makeTitle trims whitespace"):
    assertEquals("Заголовок", StringUtil.makeTitle("  Заголовок  "))

  test("makeTitle empty"):
    assertEquals("Без заглавия", StringUtil.makeTitle("  "))
    assertEquals("Без заглавия", StringUtil.makeTitle(null))
