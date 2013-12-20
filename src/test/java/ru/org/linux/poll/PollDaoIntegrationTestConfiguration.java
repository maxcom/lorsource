package ru.org.linux.poll;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource("classpath:database.xml")
public class PollDaoIntegrationTestConfiguration {
  @Bean
  public PollDao pollDao() {
    return new PollDao();
  }
}
