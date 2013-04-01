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
  public IgnoreListDao ignoreListDao() {
    return mock(IgnoreListDao.class);
  }

  @Bean
  public UserLogDao userLogDao() {
    return mock(UserLogDao.class);
  }
}
