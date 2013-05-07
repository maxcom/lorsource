package ru.org.linux.comment;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import ru.org.linux.spring.dao.DeleteInfoDao;

import javax.sql.DataSource;

@Configuration
@ImportResource("classpath:database.xml")
public class CommentDaoIntegrationTestConfiguration {
  @Bean
  public DataSourceTransactionManager transactionManager(DataSource ds) {
    return new DataSourceTransactionManager(ds);
  }

  @Bean
  public CommentDao commentDao() {
    return new CommentDaoImpl();
  }

  @Bean
  public DeleteInfoDao deleteInfoDao() {
    return new DeleteInfoDao();
  }
}
