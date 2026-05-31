/*
 * Copyright 1998-2026 Linux.org.ru
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

package ru.org.linux.scalikejdbc

import org.springframework.jdbc.datasource.DataSourceUtils
import scalikejdbc.{DB, DBSession}

import java.util.concurrent.atomic.AtomicReference
import javax.sql.DataSource

object SpringDB:

  private val dataSourceRef = new AtomicReference[DataSource]()

  def setDataSource(dataSource: DataSource): Unit = dataSourceRef.set(dataSource)

  private def ensureInitialized(): DataSource =
    val ds = dataSourceRef.get()
    if ds != null then
      ds
    else
      throw new IllegalStateException("ScalikeJdbcInitializer has not been initialized yet")

  def run[A](f: DBSession ?=> A): A =
    val ds = ensureInitialized()
    val conn = DataSourceUtils.getConnection(ds)
    try DB(conn).autoClose(false).withinTx(f(using _))
    finally DataSourceUtils.releaseConnection(conn, ds)

end SpringDB
