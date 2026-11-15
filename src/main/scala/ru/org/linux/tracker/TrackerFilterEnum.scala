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
package ru.org.linux.tracker

enum TrackerFilterEnum(val value: String, val label: String, val canBeDefault: Boolean) extends Enum[TrackerFilterEnum]:
  case ALL extends TrackerFilterEnum("all", "все", true)
  case MAIN extends TrackerFilterEnum("main", "основные", true)
  case NOTALKS extends TrackerFilterEnum("notalks", "без talks", true)
  case TECH extends TrackerFilterEnum("tech", "тех. форум", false)

  def getValue: String = value
  def getLabel: String = label

object TrackerFilterEnum:
  def getByValue(filterAction: String): Option[TrackerFilterEnum] = values.find(_.value == filterAction)
