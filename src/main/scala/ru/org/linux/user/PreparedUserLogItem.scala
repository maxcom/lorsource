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

import com.google.common.collect.ImmutableMap
import scala.beans.BeanProperty
import scala.jdk.CollectionConverters.MapHasAsJava

case class PreparedUserLogItem(
  @BeanProperty item: UserLogItem,
  @BeanProperty actionUser: User,
  @BeanProperty options: ImmutableMap[String, String],
  @BeanProperty self: Boolean
)

object PreparedUserLogItem {
  def apply(item: UserLogItem, actionUser: User, options: java.util.Map[String, String]): PreparedUserLogItem = {
    new PreparedUserLogItem(
      item,
      actionUser,
      ImmutableMap.copyOf(options),
      item.getUser == item.getActionUser
    )
  }
}
