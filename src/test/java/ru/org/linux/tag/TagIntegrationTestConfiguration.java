package ru.org.linux.tag;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import ru.org.linux.user.IgnoreListDao;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserLogDao;

import javax.sql.DataSource;

import static org.mockito.Mockito.mock;

@Configuration
@ImportResource("classpath:database.xml")
public class TagIntegrationTestConfiguration {
  @Bean
  public TagDao tagDao() {
    return new TagDao();
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
  public DataSourceTransactionManager transactionManager(DataSource ds) {
    return new DataSourceTransactionManager(ds);
  }
}
