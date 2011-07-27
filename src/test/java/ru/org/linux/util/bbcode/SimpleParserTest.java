package ru.org.linux.util.bbcode;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: hizel
 * Date: 7/5/11
 * Time: 5:06 PM
 */
public class SimpleParserTest {

    @Test
    public void brTest(){
        Assert.assertEquals(ParserUtil.bb2xhtml("[br]",null), "<p><br></p>");
    }

    @Test
    public void boldTest(){
        Assert.assertEquals(ParserUtil.bb2xhtml("[b]hello world[/b]",null), "<p><b>hello world</b></p>");
    }

    @Test
    public void italicTest(){
        Assert.assertEquals(ParserUtil.bb2xhtml("[i]hello world[/i]",null), "<p><i>hello world</i></p>");
    }

    @Test
    public void strikeoutTest(){
        Assert.assertEquals(ParserUtil.bb2xhtml("[s]hello world[/s]",null), "<p><s>hello world</s></p>");
    }

    @Test
    public void emphasisTest(){
        Assert.assertEquals(ParserUtil.bb2xhtml("[strong]hello world[/strong]",null), "<p><strong>hello world</strong></p>");
    }

    @Test
    public void quoteTest(){
        // TODO я нрипонял зачем <div> :-(
        Assert.assertEquals(ParserUtil.bb2xhtml("[quote]hello world[/quote]",null), "<div class=\"quote\"><p>hello world</p></div>");
    }
    @Test
    public void quoteParamTest(){
        // TODO я нрипонял зачем <div> :-(
        Assert.assertEquals(ParserUtil.bb2xhtml("[quote=maxcom]hello world[/quote]",null), "<div class=\"quote\"><h3>maxcom</h3><p>hello world</p></div>");
    }
    @Test
    public void quoteCleanTest(){
        Assert.assertEquals("", ParserUtil.bb2xhtml("[quote][/quote]", null));
    }
    @Test
    public void urlTest(){
        Assert.assertEquals(ParserUtil.bb2xhtml("[url]http://linux.org.ru[/url]",null), "<p><a href=\"http://linux.org.ru\">http://linux.org.ru</a></p>");
    }
    @Test
    public void urlParamTest(){
        Assert.assertEquals(ParserUtil.bb2xhtml("[url=http://linux.org.ru]linux[/url]",null), "<p><a href=\"http://linux.org.ru\">linux</a></p>");
    }
    @Test
    public void urlParamWithTagTest(){
        Assert.assertEquals(ParserUtil.bb2xhtml("[url=http://linux.org.ru][b]l[/b]inux[/url]",null), "<p><a href=\"http://linux.org.ru\"><b>l</b>inux</a></p>");
    }
    @Test
    public void listTest(){
        Assert.assertEquals(ParserUtil.bb2xhtml("[list][*]one[*]two[*]three[/list]",null), "<ul><li>one</li><li>two</li><li>three</li></ul>");
    }
    @Test
    public void codeTest(){
        Assert.assertEquals(ParserUtil.bb2xhtml("[code][list][*]one[*]two[*]three[/list][/code]",null), "<div class=\"code\"><pre class=\"no-highlight\"><code>[list][*]one[*]two[*]three[/list]</code></pre></div>");
    }
    @Test
    public void codeCleanTest(){
        Assert.assertEquals("", ParserUtil.bb2xhtml("[code][/code]", null));
    }
    @Test
    public void codeKnowTest(){
        Assert.assertEquals("<div class=\"code\"><pre class=\"language-cpp\"><code>#include &lt;stdio.h&gt;</code></pre></div>", ParserUtil.bb2xhtml("[code=cxx]#include <stdio.h>[/code]", null));
    }
    @Test
    public void codeUnKnowTest(){
        Assert.assertEquals("<div class=\"code\"><pre class=\"no-highlight\"><code>#include &lt;stdio.h&gt;</code></pre></div>", ParserUtil.bb2xhtml("[code=foo]#include <stdio.h>[/code]", null));
    }
    @Test
    public void overflow1Test(){
        Assert.assertEquals("<p>ololo<div class=\"quote\"><p><i>hz</i></p></div></p>", ParserUtil.bb2xhtml("ololo[quote][i]hz[/i][/quote]", null));
    }
// TODO а как тестировать если базы нет :-(
//    @Test
//    public void userTest(){
//        Assert.assertEquals(ParserUtil.bb2xhtml("[user]maxcom[/user]",null), "<div><span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><a style=\"text-decoration: none\" href='/people/maxcom/profile'>maxcom</a></span></div>");
//    }
}
