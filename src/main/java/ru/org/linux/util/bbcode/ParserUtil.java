package ru.org.linux.util.bbcode;

import java.sql.Connection;

/**
 * Created by IntelliJ IDEA.
 * User: hizel
 * Date: 6/30/11
 * Time: 9:51 PM
 */
public class ParserUtil {

    private static final Parser parser = new Parser();

    public static String bb2xhtml(String bbcode, Connection db){
        return parser.parse(bbcode).renderXHtml(db);
    }

    public static String bb2xhtml(String bbcode, boolean renderCut, String cutUrl, Connection db){
        return parser.parse(bbcode, renderCut, cutUrl).renderXHtml(db);
    }

    public static String correct(String bbcode){
        return parser.parse(bbcode).renderBBCode();
    }
}
