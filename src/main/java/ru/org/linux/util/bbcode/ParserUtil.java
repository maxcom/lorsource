package ru.org.linux.util.bbcode;

import org.apache.activemq.transport.stomp.Stomp;

import java.sql.Connection;

/**
 * Created by IntelliJ IDEA.
 * User: hizel
 * Date: 6/30/11
 * Time: 9:51 PM
 */
public class ParserUtil {

    private static final Parser parser = new Parser();

    public static String bb2xhtml(String bbcode){
        return parser.parse(bbcode).renderXHtml();
    }

    public static String bb2xhtml(String bbcode, boolean renderCut, String cutUrl){
        return parser.parse(bbcode, renderCut, cutUrl).renderXHtml();
    }

    public static String correct(String bbcode){
        return parser.parse(bbcode).renderBBCode();
    }
}
