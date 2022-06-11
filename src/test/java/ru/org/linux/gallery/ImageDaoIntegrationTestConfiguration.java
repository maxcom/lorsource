/*
 * Copyright 1998-2022 Linux.org.ru
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

package ru.org.linux.gallery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import ru.org.linux.section.SectionDao;
import ru.org.linux.section.SectionDaoImpl;
import ru.org.linux.section.SectionService;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserLogDao;

import javax.sql.DataSource;

import static org.mockito.Mockito.mock;

@Configuration
@ImportResource("classpath:common.xml")
public class ImageDaoIntegrationTestConfiguration {
  @Bean
  public ImageDao imageDao() {
    return new ImageDao();
  }

  @Bean
  public SectionService sectionService(SectionDao sectionDao) {
    return new SectionService(sectionDao);
  }

  @Bean
  public SectionDao sectionDao() {
    return new SectionDaoImpl();
  }

  @Bean
  public UserDao userDao(UserLogDao userLogDao, DataSource dataSource) {
    return new UserDao(userLogDao, dataSource);
  }

  @Bean
  public UserLogDao userLogDao() {
    return mock(UserLogDao.class);
  }
}
