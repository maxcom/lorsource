package ru.org.linux.site.config;

import java.sql.Connection;
import java.sql.SQLException;

public interface SQLConfig {
  Connection getConnection()
      throws SQLException;

  void SQLclose() throws SQLException;
}
