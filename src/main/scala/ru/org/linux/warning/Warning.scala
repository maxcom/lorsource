/*
 * Copyright 1998-2024 Linux.org.ru
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

package ru.org.linux.warning

import ru.org.linux.user.User

import java.time.Instant
import java.util.Date
import javax.annotation.Nullable
import scala.beans.{BeanProperty, BooleanBeanProperty}

sealed trait WarningType {
  def id: String
  def name: String

  // for jsp
  final def getId: String = id
  final def getName: String = name
}

object WarningType {
  private val AllTypes = Seq(RuleWarning, TagsWarning, SpellingWarning)
  val idToType: Map[String, WarningType] = AllTypes.map(t => t.id -> t).toMap
}

object RuleWarning extends WarningType {
  override def id: String = "rule"
  override def name: String = "Нарушение правил"
}

object TagsWarning extends WarningType {
  override def id: String = "tag"
  override def name: String = "Некорректные теги"
}

object SpellingWarning extends WarningType {
  override def id: String = "spelling"
  override def name: String = "Опечатка или форматирование"
}

case class Warning(id: Int, topicId: Int, commentId: Option[Int], postdate: Instant, authorId: Int, message: String,
                   warningType: WarningType, closedBy: Option[Int], closedWhen: Option[Instant])

case class PreparedWarning(@BeanProperty postdate: Date, @BeanProperty author: User, @BeanProperty message: String,
                          @BeanProperty id: Int, @BeanProperty @Nullable closedBy: User) {
  // for jsp
  def isClosed = closedBy != null
}
