package ru.org.linux.site;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.postgresql.ds.PGConnectionPoolDataSource;
import org.postgresql.ds.PGPoolingDataSource;

public class DatabasePool {
  private PGPoolingDataSource dataSource;
  private PGPoolingDataSource dataSourceWhois;

  protected synchronized void initDataSource(Properties config) {
    if (dataSource!=null) return;

    dataSource = new MyDataSource();

    /* TODO use config file */
    dataSource.setDataSourceName("Main pool");
    dataSource.setServerName("localhost");
    dataSource.setDatabaseName("linux");
    dataSource.setUser(config.getProperty("JDBC_USER"));
    dataSource.setPassword(config.getProperty("JDBC_PASS"));

    dataSource.setMaxConnections(10);
    dataSource.setInitialConnections(10);
  }

  protected synchronized void initDataSourceWhois(Properties config) {
    if (dataSourceWhois!=null) return;

    dataSourceWhois = new MyDataSource();

    /* TODO use config file */
    dataSourceWhois.setDataSourceName("Whois pool");
    dataSourceWhois.setServerName("localhost");
    dataSourceWhois.setDatabaseName("linux");
    dataSourceWhois.setUser(config.getProperty("JDBC_USER"));
    dataSourceWhois.setPassword(config.getProperty("JDBC_PASS"));

    dataSourceWhois.setMaxConnections(4);
    dataSourceWhois.setInitialConnections(4);
  }

  public Connection getConnection(Properties config, String user)
      throws SQLException {
    initDataSource(config);

    Connection db;

    boolean usePool = "TRUE".equals(config.getProperty("DB_POOL"));

    if (usePool)
      db=dataSource.getConnection();
    else
      db=DriverManager.getConnection(config.getProperty("JDBC_URL"), config.getProperty("JDBC_USER"), config.getProperty("JDBC_PASS"));

    return db;
  }

  public Connection getConnectionWhois(Properties config)
      throws SQLException {
    initDataSourceWhois(config);

    Connection db;
    boolean usePool = "TRUE".equals(config.getProperty("DB_POOL"));

    if (usePool)
      db = dataSourceWhois.getConnection();
    else
      db=DriverManager.getConnection(config.getProperty("JDBC_URL"), config.getProperty("JDBC_USER"), config.getProperty("JDBC_PASS"));

    return db;
  }

  static final class MyDataSource extends PGPoolingDataSource {
    protected PGConnectionPoolDataSource createConnectionPool() {
      PGConnectionPoolDataSource pool = new PGConnectionPoolDataSource();
      pool.setDefaultAutoCommit(true);
      return pool;
    }
  }
}
