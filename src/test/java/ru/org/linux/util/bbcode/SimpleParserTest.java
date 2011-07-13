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
        Assert.assertEquals(ParserUtil.bb2xhtml("[br]",null), "<br/>");
    }

    @Test
    public void boldTest(){
        Assert.assertEquals(ParserUtil.bb2xhtml("[b]hello world[/b]",null), "<b>hello world</b>");
    }

    @Test
    public void italicTest(){
        Assert.assertEquals(ParserUtil.bb2xhtml("[i]hello world[/i]",null), "<i>hello world</i>");
    }

    @Test
    public void strikeoutTest(){
        Assert.assertEquals(ParserUtil.bb2xhtml("[s]hello world[/s]",null), "<s>hello world</s>");
    }

    @Test
    public void emphasisTest(){
        Assert.assertEquals(ParserUtil.bb2xhtml("[strong]hello world[/strong]",null), "<strong>hello world</strong>");
    }

    @Test
    public void quoteTest(){
        // TODO я нрипонял зачем <div> :-(
        Assert.assertEquals(ParserUtil.bb2xhtml("[quote]hello world[/quote]",null), "<div class=\"quote\">hello world</div>");
    }
    @Test
    public void quoteParamTest(){
        // TODO я нрипонял зачем <div> :-(
        Assert.assertEquals(ParserUtil.bb2xhtml("[quote=maxcom]hello world[/quote]",null), "<div class=\"quote\"><h3>maxcom</h3>hello world</div>");
    }
    @Test
    public void urlTest(){
        Assert.assertEquals(ParserUtil.bb2xhtml("[url]http://linux.org.ru[/url]",null), "<a href=\"http://linux.org.ru\">http://linux.org.ru</a>");
    }
    @Test
    public void urlParamTest(){
        Assert.assertEquals(ParserUtil.bb2xhtml("[url=http://linux.org.ru]linux[/url]",null), "<a href=\"http://linux.org.ru\">linux</a>");
    }
    @Test
    public void listTest(){
        Assert.assertEquals(ParserUtil.bb2xhtml("[list][*]one[*]two[*]three[/list]",null), "<ul><li>one</li><li>two</li><li>three</li></ul>");
    }
    @Test
    public void codeTest(){
        Assert.assertEquals(ParserUtil.bb2xhtml("[code][list][*]one[*]two[*]three[/list][/code]",null), "<div class=\"code\"><pre class=\"no-highlight\"><code>[list][*]one[*]two[*]three[/list]</code></pre></div>");
    }
// TODO а как тестировать если базы нет :-(    
//    @Test
//    public void userTest(){
//        Assert.assertEquals(ParserUtil.bb2xhtml("[user]maxcom[/user]",null), "<div><span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><a style=\"text-decoration: none\" href='/people/maxcom/profile'>maxcom</a></span></div>");
//    }
}
