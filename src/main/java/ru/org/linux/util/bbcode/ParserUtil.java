package ru.org.linux.util.bbcode;

import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import ru.org.linux.site.User;
import ru.org.linux.spring.dao.UserDao;
import ru.org.linux.util.bbcode.nodes.RootNode;

import java.sql.Connection;
import java.util.EnumSet;
import java.util.Set;

/**
 * Вспомогательный класс, для разбора LORCODE
 */
public class ParserUtil {
  private static final Parser parserWithOutImages = new Parser(EnumSet.noneOf(Parser.ParserFlags.class));

  /**
   * Функция разбора LORCODE в HTML с параметрами по умолчанию
   * @param bbcode обрабатываемый LORCODE
   * @return результирующий HTML
   */
  public static String bb2xhtml(String bbcode) {
    return parserWithOutImages.parse(new RootNode(parserWithOutImages), bbcode).renderXHtml();
  }

  /**
   * Функция разбора LORCODE в HTML с дополнительными параметрами для обработки тэга cut и user
   * @param bbcode обрабатываемый LORCODE
   * @param renderCut признак, показывать\не показывать содержимое тэга cut
   * @param cleanCut признак, оборачивать ли содержимое cut в div с id
   * @param cutUrl url для cut, указывает на текущий топик
   * @param db интерфейс через который мы высосем информацию для каждого встреченного тэга user
   * @return результирующий HTML
   */
  @Deprecated
  public static String bb2xhtml(String bbcode, boolean renderCut, boolean cleanCut, String cutUrl, Connection db) {
    RootNode rootNode = new RootNode(parserWithOutImages);
    rootNode.setRenderOptions(renderCut, cleanCut, cutUrl);
    UserDao singleUserDao = new UserDao();
    singleUserDao.setJdbcTemplate(new SingleConnectionDataSource(db, true));
    rootNode.setUserDao(singleUserDao);
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

  public static ParserResult bb2xhtm(String bbcode, boolean renderCut, boolean cleanCut, String cutUrl, UserDao userDao) {
    RootNode rootNode = new RootNode(parserWithOutImages);
    rootNode.setRenderOptions(renderCut, cleanCut, cutUrl);
    rootNode.setUserDao(userDao);
    String html = parserWithOutImages.parse(rootNode, bbcode).renderXHtml();
    Set<User> replier = rootNode.getReplier();
    return new ParserResult(html, replier);
  }

  @Deprecated
  public static ParserResult bb2xhtm(String bbcode, boolean renderCut, boolean cleanCut, String cutUrl, Connection db) {
    UserDao singleUserDao = new UserDao();
    singleUserDao.setJdbcTemplate(new SingleConnectionDataSource(db, true));
    return bb2xhtm(bbcode, renderCut,cleanCut,cutUrl,singleUserDao);
  }
}
