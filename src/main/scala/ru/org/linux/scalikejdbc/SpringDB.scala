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
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import scalikejdbc.{DBSession, ParameterBinderFactory, SettingsProvider, TypeBinder}

import javax.sql.DataSource
import java.sql.PreparedStatement
import java.util as ju

opaque type Transaction = DBSession

object Transaction:
  private[scalikejdbc] def apply(session: DBSession): Transaction = session
  given dbSession(using t: Transaction): DBSession = t

@Component
class SpringDB(dataSource: DataSource, val transactionManager: PlatformTransactionManager)
    extends TransactionManagement:
  private val settings = SettingsProvider.default.copy(jtaDataSourceCompatible = _ => true)

  def run[A](f: DBSession ?=> A): A =
    val conn = DataSourceUtils.getConnection(dataSource)
    try f(using DBSession(conn, settings = settings))
    finally DataSourceUtils.releaseConnection(conn, dataSource)

  def localTx[A](f: Transaction ?=> A): A =
    try
      transactional(): _ =>
        val conn = DataSourceUtils.getConnection(dataSource)
        try
          given Transaction = Transaction(DBSession(conn, settings = settings))
          f(using summon[Transaction])
        finally DataSourceUtils.releaseConnection(conn, dataSource)
    catch
      case e: java.lang.reflect.UndeclaredThrowableException =>
        val cause = e.getCause
        if cause != null then throw cause else throw e

object SpringDB:
  given hstorePbf: ParameterBinderFactory[ju.Map[String, String]] =
    ParameterBinderFactory[ju.Map[String, String]] { value => (stmt: PreparedStatement, idx: Any) =>
      stmt.setObject(idx.asInstanceOf[Int], value)
    }

  given hstoreTypeBinder: TypeBinder[ju.Map[String, String]] with
    def apply(rs: java.sql.ResultSet, label: String): ju.Map[String, String] =
      rs.getObject(label).asInstanceOf[ju.Map[String, String]]
    def apply(rs: java.sql.ResultSet, idx: Int): ju.Map[String, String] =
      rs.getObject(idx).asInstanceOf[ju.Map[String, String]]
