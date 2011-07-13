package ru.org.linux.util.bbcode;

import java.sql.Connection;
import java.util.EnumSet;

/**
 * Created by IntelliJ IDEA.
 * User: hizel
 * Date: 6/30/11
 * Time: 9:51 PM
 */
public class ParserUtil {

    private static final Parser parserWithImages = new Parser(EnumSet.of(Parser.ParserFlags.ENABLE_IMG_TAG));

    public static String bb2xhtml(String bbcode, Connection db){
        return parserWithImages.parse(bbcode).renderXHtml(db);
    }

    public static String bb2xhtml(String bbcode, boolean renderCut, String cutUrl, Connection db){
        return parserWithImages.parse(bbcode, renderCut, cutUrl).renderXHtml(db);
    }
}
