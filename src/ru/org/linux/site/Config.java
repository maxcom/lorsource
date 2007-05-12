package ru.org.linux.site;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Date;

import ru.org.linux.site.config.PathConfig;
import ru.org.linux.site.config.PropertiesConfig;
import ru.org.linux.site.config.SQLConfig;
import ru.org.linux.site.config.StorageConfig;
import ru.org.linux.storage.FileStorage;
import ru.org.linux.storage.Storage;

public class Config
implements
    SQLConfig,
    PropertiesConfig,
    StorageConfig,
    PathConfig {
  private final Properties config;
  private Connection db=null;
  private Storage storage=null;
  private long dbWaitTime=0;

  protected static final DatabasePool pool = new DatabasePool();

  public Config(Properties configfile) {
    config=configfile;

    if (config==null) {
      throw new NullPointerException("configfile==null ?!");
    }
  }

  public synchronized Connection getConnection(String user)
      throws SQLException {
    if (db==null) {
      long startMillis = new Date().getTime();

      db=pool.getConnection(config, user);
      //logger.notice("config", "opened connection for '"+user+"'");

      long endMillis = new Date().getTime();

      dbWaitTime = endMillis - startMillis;
    }

    return db;
  }

  public synchronized Connection getConnectionWhois()
      throws SQLException {
    if (db==null) {
      long startMillis = new Date().getTime();

      db=pool.getConnectionWhois(config);

      long endMillis = new Date().getTime();

      dbWaitTime = endMillis - startMillis;
    }

    return db;
  }

  public synchronized void SQLclose() throws SQLException {
    if (db != null) {
      db.close();
    }

    db=null;
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

  public long getDbWaitTime() {
    return dbWaitTime;
  }
}
