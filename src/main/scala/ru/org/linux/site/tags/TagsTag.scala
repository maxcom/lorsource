/*
 * Copyright 1998-2023 Linux.org.ru
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
package ru.org.linux.site.tags

import ru.org.linux.tag.TagRef
import ru.org.linux.util.StringUtil

import javax.servlet.jsp.JspException
import javax.servlet.jsp.tagext.{Tag, TagSupport}
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util
import scala.jdk.CollectionConverters.ListHasAsScala

/**
 * tags tag
 */
class TagsTag extends TagSupport {
  private var list: collection.Seq[TagRef] = _
  private var deletable: Boolean = false

  def setList(list: util.List[TagRef]): Unit = this.list = list.asScala

  def setDeletable(value: Boolean): Unit = deletable = value

  @throws[JspException]
  override def doStartTag: Int = {
    val out = pageContext.getOut

    if (list != null) try {
      val buf = new StringBuilder("<p class=\"tags\"><i class=\"icon-tag\"></i>&nbsp;")

      buf.append(list.map { el =>
        (if (el.url.isDefined) {
          s"""<a class=tag rel=tag href="${el.url.get}">${StringUtil.escapeHtml(el.name)}</a>"""
        } else {
          s"<span class=tag>${StringUtil.escapeHtml(el.name)}</span>"
        }) + (if (deletable) {
          s""" [<a href="/tags/delete?tagName=${URLEncoder.encode(el.name, StandardCharsets.UTF_8)}">X</a>]"""
        } else {
          ""
        })
      }.mkString(", "))

      buf.append("</p>")

      out.append(buf)
    } catch {
      case e: IOException =>
        throw new JspException("Error:" + e.getMessage)
    }

    Tag.SKIP_BODY
  }
}