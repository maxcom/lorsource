package ru.org.linux.tag;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource("classpath:database.xml")
public class TagIntegrationTestConfiguration {
  @Bean
  public TagDao tagDao() {
    return new TagDao();
  }
}
