package ru.org.linux.comment;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import ru.org.linux.spring.dao.DeleteInfoDao;

@Configuration
@ImportResource("classpath:database.xml")
public class CommentDaoIntegrationTestConfiguration {
  @Bean
  public CommentDao commentDao() {
    return new CommentDao();
  }

  @Bean
  public DeleteInfoDao deleteInfoDao() {
    return new DeleteInfoDao();
  }
}
