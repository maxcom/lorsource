/*
 * Copyright 1998-2015 Linux.org.ru
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
import org.springframework.context.annotation.ImportResource;

import static org.mockito.Mockito.mock;

@Configuration
@ImportResource("classpath:database.xml")
public class SimpleIntegrationTestConfiguration {
  @Bean
  public UserTagDao userTagDao() {
    return new UserTagDao();
  }

  @Bean
  public UserDao userDao() {
    return new UserDao();
  }

  @Bean
  public UserLogDao userLogDao() {
    return mock(UserLogDao.class);
  }
}
