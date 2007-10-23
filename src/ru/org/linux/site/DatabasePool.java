package ru.org.linux.site;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

class DatabasePool {
  public Connection getConnection()
      throws SQLException {
    Connection db;

    try {
      InitialContext cxt = new InitialContext();

      DataSource ds = (DataSource) cxt.lookup("java:/comp/env/jdbc/lor");

      if (ds == null) {
        throw new SQLException("Data source not found! (java:/comp/env/jdbc/lor)");
      }

      db = ds.getConnection();
    } catch (NamingException ex) {
      throw new RuntimeException(ex);
    }

    return db;
  }

  public Connection getConnectionWhois()
      throws SQLException {
    Connection db;

    try {
      InitialContext cxt = new InitialContext();

      DataSource ds = (DataSource) cxt.lookup("java:/comp/env/jdbc/lor-whois");

      if (ds == null) {
        throw new SQLException("Data source not found! (java:/comp/env/jdbc/lor-whois)");
      }

      db = ds.getConnection();
    } catch (NamingException ex) {
      throw new RuntimeException(ex);
    }

    return db;
  }
}
