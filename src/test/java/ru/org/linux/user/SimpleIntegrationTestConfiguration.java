/*
 * Copyright 1998-2024 Linux.org.ru
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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import ru.org.linux.auth.IPBlockDao;
import ru.org.linux.spring.SiteConfig;
import ru.org.linux.spring.dao.DeleteInfoDao;
import ru.org.linux.spring.dao.UserAgentDao;

import javax.sql.DataSource;

import static org.mockito.Mockito.mock;

@Configuration
public class SimpleIntegrationTestConfiguration {
  @Bean
  public UserTagDao userTagDao(DataSource ds) {
    return new UserTagDao(ds);
  }

  @Bean
  public UserDao userDao(UserLogDao userLogDao, DataSource dataSource) {
    return new UserDao(userLogDao, dataSource);
  }

  @Bean
  UserService userService(UserDao userDao, UserLogDao userLogDao,
                          PlatformTransactionManager transactionManager) {
    return new UserService(mock(SiteConfig.class), userDao, mock(IgnoreListDao.class), mock(UserInvitesDao.class),
            userLogDao, mock(DeleteInfoDao.class), mock(IPBlockDao.class), mock(UserAgentDao.class),
            transactionManager);
  }

  @Bean
  public UserLogDao userLogDao() {
    return mock(UserLogDao.class);
  }
}
