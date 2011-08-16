package ru.org.linux.util.bbcode;

import ru.org.linux.util.bbcode.nodes.RootNode;

import java.sql.Connection;
import java.util.EnumSet;

/**
 * Created by IntelliJ IDEA.
 * User: hizel
 * Date: 6/30/11
 * Time: 9:51 PM
 */
public class ParserUtil {
  private static final Parser parserWithOutImages = new Parser(EnumSet.noneOf(Parser.ParserFlags.class));

  public static String bb2xhtml(String bbcode) {
    return parserWithOutImages.parse(new RootNode(parserWithOutImages), bbcode).renderXHtml();
  }

  public static String bb2xhtml(String bbcode, boolean renderCut, boolean cleanCut, String cutUrl, Connection db) {
    RootNode rootNode = new RootNode(parserWithOutImages);
    rootNode.setRenderOptions(renderCut, cleanCut, cutUrl);
    rootNode.setConnection(db);
    return parserWithOutImages.parse(rootNode, bbcode).renderXHtml();
  }
}
