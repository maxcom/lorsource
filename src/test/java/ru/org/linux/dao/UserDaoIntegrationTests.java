/*
 * Copyright 1998-2011 Linux.org.ru
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
package ru.org.linux.dao;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.org.linux.dto.UserDto;
import ru.org.linux.site.BadPasswordException;
import ru.org.linux.site.UserInfo;

import javax.mail.internet.InternetAddress;
import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("commonDAO-context.xml")
public class UserDaoIntegrationTests {

  private final static String RANDOM_USER_NAME = UUID.randomUUID().toString();
  private final static String RANDOM_USER_NICK = "a" + UUID.randomUUID().toString().replace('-', '_');
  private final static String RANDOM_USER_PASS = UUID.randomUUID().toString();
  private final static String USER_URL = "http://some.url";
  private final static String USER_TOWN = "Город";
  private final static String USER_EXTENDED_INFO = "Расширенная информация пользователя";

  @Autowired
  UserDao userDao;

  /**
   * Проверка создания пользователя, получения информации по нему и удаление пользователя.
   *
   * @throws Exception
   */
  @Test
  public void checkUserCreationFetchingAndDeleting()
      throws Exception {
    final InternetAddress USER_EMAIL = new InternetAddress("some@email.in.net");

    int newUserId = userDao.createUser(
        RANDOM_USER_NAME,
        RANDOM_USER_NICK,
        RANDOM_USER_PASS,
        USER_URL,
        USER_EMAIL,
        USER_TOWN,
        USER_EXTENDED_INFO
    );

    UserDto newUser = userDao.getUser(newUserId);
    try {
      Assert.assertTrue(RANDOM_USER_NAME.equals(newUser.getName()));
      Assert.assertTrue(RANDOM_USER_NICK.equals(newUser.getNick()));
      Assert.assertTrue(USER_EMAIL.equals(new InternetAddress(newUser.getEmail())));
      Assert.assertEquals(newUserId, newUser.getId());
      Assert.assertTrue(USER_EXTENDED_INFO.equals(userDao.getUserInfo(newUser)));

      UserInfo userInfo = userDao.getUserInfoClass(newUser);
      Assert.assertTrue(USER_URL.equals(userInfo.getUrl()));
      Assert.assertTrue(USER_TOWN.equals(userInfo.getTown()));

      try {
        newUser.checkPassword(RANDOM_USER_PASS);
      } catch (BadPasswordException e) {
        Assert.fail(e.getMessage());
      }
      try {
        newUser.checkPassword("wrong password");
        Assert.fail("Неправильный пароль!");
      } catch (BadPasswordException ignored) {
        // проигнорированное исключение
      }

      if (!userDao.isUserExists(RANDOM_USER_NICK)) {
        Assert.fail("Пользователь не найден!");
      }
      UserDto user = userDao.getUser(RANDOM_USER_NICK);
      Assert.assertEquals(newUserId, user.getId());
    } finally {
      userDao.deleteUser(newUser);
    }
  }

  // TODO: написать тесты для бан-листа, userInfo, userPhoto, score и прочего.
}
