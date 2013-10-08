package ru.org.linux.spring;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class JsonConfiguration {
  @Bean(name="jacksonMessageConverter")
  public MappingJackson2HttpMessageConverter jsonConverter() {
    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    converter.setPrettyPrint(true);
    converter.getObjectMapper().configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    return converter;
  }
}
