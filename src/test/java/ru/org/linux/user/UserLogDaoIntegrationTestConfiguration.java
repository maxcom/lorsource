package ru.org.linux.user;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource("classpath:database.xml")
public class UserLogDaoIntegrationTestConfiguration {
  @Bean
  public UserLogDao userLogDao() {
    return new UserLogDao();
  }
}
