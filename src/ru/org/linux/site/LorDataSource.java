package ru.org.linux.site;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class LorDataSource {
  private LorDataSource() {
  }

  public static Connection getConnection()
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
}
