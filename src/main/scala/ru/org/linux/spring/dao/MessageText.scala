/*
 * Copyright 1998-2018 Linux.org.ru
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

package ru.org.linux.spring.dao

sealed trait MarkupType {
  def id: String
}

object MarkupType {
  case object Html extends MarkupType {
    override val id = "PLAIN"
  }

  case object Lorcode extends MarkupType {
    override val id = "BBCODE_TEX"
  }

//  case object Markdown extends MarkupType

  def of(v: String): MarkupType = v match {
    case Html.id      ⇒ Html
    case Lorcode.id   ⇒ Lorcode
    case other        ⇒ throw new IllegalArgumentException(s"Unsupported markup type $other")
  }
}

case class MessageText(text: String, markup: MarkupType)