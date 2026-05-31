/*
 * Copyright 1998-2026 Linux.org.ru
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

package ru.org.linux.gallery

import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.section.{SectionDao, SectionDaoImpl, SectionService}
import ru.org.linux.user.UserDao

import javax.sql.DataSource

@Configuration @ImportResource(Array("classpath:common.xml"))
class ImageDaoIntegrationTestConfiguration:

  @Bean
  def imageDao(sectionService: SectionService, dataSource: DataSource): ImageDao =
    new ImageDao(sectionService, dataSource)

  @Bean
  def sectionService(sectionDao: SectionDao): SectionService = new SectionService(sectionDao)

  @Bean
  def sectionDao(springDB: SpringDB): SectionDao = new SectionDaoImpl(springDB)

  @Bean
  def userDao(dataSource: DataSource): UserDao = new UserDao(dataSource)

end ImageDaoIntegrationTestConfiguration
