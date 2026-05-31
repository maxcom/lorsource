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
import org.springframework.stereotype.Component
import scalikejdbc.{DBSession, SettingsProvider}

import javax.sql.DataSource

@Component
class SpringDB(dataSource: DataSource):
  private val settings = SettingsProvider.default.copy(jtaDataSourceCompatible = _ => true)

  def run[A](f: DBSession ?=> A): A =
    val conn = DataSourceUtils.getConnection(dataSource)
    try f(using DBSession(conn, settings = settings))
    finally DataSourceUtils.releaseConnection(conn, dataSource)
