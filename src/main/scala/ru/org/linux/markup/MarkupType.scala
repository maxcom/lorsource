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

package ru.org.linux.markup

sealed trait MarkupType extends Product with Serializable {
  def id: String
  def title: String
  def formId: String
  def deprecated: Boolean = false
  def order: Int
}

object MarkupType {
  case object Html extends MarkupType {
    override val id = "PLAIN"
    override val title: String = "HTML"
    override val formId: String = "plain"
    override val order: Int = 4
  }

  case object Lorcode extends MarkupType {
    override val id = "BBCODE_TEX"
    override val title: String = "LORCODE"
    override val formId: String = "lorcode"
    override val order: Int = 1
  }

  case object LorcodeUlb extends MarkupType {
    override val id = "BBCODE_ULB"
    override val title: String = "User line break"
    override val formId: String = "ntobr"
    override val deprecated = true
    override val order: Int = 3
  }

  case object Markdown extends MarkupType {
    override val id = "MARKDOWN"
    override val title: String = "Markdown (beta)"
    override val formId: String = "markdown"
    override val order: Int = 2
  }

  def of(v: String): MarkupType = v match {
    case Html.id       ⇒ Html
    case Lorcode.id    ⇒ Lorcode
    case LorcodeUlb.id ⇒ LorcodeUlb
    case Markdown.id   ⇒ Markdown
    case other         ⇒ throw new IllegalArgumentException(s"Unsupported markup type $other")
  }

  val All: Set[MarkupType] = Set(Lorcode, LorcodeUlb, Markdown, Html)

  val AllFormIds: Set[String] = All.map(_.formId)
}