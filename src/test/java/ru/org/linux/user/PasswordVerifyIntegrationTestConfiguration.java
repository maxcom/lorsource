package ru.org.linux.user;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;

import ru.org.linux.spring.Configuration;

import java.util.Properties;

import static org.mockito.Mockito.mock;

/**
 */
@org.springframework.context.annotation.Configuration
@ImportResource("classpath:database.xml")
public class PasswordVerifyIntegrationTestConfiguration {
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

  @Bean
  public PasswordVerify passwordVerify() {
    return new PasswordVerify();
  }

  @Bean
  public Configuration configuration() {
    return new Configuration();
  }

  @Bean
  public Properties properties() throws Exception {
    Properties properties = new Properties();
    properties.load(getClass().getResourceAsStream("/config.properties.user.test"));
    return properties;
  }


}
