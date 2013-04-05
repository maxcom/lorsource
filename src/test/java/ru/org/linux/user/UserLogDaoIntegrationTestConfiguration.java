package ru.org.linux.user;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

import static org.mockito.Mockito.mock;

@Configuration
@ImportResource("classpath:database.xml")
public class UserLogDaoIntegrationTestConfiguration {
  @Bean
  public UserLogDao userLogDao() {
    return new UserLogDao();
  }

  @Bean
  public DataSourceTransactionManager transactionManager(DataSource ds) {
    return new DataSourceTransactionManager(ds);
  }
}
