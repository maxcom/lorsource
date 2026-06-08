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

package ru.org.linux.user

import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.transaction.PlatformTransactionManager
import ru.org.linux.scalikejdbc.SpringDB

import javax.sql.DataSource

@Configuration
class UserEventDaoIntegrationTestConfiguration:
  @Bean
  def springDB(dataSource: DataSource,transactionManager: PlatformTransactionManager): SpringDB = 
    SpringDB(dataSource, transactionManager)

  @Bean
  def userEventDao(springDB: SpringDB): UserEventDao = UserEventDao(springDB)

  @Bean
  def userDao(springDB: SpringDB): UserDao = UserDao(springDB)
