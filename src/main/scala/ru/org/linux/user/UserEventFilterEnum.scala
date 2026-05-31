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

enum UserEventFilterEnum(val label: String, val dbType: String):
  case ALL extends UserEventFilterEnum("все", "")
  case ANSWERS extends UserEventFilterEnum("ответы", "REPLY")
  case FAVORITES extends UserEventFilterEnum("отслеживаемое", "WATCH")
  case DELETED extends UserEventFilterEnum("удаленное", "DEL")
  case REFERENCE extends UserEventFilterEnum("упоминания", "REF")
  case TAG extends UserEventFilterEnum("теги", "TAG")
  case REACTION extends UserEventFilterEnum("реакции", "REACTION")
  case WARNING extends UserEventFilterEnum("предупреждения", "WARNING")

  def getName: String = toString.toLowerCase

  def getLabel: String = label

  def getType: String = dbType

object UserEventFilterEnum:
  def valueOfByType(dbType: String): Option[UserEventFilterEnum] = values.find(_.dbType == dbType)

  def fromNameOrDefault(filterAction: String): UserEventFilterEnum =
    values.find(_.getName == filterAction).getOrElse(ALL)
