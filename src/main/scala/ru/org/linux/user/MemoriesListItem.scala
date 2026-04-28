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

package ru.org.linux.user

import java.sql.{ResultSet, Timestamp}
import scala.beans.{BeanProperty, BooleanBeanProperty}

case class MemoriesListItem(
  @BeanProperty id: Int,
  @BeanProperty userid: Int,
  @BeanProperty timestamp: Timestamp,
  @BeanProperty topic: Int,
  @BooleanBeanProperty watch: Boolean
)

object MemoriesListItem {
  def apply(rs: ResultSet): MemoriesListItem = {
    new MemoriesListItem(
      rs.getInt("id"),
      rs.getInt("userid"),
      rs.getTimestamp("add_date"),
      rs.getInt("topic"),
      rs.getBoolean("watch")
    )
  }
}
