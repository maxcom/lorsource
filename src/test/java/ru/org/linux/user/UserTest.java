/*
 * Copyright 1998-2012 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.user;

import org.junit.Assert;
import org.junit.Test;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.test.Users;

import java.sql.ResultSet;

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
    ResultSet resultSet = Users.getMaxcom();
    User user = new User(resultSet);

    Assert.assertEquals(resultSet.getInt("id"), user.getId());
    Assert.assertEquals(resultSet.getString("nick"), user.getNick());
    Assert.assertEquals("tango", resultSet.getString("style"));
    Assert.assertTrue(user.matchPassword("passwd"));
    try {
      user.checkBlocked();
    } catch (AccessViolationException e) {
      Assert.fail();
    }
    try {
      user.checkFrozen();
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
    Assert.assertTrue(user.isModerator());
    Assert.assertTrue(user.isAdministrator());
    Assert.assertFalse(user.canCorrect());
    Assert.assertFalse(user.isAnonymous());
    Assert.assertEquals(resultSet.getInt("score"), user.getScore());
    Assert.assertEquals("<span class=\"stars\">★★★★★</span>", user.getStatus()); 
    Assert.assertTrue(user.isActivated());
    Assert.assertFalse(user.isAnonymousScore());
    Assert.assertEquals(resultSet.getBoolean("corrector"), user.isCorrector());
    Assert.assertEquals(resultSet.getString("email"), user.getEmail());
    Assert.assertTrue(user.hasEmail());
    Assert.assertEquals(resultSet.getString("name"), user.getName());
    Assert.assertFalse(user.isFrozen());
  }

  /**
   * проверка анонимуса
   * @throws Exception ?
   */
  @Test
  public void anonymousTest() throws Exception {
    ResultSet resultSet = Users.getAnonymous();
    User user = new User(resultSet);

    Assert.assertEquals(resultSet.getInt("id"), user.getId());
    Assert.assertEquals(resultSet.getString("nick"), user.getNick());
    Assert.assertEquals("tango", resultSet.getString("style"));
    Assert.assertFalse(user.matchPassword("passwd"));
    try {
      user.checkBlocked();
    } catch (AccessViolationException e) {
      Assert.fail();
    }
    try {
      user.checkFrozen();
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
    Assert.assertFalse(user.isModerator());
    Assert.assertFalse(user.isAdministrator());
    Assert.assertFalse(user.canCorrect());
    Assert.assertTrue(user.isAnonymous());
    Assert.assertEquals(0, user.getScore());
    Assert.assertEquals("анонимный", user.getStatus());
    Assert.assertTrue(user.isActivated());
    Assert.assertTrue(user.isAnonymousScore());
    Assert.assertEquals(resultSet.getBoolean("corrector"), user.isCorrector());
    Assert.assertEquals(resultSet.getString("email"), user.getEmail());
    Assert.assertFalse(user.hasEmail());
    Assert.assertEquals(resultSet.getString("name"), user.getName());
    Assert.assertFalse(user.isFrozen());
  }

  /**
   * проверка модератора
   * @throws Exception хм
   */
  @Test
  public void svuTest() throws Exception {
    ResultSet resultSet = Users.getModerator();
    User user = new User(resultSet);

    Assert.assertEquals(resultSet.getInt("id"), user.getId());
    Assert.assertEquals(resultSet.getString("nick"), user.getNick());
    Assert.assertEquals("tango", resultSet.getString("style"));
    Assert.assertTrue(user.matchPassword("passwd"));
    try {
      user.checkBlocked();
    } catch (AccessViolationException e) {
      Assert.fail();
    }
    try {
      user.checkFrozen();
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
    Assert.assertTrue(user.isModerator());
    Assert.assertFalse(user.isAdministrator());
    Assert.assertFalse(user.canCorrect());
    Assert.assertFalse(user.isAnonymous());
    Assert.assertEquals(resultSet.getInt("score"), user.getScore());
    Assert.assertEquals("<span class=\"stars\">★★★★★</span>", user.getStatus()); 
    Assert.assertTrue(user.isActivated());
    Assert.assertFalse(user.isAnonymousScore());
    Assert.assertEquals(resultSet.getBoolean("corrector"), user.isCorrector());
    Assert.assertEquals(resultSet.getString("email"), user.getEmail());
    Assert.assertFalse(user.hasEmail());
    Assert.assertEquals(resultSet.getString("name"), user.getName());
    Assert.assertFalse(user.isFrozen());
  }

  /**
   * проверка для пользователя с 5-ю звездами
   * @throws Exception хм
   */
  @Test
  public void user5starTest() throws Exception {
    ResultSet resultSet = Users.getUser5star();
    User user = new User(resultSet);

    Assert.assertEquals(resultSet.getInt("id"), user.getId());
    Assert.assertEquals(resultSet.getString("nick"), user.getNick());
    Assert.assertEquals("tango", resultSet.getString("style"));
    Assert.assertTrue(user.matchPassword("passwd"));
    try {
      user.checkBlocked();
    } catch (AccessViolationException e) {
      Assert.fail();
    }
    try {
      user.checkFrozen();
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
    Assert.assertFalse(user.isModerator());
    Assert.assertFalse(user.isAdministrator());
    Assert.assertFalse(user.canCorrect());
    Assert.assertFalse(user.isAnonymous());
    Assert.assertEquals(resultSet.getInt("score"), user.getScore());
    Assert.assertEquals("<span class=\"stars\">★★★★★</span>", user.getStatus()); 
    Assert.assertTrue(user.isActivated());
    Assert.assertFalse(user.isAnonymousScore());
    Assert.assertEquals(resultSet.getBoolean("corrector"), user.isCorrector());
    Assert.assertEquals(resultSet.getString("email"), user.getEmail());
    Assert.assertFalse(user.hasEmail());
    Assert.assertEquals(resultSet.getString("name"), user.getName());
    Assert.assertFalse(user.isFrozen());
  }

  /**
   * проверка для пользователя с 1-ой звездами
   * @throws Exception хм
   */
  @Test
  public void user1starTest() throws Exception {
    ResultSet resultSet = Users.getUser1star();
    User user = new User(resultSet);

    Assert.assertEquals(resultSet.getInt("id"), user.getId());
    Assert.assertEquals(resultSet.getString("nick"), user.getNick());
    Assert.assertEquals("tango", resultSet.getString("style"));
    Assert.assertTrue(user.matchPassword("passwd"));
    try {
      user.checkBlocked();
    } catch (AccessViolationException e) {
      Assert.fail();
    }
    try {
      user.checkFrozen();
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
    Assert.assertFalse(user.isModerator());
    Assert.assertFalse(user.isAdministrator());
    Assert.assertFalse(user.canCorrect());
    Assert.assertFalse(user.isAnonymous());
    Assert.assertEquals(resultSet.getInt("score"), user.getScore());
    Assert.assertEquals("<span class=\"stars\">★</span>", user.getStatus()); 
    Assert.assertTrue(user.isActivated());
    Assert.assertFalse(user.isAnonymousScore());
    Assert.assertEquals(resultSet.getBoolean("corrector"), user.isCorrector());
    Assert.assertEquals(resultSet.getString("email"), user.getEmail());
    Assert.assertFalse(user.hasEmail());
    Assert.assertEquals(resultSet.getString("name"), user.getName());
    Assert.assertFalse(user.isFrozen());
  }

  /**
   * проверка для пользователя с < 50 score
   * @throws Exception хм
   */
  @Test
  public void user45scoreTest() throws Exception {
    ResultSet resultSet = Users.getUser45Score();
    User user = new User(resultSet);

    Assert.assertEquals(resultSet.getInt("id"), user.getId());
    Assert.assertEquals(resultSet.getString("nick"), user.getNick());
    Assert.assertEquals("tango", resultSet.getString("style"));
    Assert.assertTrue(user.matchPassword("passwd"));
    try {
      user.checkBlocked();
    } catch (AccessViolationException e) {
      Assert.fail();
    }
    try {
      user.checkFrozen();
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
    Assert.assertFalse(user.isModerator());
    Assert.assertFalse(user.isAdministrator());
    Assert.assertFalse(user.canCorrect());
    Assert.assertFalse(user.isAnonymous());
    Assert.assertEquals(resultSet.getInt("score"), user.getScore());
    Assert.assertEquals("анонимный", user.getStatus());
    Assert.assertTrue(user.isActivated());
    Assert.assertTrue(user.isAnonymousScore());
    Assert.assertEquals(resultSet.getBoolean("corrector"), user.isCorrector());
    Assert.assertEquals(resultSet.getString("email"), user.getEmail());
    Assert.assertFalse(user.hasEmail());
    Assert.assertEquals(resultSet.getString("name"), user.getName());
    Assert.assertFalse(user.isFrozen());
  }

  /**
   * проверка для заблокированного пользователя с < 50 score
   * @throws Exception хм
   */
  @Test
  public void userBlockedTest() throws Exception {
    ResultSet resultSet = Users.getUser45ScoreBlocked();
    User user = new User(resultSet);

    Assert.assertEquals(resultSet.getInt("id"), user.getId());
    Assert.assertEquals(resultSet.getString("nick"), user.getNick());
    Assert.assertEquals("tango", resultSet.getString("style"));
    Assert.assertTrue(user.matchPassword("passwd"));
    try {
      user.checkBlocked();
      Assert.fail();
    } catch (AccessViolationException e) {
      Assert.assertEquals("Пользователь заблокирован", e.getMessage());
    }
    try {
      user.checkFrozen();
    } catch (AccessViolationException e) {
      Assert.fail();
    }
    try {
      user.checkCommit();
      Assert.fail();
    } catch (AccessViolationException e) {
      Assert.assertEquals("Commit access denied for anonymous user", e.getMessage());
    }
    Assert.assertTrue(user.isBlocked());
    try {
      user.checkDelete();
      Assert.fail();
    } catch (AccessViolationException e) {
      Assert.assertEquals("Delete access denied for anonymous user", e.getMessage());
    }
    Assert.assertFalse(user.isModerator());
    Assert.assertFalse(user.isAdministrator());
    Assert.assertFalse(user.canCorrect());
    Assert.assertFalse(user.isAnonymous());  // TODO для заблокированного ананомного пользователя False :-\
    Assert.assertEquals(resultSet.getInt("score"), user.getScore());
    Assert.assertEquals("анонимный", user.getStatus());
    Assert.assertTrue(user.isActivated());
    Assert.assertTrue(user.isAnonymousScore());
    Assert.assertEquals(resultSet.getBoolean("corrector"), user.isCorrector());
    Assert.assertEquals(resultSet.getString("email"), user.getEmail());
    Assert.assertFalse(user.hasEmail());
    Assert.assertEquals(resultSet.getString("name"), user.getName());
    Assert.assertFalse(user.isFrozen());
  }

  /**
   * проверка размороженного пользователя, или оттаевшего
   * @throws Exception хм
   */
  @Test
  public void userDefrosedTest() throws Exception {
    ResultSet resultSet = Users.getUserDefrosted();
    User user = new User(resultSet);

    Assert.assertEquals(resultSet.getInt("id"), user.getId());
    Assert.assertEquals(resultSet.getString("nick"), user.getNick());
    Assert.assertEquals("tango", resultSet.getString("style"));
    Assert.assertTrue(user.matchPassword("passwd"));
    try {
      user.checkBlocked();
    } catch (AccessViolationException e) {
      Assert.fail();
    }
    try {
      user.checkFrozen();
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
    Assert.assertFalse(user.isModerator());
    Assert.assertFalse(user.isAdministrator());
    Assert.assertFalse(user.canCorrect());
    Assert.assertFalse(user.isAnonymous());
    Assert.assertEquals(resultSet.getInt("score"), user.getScore());
    Assert.assertEquals("<span class=\"stars\">★</span>", user.getStatus()); 
    Assert.assertTrue(user.isActivated());
    Assert.assertFalse(user.isAnonymousScore());
    Assert.assertEquals(resultSet.getBoolean("corrector"), user.isCorrector());
    Assert.assertEquals(resultSet.getString("email"), user.getEmail());
    Assert.assertFalse(user.hasEmail());
    Assert.assertEquals(resultSet.getString("name"), user.getName());
    Assert.assertFalse(user.isFrozen());
  }

  /**
   * проверка замороженного пользователя
   * @throws Exception хм
   */
  @Test
  public void userFrozenTest() throws Exception {
    ResultSet resultSet = Users.getUserFrozen();
    User user = new User(resultSet);

    Assert.assertEquals(resultSet.getInt("id"), user.getId());
    Assert.assertEquals(resultSet.getString("nick"), user.getNick());
    Assert.assertEquals("tango", resultSet.getString("style"));
    Assert.assertTrue(user.matchPassword("passwd"));
    try {
      user.checkBlocked();
    } catch (AccessViolationException e) {
      Assert.fail();
    }
    try {
      user.checkFrozen();
      Assert.fail();
    } catch (AccessViolationException e) {
      Assert.assertEquals("Пользователь временно заморожен", e.getMessage());
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
    Assert.assertFalse(user.isModerator());
    Assert.assertFalse(user.isAdministrator());
    Assert.assertFalse(user.canCorrect());
    Assert.assertFalse(user.isAnonymous());
    Assert.assertEquals(resultSet.getInt("score"), user.getScore());
    Assert.assertEquals("<span class=\"stars\">★</span>", user.getStatus()); 
    Assert.assertTrue(user.isActivated());
    Assert.assertFalse(user.isAnonymousScore());
    Assert.assertEquals(resultSet.getBoolean("corrector"), user.isCorrector());
    Assert.assertEquals(resultSet.getString("email"), user.getEmail());
    Assert.assertFalse(user.hasEmail());
    Assert.assertEquals(resultSet.getString("name"), user.getName());
    Assert.assertTrue(user.isFrozen());
    Assert.assertEquals(resultSet.getTimestamp("frozen_until"), 
      user.getFrozenUntil());
    Assert.assertEquals(resultSet.getInt("frozen_by"), user.getFrozenBy());
    Assert.assertEquals(resultSet.getString("freezing_reason"), 
      user.getFreezingReason());
  }


  /**
   * проверка активации
   * @throws Exception хм
   */
  @Test
  public void hizelTest() throws Exception {
    ResultSet resultSet = Users.getHizel();
    User user = new User(resultSet);
    Assert.assertEquals("0428dfed932b07ea582efd94038b1076", user.getActivationCode("secret"));
  }


}
