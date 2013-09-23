package ru.org.linux.gallery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import ru.org.linux.section.SectionDao;
import ru.org.linux.section.SectionDaoImpl;
import ru.org.linux.section.SectionService;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserLogDao;

import static org.mockito.Mockito.mock;

@Configuration
@ImportResource({"classpath:database.xml","classpath:common.xml"})
public class ImageDaoIntegrationTestConfiguration {
  @Bean
  public ImageDao imageDao() {
    return new ImageDao();
  }

  @Bean
  public SectionService sectionService() {
    return new SectionService();
  }

  @Bean
  public SectionDao sectionDao() {
    return new SectionDaoImpl();
  }

  @Bean
  public UserDao userDao() {
    return new UserDao();
  }

  @Bean
  public UserLogDao userLogDao() {
    return mock(UserLogDao.class);
  }
}
