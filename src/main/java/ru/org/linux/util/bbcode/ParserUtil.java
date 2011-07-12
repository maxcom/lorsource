package ru.org.linux.util.bbcode;

/**
 * Created by IntelliJ IDEA.
 * User: hizel
 * Date: 6/30/11
 * Time: 9:51 PM
 */
public class ParserUtil {

    private static final Parser parser = new Parser();

    public static String bb2xhtml(String bbcode, boolean rootAllowsInline){
        return parser.parse(bbcode).renderXHtml();
    }

    public static String bb2xhtml(String bbcode, boolean rootAllowsInline, boolean renderCut, String cutUrl){
        return parser.parse(bbcode, renderCut, cutUrl).renderXHtml();
    }

    public static String correct(String bbcode, boolean rootAllowsInline){
        return parser.parse(bbcode).renderBBCode();
    }

    public static String to_html(String text){
        return bb2xhtml(text, false);
    }

}
