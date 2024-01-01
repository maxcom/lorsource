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

import com.google.common.collect.ImmutableList;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@EnableCaching
public class UserDaoIntegrationTestConfiguration {
  private static final String USERS_CACHE = "Users";

  @Bean
  public UserDao userDao(UserLogDao userLogDao, DataSource dataSource) {
    return new UserDao(userLogDao, dataSource);
  }

  @Bean
  public CacheManager cacheManager() {
    SimpleCacheManager cacheManager = new SimpleCacheManager();

    cacheManager.setCaches(
            ImmutableList.of(new ConcurrentMapCache(USERS_CACHE))
    );

    return cacheManager;
  }

  @Bean(name="usersCache")
  public Cache usersCache(CacheManager cacheManager) {
    return cacheManager.getCache(USERS_CACHE);
  }

  @Bean
  public UserLogDao userLogDao() {
    return new UserLogDao();
  }
}
