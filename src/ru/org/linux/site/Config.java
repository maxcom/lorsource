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
