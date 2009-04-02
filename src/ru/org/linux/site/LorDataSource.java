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
