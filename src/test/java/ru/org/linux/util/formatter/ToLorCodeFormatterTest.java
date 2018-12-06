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

package ru.org.linux.util.formatter;

import org.junit.Before;
import org.junit.Test;
import ru.org.linux.comment.CommentDao;
import ru.org.linux.markup.MessageTextService;
import ru.org.linux.spring.SiteConfig;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.util.bbcode.LorCodeService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class ToLorCodeFormatterTest {
  private static final String QUOTING1 = "> 1";
  private static final String RESULT_QUOTING1 = "[quote] 1[/quote]";

  private static final String QUOTING2 = "> 1\n2";
  private static final String RESULT_QUOTING2 = "[quote] 1[br][/quote]2";

  private static final String QUOTING3 = "> 1\n2\n\n3";
  private static final String RESULT_QUOTING3 = "[quote] 1[br][/quote]2\n\n3";

  @Before
  public void init() {
    TopicDao messageDao = mock(TopicDao.class);
    CommentDao commentDao = mock(CommentDao.class);

    SiteConfig siteConfig = mock(SiteConfig.class);

    ToHtmlFormatter toHtmlFormatter = new ToHtmlFormatter();
    toHtmlFormatter.setSiteConfig(siteConfig);
    toHtmlFormatter.setMessageDao(messageDao);
    toHtmlFormatter.setCommentDao(commentDao);

    LorCodeService lorCodeService = new LorCodeService();
    lorCodeService.setToHtmlFormatter(toHtmlFormatter);
  }

  private String formatNtobr(String str) {
    return MessageTextService.preprocessPostingText(str, "ntobr").text();
  }

  private String format(String str) {
    return MessageTextService.preprocessPostingText(str, "lorcode").text();
  }

  @Test
  public void testToLorCodeTexFormatter() {
    assertEquals(RESULT_QUOTING1, format(QUOTING1));
    assertEquals(RESULT_QUOTING2, format(QUOTING2));
    assertEquals(RESULT_QUOTING3, format(QUOTING3));

    assertEquals("[quote]test[br][/quote]test", format(">test\n\ntest")); // 4
    assertEquals("test\n\ntest\ntest", format("test\n\ntest\ntest")); // 1
    assertEquals("test\n\n[quote]test[/quote]", format("test\n\n>test")); // 7
    assertEquals("test &", formatNtobr("test &")); // 8
    assertEquals("test[br]test", formatNtobr("test\r\ntest")); // 9
    assertEquals("test[br]test", formatNtobr("test\ntest")); // 10
    assertEquals("[quote]test[br][/quote]test", formatNtobr(">test\ntest")); // 11
    assertEquals("[quote]test[br]test[/quote]", formatNtobr(">test\n>test")); // 12
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
    assertEquals("[code][/code]", format("[code][/code]"));
    assertEquals("[code=perl][/code]", format("[code=perl][/code]"));
  }

  @Test
  public void codeAndQuoteTest() {
    assertEquals(
            "[quote] test [br][/quote][code]\n" +
            "> test\n" +
            "[/code]",
            format(
                    "> test \n\n"+
                    "[code]\n"+
                    "> test\n"+
                    "[/code]")
    );

    assertEquals(
            "[quote] test [br][/quote][code]\n" +
            "> test\n" +
            "[/code]",
            format(
                    "> test \n"+
                    "[code]\n"+
                    "> test\n"+
                    "[/code]")
    );

    assertEquals(
            "[quote] test [br] [[code]] [br] test [/quote]",
            format(
                    "> test \n"+
                    "> [code] \n"+
                    "> test \n")
    );

    assertEquals(
            "[quote] test [[code]] [br] test[br] test [[/code]][/quote]",
            format(
                    "> test [code] \n"+
                    "> test\n"+
                    "> test [/code]\n")
    );

    assertEquals(
            "[code]test[/code]\n" +
            "[quote] test[/quote]",
            format(
                    "[code]test[/code]\n"+
                    "> test\n")
    );

    assertEquals(
            "[[code]] test",
            format(
              "[[code]] test"
            )
    );

    assertEquals(
            "[quote] [[code]] test[/quote]",
            format(
              "> [[code]] test"
            )
    );

    assertEquals(
            "[[code]] test\n" +
            "[quote] test[/quote]",
            format(
              "[[code]] test\n" +
              "> test\n"
            )
    );

  }

  @Test
  public void againQuoteFormatter() {
    assertEquals("[quote]one[br][quote]two[br][/quote]one[br][quote][quote]three[/quote][/quote][/quote]",
        formatNtobr(">one\n>>two\n>one\n>>>three"));
    assertEquals("[quote]one[br][quote]two[br][/quote]one[br][quote][quote]three[/quote][/quote][/quote]",
            format(">one\n>>two\n>one\n>>>three"));
  }
}
