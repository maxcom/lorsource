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

package ru.org.linux.markup

enum MarkupType(val id: String, val title: String, val formId: String, val deprecated: Boolean = false)
    extends Enum[MarkupType]:
  case Markdown extends MarkupType(id = "MARKDOWN", title = "Markdown", formId = "markdown")
  case Lorcode extends MarkupType(id = "BBCODE_TEX", title = "LORCODE", formId = "lorcode")
  case LorcodeUlb extends MarkupType(id = "BBCODE_ULB", title = "User line break", formId = "ntobr", deprecated = true)
  case Html extends MarkupType(id = "PLAIN", title = "HTML", formId = "plain", deprecated = true)

object MarkupType:
  def of(v: String): MarkupType =
    v match
      case Html.id =>
        Html
      case Lorcode.id =>
        Lorcode
      case LorcodeUlb.id =>
        LorcodeUlb
      case Markdown.id =>
        Markdown
      case other =>
        throw new IllegalArgumentException(s"Unsupported markup type $other")

  def ofFormId(v: String): MarkupType =
    v match
      case Html.formId =>
        Html
      case Lorcode.formId =>
        Lorcode
      case LorcodeUlb.formId =>
        LorcodeUlb
      case Markdown.formId =>
        Markdown
      case other =>
        throw new IllegalArgumentException(s"Unsupported markup type $other")

  val AllFormIds: Set[String] = values.view.map(_.formId).toSet
