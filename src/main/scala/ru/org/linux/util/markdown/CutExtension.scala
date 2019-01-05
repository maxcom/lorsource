/*
 * Copyright 1998-2019 Linux.org.ru
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

package ru.org.linux.util.markdown

import java.util

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.renderer.{NodeRenderer, NodeRenderingHandler}
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.options.{DataHolder, DataKey, MutableDataHolder}

import scala.collection.JavaConverters._

object CutExtension {
  val CutCollapsed = new DataKey[Boolean]("CutCollapsed", false)
  val CutLink = new DataKey[String]("CutLink", "")
}

class CutExtension extends Parser.ParserExtension with HtmlRenderer.HtmlRendererExtension {
  override def parserOptions(mutableDataHolder: MutableDataHolder): Unit = {}

  override def extend(builder: Parser.Builder): Unit = {
    builder.customBlockParserFactory(new LorCutParser.Factory)
  }

  override def rendererOptions(mutableDataHolder: MutableDataHolder): Unit = {}

  override def extend(builder: HtmlRenderer.Builder, renderType: String): Unit = {
    builder.nodeRendererFactory(options ⇒ new CutRenderer(options))
  }
}

class CutRenderer(options: DataHolder) extends NodeRenderer {
  override def getNodeRenderingHandlers: util.Set[NodeRenderingHandler[_ <: Node]] = Set(
    new NodeRenderingHandler[CutNode](classOf[CutNode], (node, ctx, html) => {
      val id = ctx.getNodeId(node)

      if (options.get(CutExtension.CutCollapsed)) {
        html.tag("p")

        html.text("( ")

        html
          .withAttr()
          .attr("href", options.get(CutExtension.CutLink) + "#" + id)
          .tag("a")
          .text("читать дальше...")
          .closeTag("a")

        html.text(" )")

        html.closeTag("p")
      } else {
        html.withAttr.attr("id", id).tagLineIndent("div", () => {
          ctx.renderChildren(node)
        })
      }

  })).asJava.asInstanceOf[java.util.Set[NodeRenderingHandler[_]]]
}
