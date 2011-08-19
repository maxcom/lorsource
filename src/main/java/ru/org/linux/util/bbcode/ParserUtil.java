package ru.org.linux.util.bbcode;

import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import ru.org.linux.spring.dao.UserDao;
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

  @Deprecated
  public static String bb2xhtml(String bbcode, boolean renderCut, boolean cleanCut, String cutUrl, Connection db) {
    RootNode rootNode = new RootNode(parserWithOutImages);
    rootNode.setRenderOptions(renderCut, cleanCut, cutUrl);
    rootNode.setUserDao(new UserDao(new SingleConnectionDataSource(db, true)));
    return parserWithOutImages.parse(rootNode, bbcode).renderXHtml();
  }

  public static String bb2xhtml(String bbcode, boolean renderCut, boolean cleanCut, String cutUrl, UserDao userDao) {
    RootNode rootNode = new RootNode(parserWithOutImages);
    rootNode.setRenderOptions(renderCut, cleanCut, cutUrl);
    rootNode.setUserDao(userDao);
    return parserWithOutImages.parse(rootNode, bbcode).renderXHtml();
  }
  
  public static String bb2xhtml(String bbcode, boolean renderCut, boolean cleanCut, String cutUrl) {
    RootNode rootNode = new RootNode(parserWithOutImages);
    rootNode.setRenderOptions(renderCut, cleanCut, cutUrl);
    return parserWithOutImages.parse(rootNode, bbcode).renderXHtml();
  }
}
