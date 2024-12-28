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

package ru.org.linux.util.formatter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ru.org.linux.util.bbcode.LorCodeService.prepareLorcode;
import static ru.org.linux.util.bbcode.LorCodeService.prepareUlb;

public class ToLorCodeFormatterTest {
  private static final String QUOTING1 = "> 1";
  private static final String RESULT_QUOTING1 = "[quote] 1[/quote]";

  private static final String QUOTING2 = "> 1\n2";
  private static final String RESULT_QUOTING2 = "[quote] 1[br][/quote]2";

  private static final String QUOTING3 = "> 1\n2\n\n3";
  private static final String RESULT_QUOTING3 = "[quote] 1[br][/quote]2\n\n3";

  @Test
  public void testToLorCodeTexFormatter() {
    assertEquals(RESULT_QUOTING1, prepareLorcode(QUOTING1));
    assertEquals(RESULT_QUOTING2, prepareLorcode(QUOTING2));
    assertEquals(RESULT_QUOTING3, prepareLorcode(QUOTING3));

    assertEquals("[quote]test[br][/quote]test", prepareLorcode(">test\n\ntest")); // 4
    assertEquals("test\n\ntest\ntest", prepareLorcode("test\n\ntest\ntest")); // 1
    assertEquals("test\n\n[quote]test[/quote]", prepareLorcode("test\n\n>test")); // 7
    assertEquals("test &", prepareUlb("test &")); // 8
    assertEquals("test[br]test", prepareUlb("test\r\ntest")); // 9
    assertEquals("test[br]test", prepareUlb("test\ntest")); // 10
    assertEquals("[quote]test[br][/quote]test", prepareUlb(">test\ntest")); // 11
    assertEquals("[quote]test[br]test[/quote]", prepareUlb(">test\n>test")); // 12
  }

  @Test
  public void codeEscapeBasic() {
    assertEquals("[[code]]", ToLorCodeTexFormatter.escapeCode("[code]"));
    assertEquals(" [[code]]", ToLorCodeTexFormatter.escapeCode(" [code]"));
    assertEquals("[[/code]]", ToLorCodeTexFormatter.escapeCode("[/code]"));
    assertEquals(" [[/code]]", ToLorCodeTexFormatter.escapeCode(" [/code]"));
    assertEquals("[[code]]", ToLorCodeTexFormatter.escapeCode("[[code]]"));
    assertEquals(" [[code]]", ToLorCodeTexFormatter.escapeCode(" [[code]]"));
    assertEquals(" [[/code]]", ToLorCodeTexFormatter.escapeCode(" [[/code]]"));

    assertEquals("][[code]]", ToLorCodeTexFormatter.escapeCode("][code]"));
    assertEquals("[[code]] [[code]]", ToLorCodeTexFormatter.escapeCode("[code] [code]"));
    assertEquals("[[code]] [[/code]]", ToLorCodeTexFormatter.escapeCode("[code] [/code]"));
  }

  @Test
  public void codeEscape() {
    assertEquals("[code][/code]", prepareLorcode("[code][/code]"));
    assertEquals("[code=perl][/code]", prepareLorcode("[code=perl][/code]"));
  }

  @Test
  public void codeAndQuoteTest() {
    assertEquals(
            """
                    [quote] test [br][/quote][code]
                    > test
                    [/code]""",
            prepareLorcode("""
                    > test\s
                    
                    [code]
                    > test
                    [/code]""")
    );

    assertEquals(
            """
                    [quote] test [br][/quote][code]
                    > test
                    [/code]""",
            prepareLorcode("""
                    > test\s
                    [code]
                    > test
                    [/code]""")
    );

    assertEquals(
            "[quote] test [br] [[code]] [br] test [/quote]",
            prepareLorcode("""
                    > test\s
                    > [code]\s
                    > test\s
                    """)
    );

    assertEquals(
            "[quote] test [[code]] [br] test[br] test [[/code]][/quote]",
            prepareLorcode("""
                    > test [code]\s
                    > test
                    > test [/code]
                    """)
    );

    assertEquals(
            "[code]test[/code]\n" +
            "[quote] test[/quote]",
            prepareLorcode("""
                    [code]test[/code]
                    > test
                    """)
    );

    assertEquals(
            "[[code]] test",
            prepareLorcode("[[code]] test")
    );

    assertEquals(
            "[quote] [[code]] test[/quote]",
            prepareLorcode("> [[code]] test")
    );

    assertEquals(
            "[[code]] test\n" +
            "[quote] test[/quote]",
            prepareLorcode("""
                    [[code]] test
                    > test
                    """)
    );

  }

  @Test
  public void againQuoteFormatter() {
    assertEquals("[quote]one[br][quote]two[br][/quote]one[br][quote][quote]three[/quote][/quote][/quote]",
            prepareUlb(">one\n>>two\n>one\n>>>three"));
    assertEquals("[quote]one[br][quote]two[br][/quote]one[br][quote][quote]three[/quote][/quote][/quote]",
            prepareLorcode(">one\n>>two\n>one\n>>>three"));
  }
}
