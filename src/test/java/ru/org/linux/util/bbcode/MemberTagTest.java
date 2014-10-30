package ru.org.linux.util.bbcode;

import org.apache.commons.httpclient.URI;
import org.junit.Before;
import org.junit.Test;
import ru.org.linux.spring.SiteConfig;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserNotFoundException;
import ru.org.linux.util.formatter.ToHtmlFormatter;

import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MemberTagTest {
  private User maxcom; // Администратор
  private User JB;     // Модератор
  private User isden;  // Заблокированный пользователь
  private LorCodeService lorCodeService;

  @Before
  public void initTest() throws Exception {
    UserDao userDao = mock(UserDao.class);
    User splinter = mock(User.class);

    maxcom = mock(User.class);
    JB = mock(User.class);
    isden = mock(User.class);

    when(maxcom.isBlocked()).thenReturn(false);
    when(JB.isBlocked()).thenReturn(false);
    when(isden.isBlocked()).thenReturn(true);
    when(maxcom.getNick()).thenReturn("maxcom");
    when(JB.getNick()).thenReturn("JB");
    when(isden.getNick()).thenReturn("isden");

    when(splinter.isBlocked()).thenReturn(false);
    when(splinter.getNick()).thenReturn("splinter");
    when(userDao.getUser("splinter")).thenReturn(splinter);

    when(userDao.getUser("maxcom")).thenReturn(maxcom);
    when(userDao.getUser("JB")).thenReturn(JB);
    when(userDao.getUser("isden")).thenReturn(isden);
    when(userDao.getUser("hizel")).thenThrow(new UserNotFoundException("hizel"));

    String mainUrl = "http://127.0.0.1:8080/";
    URI mainURI = new URI(mainUrl, true, "UTF-8");

    SiteConfig siteConfig = mock(SiteConfig.class);
    when(siteConfig.getMainURI()).thenReturn(mainURI);
    when(siteConfig.getMainUrl()).thenReturn(mainUrl);


    ToHtmlFormatter toHtmlFormatter = new ToHtmlFormatter();
    toHtmlFormatter.setSiteConfig(siteConfig);

    lorCodeService = new LorCodeService();
    lorCodeService.setUserDao(userDao);
    lorCodeService.setSiteConfig(siteConfig);
    lorCodeService.setToHtmlFormatter(toHtmlFormatter);
  }

  @Test
  public void testExtraLines() {
    //user
    assertEquals(lorCodeService.parseComment("[user]splinter[/user]", false, false),
            lorCodeService.parseComment("\n\n[user]\n\nsplinter\n\n[/user]\n\n", false, false));
  }

  @Test
  public void splinterTest1() { // http://www.linux.org.ru/forum/linux-org-ru/6448266
    assertEquals("<p><a href=\"http://www.fishing.org/\">http://www.fishing.org/</a> <span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><a style=\"text-decoration: none\" href=\"http://127.0.0.1:8080/people/splinter/profile\">splinter</a></span></p>",
            lorCodeService.parseComment("[url=http://www.fishing.org/][user]splinter[/user][/url]", false, false));
  }

  @Test
  public void userTest() {
    assertEquals("<p> <span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><a style=\"text-decoration: none\" href=\"http://127.0.0.1:8080/people/maxcom/profile\">maxcom</a></span></p>",
            lorCodeService.parseComment("[user]maxcom[/user]", false, false));
    assertEquals("<p> <span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><s><a style=\"text-decoration: none\" href=\"http://127.0.0.1:8080/people/isden/profile\">isden</a></s></span></p>",
            lorCodeService.parseComment("[user]isden[/user]", false, false));
    assertEquals("<p> <s>hizel</s></p>",
            lorCodeService.parseComment("[user]hizel[/user]", false, false));
  }

  @Test
  public void parserResultTest() {
    String msg = "[user]hizel[/user][user]JB[/user][user]maxcom[/user]";
    Set<User> replier = lorCodeService.getReplierFromMessage(msg);

    assertTrue(replier.contains(maxcom));
    assertTrue(replier.contains(JB));
    assertFalse(replier.contains(isden));
  }

  @Test
  public void userTest2() {
    assertEquals("<p> <span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><a style=\"text-decoration: none\" href=\"http://127.0.0.1:8080/people/maxcom/profile\">maxcom</a></span></p>",
            lorCodeService.parseComment("[user]maxcom[/USER]", false, false));
    assertEquals("<p> <span style=\"white-space: nowrap\"><img src=\"/img/tuxlor.png\"><s><a style=\"text-decoration: none\" href=\"http://127.0.0.1:8080/people/isden/profile\">isden</a></s></span></p>",
            lorCodeService.parseComment("[USER]isden[/USER]", false, false));
    assertEquals("<p> <s>hizel</s></p>",
            lorCodeService.parseComment("[user]hizel[/USER]", false, false));
  }

  @Test
  public void parserResultTest2() {
    String msg = "[user]hizel[/user][USER]JB[/user][user]maxcom[/USER]";
    Set<User> replier = lorCodeService.getReplierFromMessage(msg);

    assertTrue(replier.contains(maxcom));
    assertTrue(replier.contains(JB));
    assertFalse(replier.contains(isden));
  }
}
