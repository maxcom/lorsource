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

package ru.org.linux.util.formatter

import munit.FunSuite
import ru.org.linux.util.bbcode.LorCodeService.{prepareLorcode, prepareUlb}

class ToLorCodeFormatterTest extends FunSuite:
  private val Quoting1 = "> 1"
  private val ResultQuoting1 = "[quote] 1[/quote]"

  private val Quoting2 = "> 1\n2"
  private val ResultQuoting2 = "[quote] 1[br][/quote]2"

  private val Quoting3 = "> 1\n2\n\n3"
  private val ResultQuoting3 = "[quote] 1[br][/quote]2\n\n3"

  test("testToLorCodeTexFormatter"):
    assertEquals(ResultQuoting1, prepareLorcode(Quoting1))
    assertEquals(ResultQuoting2, prepareLorcode(Quoting2))
    assertEquals(ResultQuoting3, prepareLorcode(Quoting3))

    assertEquals("[quote]test[br][/quote]test", prepareLorcode(">test\n\ntest"))
    assertEquals("test\n\ntest\ntest", prepareLorcode("test\n\ntest\ntest"))
    assertEquals("test\n\n[quote]test[/quote]", prepareLorcode("test\n\n>test"))
    assertEquals("test &", prepareUlb("test &"))
    assertEquals("test[br]test", prepareUlb("test\r\ntest"))
    assertEquals("test[br]test", prepareUlb("test\ntest"))
    assertEquals("[quote]test[br][/quote]test", prepareUlb(">test\ntest"))
    assertEquals("[quote]test[br]test[/quote]", prepareUlb(">test\n>test"))

  test("codeEscapeBasic"):
    assertEquals("[[code]]", ToLorCodeTexFormatter.escapeCode("[code]"))
    assertEquals(" [[code]]", ToLorCodeTexFormatter.escapeCode(" [code]"))
    assertEquals("[[/code]]", ToLorCodeTexFormatter.escapeCode("[/code]"))
    assertEquals(" [[/code]]", ToLorCodeTexFormatter.escapeCode(" [/code]"))
    assertEquals("[[code]]", ToLorCodeTexFormatter.escapeCode("[[code]]"))
    assertEquals(" [[code]]", ToLorCodeTexFormatter.escapeCode(" [[code]]"))
    assertEquals(" [[/code]]", ToLorCodeTexFormatter.escapeCode(" [[/code]]"))

    assertEquals("][[code]]", ToLorCodeTexFormatter.escapeCode("][code]"))
    assertEquals("[[code]] [[code]]", ToLorCodeTexFormatter.escapeCode("[code] [code]"))
    assertEquals("[[code]] [[/code]]", ToLorCodeTexFormatter.escapeCode("[code] [/code]"))

  test("codeEscape"):
    assertEquals("[code][/code]", prepareLorcode("[code][/code]"))
    assertEquals("[code=perl][/code]", prepareLorcode("[code=perl][/code]"))

  test("codeAndQuoteTest"):
    assertEquals(
      "[quote] test [br][/quote][code]\n> test\n[/code]",
      prepareLorcode("> test \n\n[code]\n> test\n[/code]"))

    assertEquals(
      "[quote] test [br][/quote][code]\n> test\n[/code]",
      prepareLorcode("> test \n[code]\n> test\n[/code]"))

    assertEquals(
      "[quote] test [br] [[code]] [br] test [/quote]",
      prepareLorcode("> test \n> [code] \n> test \n"))

    assertEquals(
      "[quote] test [[code]] [br] test[br] test [[/code]][/quote]",
      prepareLorcode("> test [code] \n> test\n> test [/code]\n"))

    assertEquals(
      "[code]test[/code]\n[quote] test[/quote]",
      prepareLorcode("[code]test[/code]\n> test\n"))

    assertEquals(
      "[[code]] test",
      prepareLorcode("[[code]] test"))

    assertEquals(
      "[quote] [[code]] test[/quote]",
      prepareLorcode("> [[code]] test"))

    assertEquals(
      "[[code]] test\n[quote] test[/quote]",
      prepareLorcode("[[code]] test\n> test\n"))

  test("againQuoteFormatter"):
    assertEquals(
      "[quote]one[br][quote]two[br][/quote]one[br][quote][quote]three[/quote][/quote][/quote]",
      prepareUlb(">one\n>>two\n>one\n>>>three"))
    assertEquals(
      "[quote]one[br][quote]two[br][/quote]one[br][quote][quote]three[/quote][/quote][/quote]",
      prepareLorcode(">one\n>>two\n>one\n>>>three"))

  test("ignoreFirstQuoteNl"):
    assertEquals(
      "text[quote]one[br]two[br]three[br][/quote]text",
      prepareUlb("text[quote]\none\ntwo\nthree\n[/quote]text"))