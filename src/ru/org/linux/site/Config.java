/*
 * Copyright 1998-2009 Linux.org.ru
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

package ru.org.linux.site;

import java.util.Properties;

import ru.org.linux.site.config.PathConfig;
import ru.org.linux.site.config.PropertiesConfig;
import ru.org.linux.site.config.StorageConfig;
import ru.org.linux.storage.FileStorage;
import ru.org.linux.storage.Storage;

public class Config
implements
    PropertiesConfig,
    StorageConfig,
    PathConfig {
  private final Properties config;
  private Storage storage=null;

  public Config(Properties configfile) {
    config=configfile;

    if (config==null) {
      throw new NullPointerException("configfile==null ?!");
    }
  }

  public Properties getProperties() {
    return config;
  }

  public Storage getStorage() {
    if (storage == null) {
      storage = new FileStorage(getPathPrefix() + "linux-storage/");
    }
    return storage;
  }

  public String getPathPrefix() {
    return config.getProperty("PathPrefix");
  }

  public String getHTMLPathPrefix() {
    return config.getProperty("HTMLPathPrefix");
  }
}
