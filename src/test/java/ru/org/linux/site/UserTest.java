package ru.org.linux.site;

import org.junit.Assert;
import org.junit.Test;

import java.sql.ResultSet;
import static org.mockito.Mockito.*;

/**
 * Unit Tests для User
 */
public class UserTest {

  /**
   * проверка администратора
   * @throws Exception хм
   */
  @Test
  public void maxcomTest() throws Exception {
    ResultSet resultSet;
    resultSet = mock(ResultSet.class);
    when(resultSet.getInt("id")).thenReturn(1);
    when(resultSet.getString("nick")).thenReturn("maxcom");
    when(resultSet.getBoolean("canmod")).thenReturn(true);
    when(resultSet.getBoolean("candel")).thenReturn(true);
    when(resultSet.getBoolean("corrector")).thenReturn(false);
    when(resultSet.getBoolean("activated")).thenReturn(true);
    when(resultSet.getBoolean("blocked")).thenReturn(false);
    when(resultSet.getInt("score")).thenReturn(600);
    when(resultSet.getInt("max_score")).thenReturn(600);
    when(resultSet.getString("name")).thenReturn("Максим Валянский");
    when(resultSet.getString("passwd")).thenReturn("UEX2F5/8Q5loMT3EQaknMyNbSxtlgain");
    when(resultSet.getString("photo")).thenReturn("1:403073453.png");
    when(resultSet.getString("email")).thenReturn("max@linux.org.ru");
    when(resultSet.getInt("unread_events")).thenReturn(0);

    User user = new User(resultSet);

    Assert.assertEquals(resultSet.getInt("id"), user.getId());
    Assert.assertEquals(resultSet.getString("nick"), user.getNick());
    Assert.assertTrue(user.matchPassword("passwd"));
    try {
      user.checkAnonymous();
    } catch (AccessViolationException e) {
      Assert.fail();
    }
    try {
      user.checkBlocked();
    } catch (AccessViolationException e) {
      Assert.fail();
    }
    try {
      user.checkCommit();
    } catch (AccessViolationException e) {
      Assert.fail();
    }
    Assert.assertFalse(user.isBlocked());
    try {
      user.checkDelete();
    } catch (AccessViolationException e) {
      Assert.fail();
    }
    Assert.assertTrue(user.canModerate());
    Assert.assertTrue(user.isAdministrator());
    Assert.assertFalse(user.canCorrect());
    Assert.assertFalse(user.isAnonymous());
    Assert.assertEquals(resultSet.getInt("score"), user.getScore());
    Assert.assertEquals("<img src=\"/img/normal-star.gif\" width=9 height=9 alt=\"*\">"+
                        "<img src=\"/img/normal-star.gif\" width=9 height=9 alt=\"*\">"+
                        "<img src=\"/img/normal-star.gif\" width=9 height=9 alt=\"*\">"+
                        "<img src=\"/img/normal-star.gif\" width=9 height=9 alt=\"*\">"+
                        "<img src=\"/img/normal-star.gif\" width=9 height=9 alt=\"*\">", user.getStatus());
    Assert.assertFalse(user.isBlockable());
    Assert.assertTrue(user.isActivated());
    Assert.assertFalse(user.isAnonymousScore());
    Assert.assertEquals(resultSet.getBoolean("corrector"), user.isCorrector());
    Assert.assertEquals(resultSet.getString("email"), user.getEmail());
    Assert.assertTrue(user.hasGravatar());
    Assert.assertEquals(resultSet.getString("name"), user.getName());
  }

  /**
   * проверка анонимуса
   * @throws Exception ?
   */
  @Test
  public void anonymousTest() throws Exception {
    ResultSet resultSet;
    resultSet = mock(ResultSet.class);
    when(resultSet.getInt("id")).thenReturn(2);
    when(resultSet.getString("nick")).thenReturn("anonymous");
    when(resultSet.getBoolean("canmod")).thenReturn(false);
    when(resultSet.getBoolean("candel")).thenReturn(false);
    when(resultSet.getBoolean("corrector")).thenReturn(false);
    when(resultSet.getBoolean("activated")).thenReturn(true);
    when(resultSet.getBoolean("blocked")).thenReturn(false);
    when(resultSet.getInt("score")).thenReturn(-117654);
    when(resultSet.getInt("max_score")).thenReturn(4);
    when(resultSet.getString("name")).thenReturn("Anonymous");
    when(resultSet.getString("passwd")).thenReturn(null);
    when(resultSet.getString("photo")).thenReturn(null);
    when(resultSet.getString("email")).thenReturn(null);
    when(resultSet.getInt("unread_events")).thenReturn(161);

    User user = new User(resultSet);

    Assert.assertEquals(resultSet.getInt("id"), user.getId());
    Assert.assertEquals(resultSet.getString("nick"), user.getNick());
    Assert.assertFalse(user.matchPassword("passwd"));
    try {
      user.checkAnonymous();
      Assert.fail();
    } catch (AccessViolationException e) {
      Assert.assertEquals("Anonymous user",e.getMessage());
    }
    try {
      user.checkBlocked();
    } catch (AccessViolationException e) {
      Assert.fail();
    }
    try {
      user.checkCommit();
      Assert.fail();
    } catch (AccessViolationException e) {
      Assert.assertEquals("Commit access denied for anonymous user", e.getMessage());
    }
    Assert.assertFalse(user.isBlocked());
    try {
      user.checkDelete();
      Assert.fail();
    } catch (AccessViolationException e) {
      Assert.assertEquals("Delete access denied for anonymous user", e.getMessage());
    }
    Assert.assertFalse(user.canModerate());
    Assert.assertFalse(user.isAdministrator());
    Assert.assertFalse(user.canCorrect());
    Assert.assertTrue(user.isAnonymous());
    Assert.assertEquals(0, user.getScore());
    Assert.assertEquals("анонимный", user.getStatus());
    Assert.assertFalse(user.isBlockable());
    Assert.assertTrue(user.isActivated());
    Assert.assertTrue(user.isAnonymousScore());
    Assert.assertEquals(resultSet.getBoolean("corrector"), user.isCorrector());
    Assert.assertEquals(resultSet.getString("email"), user.getEmail());
    Assert.assertFalse(user.hasGravatar());
    Assert.assertEquals(resultSet.getString("name"), user.getName());
  }

  /**
   * проверка модератора
   * @throws Exception хм
   */
  @Test
  public void svuTest() throws Exception {
    ResultSet resultSet;
    resultSet = mock(ResultSet.class);
    when(resultSet.getInt("id")).thenReturn(5280);
    when(resultSet.getString("nick")).thenReturn("svu");
    when(resultSet.getBoolean("canmod")).thenReturn(true);
    when(resultSet.getBoolean("candel")).thenReturn(false);
    when(resultSet.getBoolean("corrector")).thenReturn(false);
    when(resultSet.getBoolean("activated")).thenReturn(true);
    when(resultSet.getBoolean("blocked")).thenReturn(false);
    when(resultSet.getInt("score")).thenReturn(500);
    when(resultSet.getInt("max_score")).thenReturn(500);
    when(resultSet.getString("name")).thenReturn("Sergey V. Udaltsov");
    when(resultSet.getString("passwd")).thenReturn("0vwkMky44u8kIqSasrH+X8mHao1a3jOC");
    when(resultSet.getString("photo")).thenReturn("5280.png");
    when(resultSet.getString("email")).thenReturn(null);
    when(resultSet.getInt("unread_events")).thenReturn(2);

    User user = new User(resultSet);

    Assert.assertEquals(resultSet.getInt("id"), user.getId());
    Assert.assertEquals(resultSet.getString("nick"), user.getNick());
    Assert.assertTrue(user.matchPassword("passwd"));
    try {
      user.checkAnonymous();
    } catch (AccessViolationException e) {
      Assert.fail();
    }
    try {
      user.checkBlocked();
    } catch (AccessViolationException e) {
      Assert.fail();
    }
    try {
      user.checkCommit();
    } catch (AccessViolationException e) {
      Assert.fail();
    }
    Assert.assertFalse(user.isBlocked());
    try {
      user.checkDelete();
      Assert.fail();
    } catch (AccessViolationException e) {
      Assert.assertEquals("Delete access denied for user "+
          resultSet.getString("nick") + " (" +
          resultSet.getInt("id") + ") ", e.getMessage());
    }
    Assert.assertTrue(user.canModerate());
    Assert.assertFalse(user.isAdministrator());
    Assert.assertFalse(user.canCorrect());
    Assert.assertFalse(user.isAnonymous());
    Assert.assertEquals(resultSet.getInt("score"), user.getScore());
    Assert.assertEquals("<img src=\"/img/normal-star.gif\" width=9 height=9 alt=\"*\">"+
                        "<img src=\"/img/normal-star.gif\" width=9 height=9 alt=\"*\">"+
                        "<img src=\"/img/normal-star.gif\" width=9 height=9 alt=\"*\">"+
                        "<img src=\"/img/normal-star.gif\" width=9 height=9 alt=\"*\">"+
                        "<img src=\"/img/normal-star.gif\" width=9 height=9 alt=\"*\">", user.getStatus());
    Assert.assertFalse(user.isBlockable());
    Assert.assertTrue(user.isActivated());
    Assert.assertFalse(user.isAnonymousScore());
    Assert.assertEquals(resultSet.getBoolean("corrector"), user.isCorrector());
    Assert.assertEquals(resultSet.getString("email"), user.getEmail());
    Assert.assertFalse(user.hasGravatar());
    Assert.assertEquals(resultSet.getString("name"), user.getName());
  }

  /**
   * проверка для пользователя с 5-ю звездами
   * @throws Exception хм
   */
  @Test
  public void user5starTest() throws Exception {
    ResultSet resultSet;
    resultSet = mock(ResultSet.class);
    when(resultSet.getInt("id")).thenReturn(13);
    when(resultSet.getString("nick")).thenReturn("user5star");
    when(resultSet.getBoolean("canmod")).thenReturn(false);
    when(resultSet.getBoolean("candel")).thenReturn(false);
    when(resultSet.getBoolean("corrector")).thenReturn(false);
    when(resultSet.getBoolean("activated")).thenReturn(true);
    when(resultSet.getBoolean("blocked")).thenReturn(false);
    when(resultSet.getInt("score")).thenReturn(500);
    when(resultSet.getInt("max_score")).thenReturn(500);
    when(resultSet.getString("name")).thenReturn("5 star");
    when(resultSet.getString("passwd")).thenReturn("S+Q/c5dtkvNxO42uEcQBdP8r32zOfdUq");
    when(resultSet.getString("photo")).thenReturn(null);
    when(resultSet.getString("email")).thenReturn(null);
    when(resultSet.getInt("unread_events")).thenReturn(13);

    User user = new User(resultSet);

    Assert.assertEquals(resultSet.getInt("id"), user.getId());
    Assert.assertEquals(resultSet.getString("nick"), user.getNick());
    Assert.assertTrue(user.matchPassword("passwd"));
    try {
      user.checkAnonymous();
    } catch (AccessViolationException e) {
      Assert.fail();
    }
    try {
      user.checkBlocked();
    } catch (AccessViolationException e) {
      Assert.fail();
    }
    try {
      user.checkCommit();
      Assert.fail();
    } catch (AccessViolationException e) {
      Assert.assertEquals("Commit access denied for user "+
          resultSet.getString("nick") + " (" +
          resultSet.getInt("id") + ") ", e.getMessage());
    }
    Assert.assertFalse(user.isBlocked());
    try {
      user.checkDelete();
      Assert.fail();
    } catch (AccessViolationException e) {
      Assert.assertEquals("Delete access denied for user "+
          resultSet.getString("nick") + " (" +
          resultSet.getInt("id") + ") ", e.getMessage());
    }
    Assert.assertFalse(user.canModerate());
    Assert.assertFalse(user.isAdministrator());
    Assert.assertFalse(user.canCorrect());
    Assert.assertFalse(user.isAnonymous());
    Assert.assertEquals(resultSet.getInt("score"), user.getScore());
    Assert.assertEquals("<img src=\"/img/normal-star.gif\" width=9 height=9 alt=\"*\">"+
                        "<img src=\"/img/normal-star.gif\" width=9 height=9 alt=\"*\">"+
                        "<img src=\"/img/normal-star.gif\" width=9 height=9 alt=\"*\">"+
                        "<img src=\"/img/normal-star.gif\" width=9 height=9 alt=\"*\">"+
                        "<img src=\"/img/normal-star.gif\" width=9 height=9 alt=\"*\">", user.getStatus());
    Assert.assertFalse(user.isBlockable());
    Assert.assertTrue(user.isActivated());
    Assert.assertFalse(user.isAnonymousScore());
    Assert.assertEquals(resultSet.getBoolean("corrector"), user.isCorrector());
    Assert.assertEquals(resultSet.getString("email"), user.getEmail());
    Assert.assertFalse(user.hasGravatar());
    Assert.assertEquals(resultSet.getString("name"), user.getName());
  }

}
