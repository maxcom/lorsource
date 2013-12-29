package ru.org.linux.tag;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import javax.sql.DataSource;

@Configuration
@ImportResource("classpath:database.xml")
public class TagIntegrationTestConfiguration {
  @Bean
  public TagDao tagDao(DataSource ds) {
    return new TagDao(ds);
  }
}
