package ru.org.linux.spring;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.springframework.stereotype.Component;

@Component
public class ObjectMapperFactory {
  public ObjectMapper getMapper() {
    ObjectMapper mapper = new ObjectMapper();

    mapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);

    return mapper;
  }
}
