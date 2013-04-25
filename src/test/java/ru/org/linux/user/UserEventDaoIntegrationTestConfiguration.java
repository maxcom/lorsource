package ru.org.linux.user;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

@Configuration
@ImportResource("classpath:database.xml")
public class UserEventDaoIntegrationTestConfiguration {
  @Bean
  public UserEventDao userLogDao() {
    return new UserEventDao();
  }

  @Bean
  public DataSourceTransactionManager transactionManager(DataSource ds) {
    return new DataSourceTransactionManager(ds);
  }
}
