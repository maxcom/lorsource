/*
 * Copyright 1998-2012 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.user;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
public class ProfileDaoTestConfiguration {
  private File tempDir;

  @PostConstruct
  public void createTempDir() throws IOException {
    tempDir = Files.createTempDir();

    Files.createParentDirs(new File(tempDir, "linux-storage/profile/test"));
  }

  @PreDestroy
  public void dropTempDir() throws IOException {
    FileUtils.deleteDirectory(tempDir);
  }

  @Bean
  public ProfileDao profileDao() {
    return new ProfileDao();
  }

  @Bean
  public ru.org.linux.spring.Configuration configuration() {
    ru.org.linux.spring.Configuration mock = mock(ru.org.linux.spring.Configuration.class);

    when(mock.getPathPrefix()).thenReturn(tempDir.getAbsolutePath()+ '/');

    return mock;
  }

  @Bean(name="properties")
  public Properties properties() {
    return new Properties();
  }

}
