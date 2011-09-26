package ru.org.linux.util.bbcode;

import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import ru.org.linux.site.User;
import ru.org.linux.spring.dao.UserDao;
import ru.org.linux.util.bbcode.nodes.RootNode;

import java.sql.Connection;
import java.util.Set;

/**
 * Вспомогательный класс, для разбора LORCODE
 */
public class ParserUtil {
  private static final Parser defaultParser = new Parser(new DefaultParserParameters());

  /**
   * Функция разбора LORCODE в HTML с параметрами по умолчанию
   * @param bbcode обрабатываемый LORCODE
   * @return результирующий HTML
   */
  public static String bb2xhtml(String bbcode) {
    return defaultParser.parse(bbcode).renderXHtml();
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
    UserDao singleUserDao = new UserDao();
    singleUserDao.setJdbcTemplate(new SingleConnectionDataSource(db, true));
    return bb2xhtml(bbcode, renderCut, cleanCut, cutUrl, singleUserDao);
  }

  public static String bb2xhtml(String bbcode, boolean renderCut, boolean cleanCut, String cutUrl, UserDao userDao) {
    return defaultParser.parse(bbcode, renderCut, cleanCut, cutUrl, userDao).renderXHtml();
  }
  
  public static String bb2xhtml(String bbcode, boolean renderCut, boolean cleanCut, String cutUrl) {
    return defaultParser.parse(bbcode, renderCut, cleanCut, cutUrl, null).renderXHtml();
  }

  public static ParserResult bb2xhtm(String bbcode, boolean renderCut, boolean cleanCut, String cutUrl, UserDao userDao) {
    RootNode rootNode = defaultParser.parse(bbcode, renderCut, cleanCut, cutUrl, userDao);
    String html = rootNode.renderXHtml();
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
