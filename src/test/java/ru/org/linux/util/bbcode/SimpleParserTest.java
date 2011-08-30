package ru.org.linux.util.bbcode;

import junit.framework.Assert;
import org.junit.Test;
import ru.org.linux.site.User;
import ru.org.linux.site.UserNotFoundException;
import ru.org.linux.spring.dao.UserDao;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: hizel
 * Date: 7/5/11
 * Time: 5:06 PM
 */
public class SimpleParserTest {

  @Test
  public void brTest() {
    Assert.assertEquals(ParserUtil.bb2xhtml("[br]"), "<p><br></p>");
  }

  @Test
  public void boldTest() {
    Assert.assertEquals(ParserUtil.bb2xhtml("[b]hello world[/b]"), "<p><b>hello world</b></p>");
  }

  @Test
  public void italicTest() {
    Assert.assertEquals(ParserUtil.bb2xhtml("[i]hello world[/i]"), "<p><i>hello world</i></p>");
  }

  @Test
  public void strikeoutTest() {
    Assert.assertEquals(ParserUtil.bb2xhtml("[s]hello world[/s]"), "<p><s>hello world</s></p>");
  }

  @Test
  public void emphasisTest() {
    Assert.assertEquals(ParserUtil.bb2xhtml("[strong]hello world[/strong]"), "<p><strong>hello world</strong></p>");
  }

  @Test
  public void quoteTest() {
    // TODO я нрипонял зачем <div> :-(
    Assert.assertEquals(ParserUtil.bb2xhtml("[quote]hello world[/quote]"), "<div class=\"quote\"><h3>Цитата</h3><p>hello world</p></div>");
  }

  @Test
  public void quoteParamTest() {
    // TODO я нрипонял зачем <div> :-(
    Assert.assertEquals(ParserUtil.bb2xhtml("[quote=maxcom]hello world[/quote]"), "<div class=\"quote\"><h3>maxcom</h3><p>hello world</p></div>");
  }

  @Test
  public void quoteCleanTest() {
    Assert.assertEquals("", ParserUtil.bb2xhtml("[quote][/quote]"));
  }

  @Test
  public void cutTest() {
    Assert.assertEquals("<p>test</p>", ParserUtil.bb2xhtml("[cut]test[/cut]", true, true, ""));
    Assert.assertEquals("<p>( <a href=\"#cut0\">читать дальше...</a> )</p>", ParserUtil.bb2xhtml("[cut]test[/cut]", false, false, ""));
    Assert.assertEquals("<p>( <a href=\"#cut0\">читать дальше...</a> )</p>", ParserUtil.bb2xhtml("[cut]test[/cut]", false, true, ""));
    Assert.assertEquals("<div id=\"cut0\"><p>test</p></div>", ParserUtil.bb2xhtml("[cut]test[/cut]", true, false, ""));
  }

  @Test
  public void cut2Test() {
    Assert.assertEquals("<p>test</p>", ParserUtil.bb2xhtml("[cut]\n\ntest[/cut]", true, true, ""));
    Assert.assertEquals("<p>( <a href=\"#cut0\">читать дальше...</a> )</p>", ParserUtil.bb2xhtml("[cut]\n\ntest[/cut]", false, false, ""));
    Assert.assertEquals("<p>( <a href=\"#cut0\">читать дальше...</a> )</p>", ParserUtil.bb2xhtml("[cut]\n\ntest[/cut]", false, true, ""));
    Assert.assertEquals("<div id=\"cut0\"><p>test</p></div>", ParserUtil.bb2xhtml("[cut]\n\ntest[/cut]", true, false, ""));
  }

  @Test
  public void cut3Test() {
    Assert.assertEquals("<p>some text</p><div id=\"cut0\"><ul><li>one</li><li><p>two</p></li></ul></div>",
        ParserUtil.bb2xhtml("some text\n\n[cut]\n\n[list][*]one\n\n[*]\n\ntwo[/cut]", true, false, ""));
    Assert.assertEquals("<p>some text</p><p>( <a href=\"#cut0\">читать дальше...</a> )</p>", ParserUtil.bb2xhtml("some text\n\n[cut]\n\n[list][*]one\n\n[*]\n\ntwo[/cut]", false, false, ""));
  }

  @Test
  public void cut4Test() {
    Assert.assertEquals("<div id=\"cut0\"><p>test</p></div><div id=\"cut1\"><p>test</p></div>", ParserUtil.bb2xhtml("[cut]\n\ntest[/cut][cut]test[/cut]", true, false, ""));
  }

  @Test
  public void urlTest() {
    Assert.assertEquals(ParserUtil.bb2xhtml("[url]http://linux.org.ru[/url]"), "<p><a href=\"http://linux.org.ru\">http://linux.org.ru</a></p>");
  }

  @Test
  public void urlParamTest() {
    Assert.assertEquals(ParserUtil.bb2xhtml("[url=http://linux.org.ru]linux[/url]"), "<p><a href=\"http://linux.org.ru\">linux</a></p>");
  }

  @Test
  public void urlParamWithTagTest() {
    Assert.assertEquals(ParserUtil.bb2xhtml("[url=http://linux.org.ru][b]l[/b]inux[/url]"), "<p><a href=\"http://linux.org.ru\"><b>l</b>inux</a></p>");
  }

  @Test
  public void urlParamWithTagTest2() {
    Assert.assertEquals(ParserUtil.bb2xhtml("[url=http://linux.org.ru][b]linux[/b][/url]"), "<p><a href=\"http://linux.org.ru\"><b>linux</b></a></p>");
  }

  @Test
  public void listTest() {
    Assert.assertEquals(ParserUtil.bb2xhtml("[list][*]one[*]two[*]three[/list]"), "<ul><li>one</li><li>two</li><li>three</li></ul>");
  }

  @Test
  public void codeTest() {
    Assert.assertEquals(ParserUtil.bb2xhtml("[code][list][*]one[*]two[*]three[/list][/code]"), "<div class=\"code\"><pre class=\"no-highlight\"><code>[list][*]one[*]two[*]three[/list]</code></pre></div>");
  }

  @Test
  public void codeCleanTest() {
    Assert.assertEquals("", ParserUtil.bb2xhtml("[code][/code]"));
  }

  @Test
  public void codeKnowTest() {
    Assert.assertEquals("<div class=\"code\"><pre class=\"language-cpp\"><code>#include &lt;stdio.h&gt;</code></pre></div>",
            ParserUtil.bb2xhtml("[code=cxx]#include <stdio.h>[/code]"));
  }

  @Test
  public void codeUnKnowTest() {
    Assert.assertEquals("<div class=\"code\"><pre class=\"no-highlight\"><code>#include &lt;stdio.h&gt;</code></pre></div>",
            ParserUtil.bb2xhtml("[code=foo]#include <stdio.h>[/code]"));
  }

  @Test
  public void overflow1Test() {
    Assert.assertEquals("<p>ololo</p><div class=\"quote\"><h3>Цитата</h3><p><i>hz</i></p></div>",
            ParserUtil.bb2xhtml("ololo[quote][i]hz[/i][/quote]"));
  }

  @Test
  public void preTest() {
    Assert.assertEquals(
        "<pre>Тег используем мы этот,\n" +
            "Чтобы строки разделять,\n" +
            "Если вдруг стихи захочем\n" +
            "Здесь, на ЛОРе, запощать.\n\n" +
            "Ну а строфы разделяем\n" +
            "Как привыкли уж давно!</pre>"
        , ParserUtil.bb2xhtml(
        "[pre]Тег используем мы этот,\n" +
            "Чтобы строки разделять,\n" +
            "Если вдруг стихи захочем\n" +
            "Здесь, на ЛОРе, запощать.\n\n" +
            "Ну а строфы разделяем\n" +
            "Как привыкли уж давно![/pre]"
        ));

  }

  @Test
  public void spacesTest() {
    Assert.assertEquals("<p>some text</p><p> some again text <a href=\"http://example.com\">example</a> example</p>",
        ParserUtil.bb2xhtml("some text\n\n some again text [url=http://example.com]example[/url] example"));
  }

  @Test
  public void userTest() throws Exception{
    UserDao userDao;
    User maxcom; // Администратор
    User JB;     // Модератор
    User isden;  // Заблокированный пользователь

    maxcom = mock(User.class);
    JB = mock(User.class);
    isden = mock(User.class);

    when(maxcom.isBlocked()).thenReturn(false);
    when(JB.isBlocked()).thenReturn(false);
    when(isden.isBlocked()).thenReturn(true);

    userDao = mock(UserDao.class);
    when(userDao.getUser("maxcom")).thenReturn(maxcom);
    when(userDao.getUser("JB")).thenReturn(JB);
    when(userDao.getUser("isden")).thenReturn(isden);
    when(userDao.getUser("hizel")).thenThrow(new UserNotFoundException("hizel"));

    Assert.assertEquals("<p><span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><a style=\"text-decoration: none\" href='/people/maxcom/profile'>maxcom</a></span></p>",
        ParserUtil.bb2xhtml("[user]maxcom[/user]", true, true, "", userDao));
    Assert.assertEquals("<p><span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><s><a style=\"text-decoration: none\" href='/people/isden/profile'>isden</a></s></span></p>",
        ParserUtil.bb2xhtml("[user]isden[/user]", true, true, "", userDao));
    Assert.assertEquals("<p><s>hizel</s></p>",
        ParserUtil.bb2xhtml("[user]hizel[/user]", true, true, "", userDao));
  }

  @Test
  public void parserResultTest() throws Exception {
    UserDao userDao;
    User maxcom; // Администратор
    User JB;     // Модератор
    User isden;  // Заблокированный пользователь

    maxcom = mock(User.class);
    JB = mock(User.class);
    isden = mock(User.class);

    when(maxcom.isBlocked()).thenReturn(false);
    when(maxcom.getId()).thenReturn(1);
    when(JB.isBlocked()).thenReturn(false);
    when(JB.getId()).thenReturn(2);
    when(isden.isBlocked()).thenReturn(true);
    when(isden.getId()).thenReturn(3);


    userDao = mock(UserDao.class);
    when(userDao.getUser("maxcom")).thenReturn(maxcom);
    when(userDao.getUser("JB")).thenReturn(JB);
    when(userDao.getUser("isden")).thenReturn(isden);
    when(userDao.getUser("hizel")).thenThrow(new UserNotFoundException("hizel"));

    ParserResult parserResult = ParserUtil.bb2xhtm("[user]hizel[/user][user]JB[/user][user]maxcom[/user]", true, true, "", userDao);

    Assert.assertTrue(parserResult.getReplier().contains(maxcom));
    Assert.assertTrue(parserResult.getReplier().contains(JB));
    Assert.assertFalse(parserResult.getReplier().contains(isden));
    Assert.assertEquals("<p><s>hizel</s><span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><a style=\"text-decoration: none\" href='/people/JB/profile'>JB</a></span><span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><a style=\"text-decoration: none\" href='/people/maxcom/profile'>maxcom</a></span></p>", parserResult.getHtml());

  }
}
