package ru.org.linux.user;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

@Configuration
@ImportResource("classpath:database.xml")
public class UserEventDaoIntegrationTestConfiguration {
  @Bean
  public UserEventDao userEventDao() {
    return new UserEventDao();
  }

  @Bean
  public DataSourceTransactionManager transactionManager(DataSource ds) {
    return new DataSourceTransactionManager(ds);
  }

  @Bean
  public UserDao userDao() {
    return new UserDao();
  }

  @Bean
  public UserLogDao userLogDao() {
    return Mockito.mock(UserLogDao.class);
  }
}
