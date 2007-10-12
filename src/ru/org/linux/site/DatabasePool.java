package ru.org.linux.site;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.postgresql.ds.PGConnectionPoolDataSource;
import org.postgresql.ds.PGPoolingDataSource;

public class DatabasePool {
  private PGPoolingDataSource dataSource;
  private PGPoolingDataSource dataSourceWhois;

  private synchronized void initDataSource(Properties config) {
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

  private synchronized void initDataSourceWhois(Properties config) {
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

  public Connection getConnection(Properties config)
      throws SQLException {
    initDataSource(config);

    Connection db;

    boolean usePool = "TRUE".equals(config.getProperty("DB_POOL"));
    boolean useJNDI = "JNDI".equals(config.getProperty("DB_POOL"));

    if (useJNDI) {
      try {
        InitialContext cxt = new InitialContext();

        DataSource ds = (DataSource) cxt.lookup("java:/comp/env/jdbc/lor");

        if ( ds == null ) {
          throw new SQLException("Data source not found! (java:/comp/env/jdbc/lor)");
        }

        db = ds.getConnection();
      } catch (NamingException ex) {
        throw new RuntimeException(ex);
      }
    } else if (usePool) {
      db=dataSource.getConnection();
    } else {
      db=DriverManager.getConnection(config.getProperty("JDBC_URL"), config.getProperty("JDBC_USER"), config.getProperty("JDBC_PASS"));
    }

    return db;
  }

  public Connection getConnectionWhois(Properties config)
      throws SQLException {
    initDataSourceWhois(config);

    Connection db;
    boolean usePool = "TRUE".equals(config.getProperty("DB_POOL"));
    boolean useJNDI = "JNDI".equals(config.getProperty("DB_POOL"));

        if (useJNDI) {
      try {
        InitialContext cxt = new InitialContext();

        DataSource ds = (DataSource) cxt.lookup("java:/comp/env/jdbc/lor-whois");

        if ( ds == null ) {
          throw new SQLException("Data source not found! (java:/comp/env/jdbc/lor-whois)");
        }

        db = ds.getConnection();
      } catch (NamingException ex) {
        throw new RuntimeException(ex);
      }
    } else if (usePool)
      db = dataSourceWhois.getConnection();
    else
      db=DriverManager.getConnection(config.getProperty("JDBC_URL"), config.getProperty("JDBC_USER"), config.getProperty("JDBC_PASS"));

    return db;
  }

  private static final class MyDataSource extends PGPoolingDataSource {
    protected PGConnectionPoolDataSource createConnectionPool() {
      PGConnectionPoolDataSource pool = new PGConnectionPoolDataSource();
      pool.setDefaultAutoCommit(true);
      return pool;
    }
  }
}
