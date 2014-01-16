package ru.org.linux.topic;

import net.tanesha.recaptcha.ReCaptcha;
import org.elasticsearch.client.Client;
import org.mockito.Mockito;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Controller;
import ru.org.linux.search.SearchQueueListener;
import ru.org.linux.search.SearchQueueSender;
import ru.org.linux.spring.FeedPinger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static org.mockito.Mockito.mock;

@Configuration
@ImportResource("classpath:database.xml")
@ComponentScan(
        basePackages = "ru.org.linux",
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ANNOTATION,
                        value = {Controller.class,Configuration.class}
                ),
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        value = {
                                SearchQueueListener.class,
                                SearchQueueSender.class,
                                FeedPinger.class,
                                TopicListService.class,
                        }
                )
        }
)
public class TopicIntegrationTestConfiguration {
  @Bean
  public TopicController topicController() {
    return new TopicController();
  }

  @Bean
  public ReCaptcha reCaptcha() {
    return mock(ReCaptcha.class);
  }

  @Bean
  public Properties properties() throws IOException {
    Properties properties = new Properties();

    properties.load(new FileInputStream("src/main/webapp/WEB-INF/config.properties.dist"));

    return properties;
  }

  @Bean
  public Client elasticsearch() {
    return Mockito.mock(Client.class);
  }

}
