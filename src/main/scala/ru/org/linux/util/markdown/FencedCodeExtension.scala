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

import com.vladsch.flexmark.ast.FencedCodeBlock
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.renderer.{CoreNodeRenderer, NodeRenderer, NodeRenderingHandler}
import com.vladsch.flexmark.util.options.MutableDataHolder

import scala.jdk.CollectionConverters._

class FencedCodeExtension extends HtmlRenderer.HtmlRendererExtension {
  override def rendererOptions(options: MutableDataHolder): Unit = {}

  override def extend(rendererBuilder: HtmlRenderer.Builder, rendererType: String): Unit = {
    if (rendererBuilder.isRendererType("HTML")) {
      rendererBuilder.nodeRendererFactory(_ => new FencedCodeRenderer)
    }
  }
}


class FencedCodeRenderer extends NodeRenderer {
  override def getNodeRenderingHandlers = Set(new NodeRenderingHandler[FencedCodeBlock](classOf[FencedCodeBlock], (node, context, html) => {
    html.line
    html.srcPosWithTrailingEOL(node.getChars).withAttr().attr("class", "code").tag("div")

    html.withAttr.tag("pre").openPre

    val info = node.getInfo
    if (info.isNotNull && !info.isBlank) {
      val language = node.getInfoDelimitedByAny(" ")
      html.attr("class", context.getHtmlOptions.languageClassPrefix + language.unescape)
    }
    else {
      val noLanguageClass = context.getHtmlOptions.noLanguageClass.trim
      if (!noLanguageClass.isEmpty) html.attr("class", noLanguageClass)
    }

    html.srcPosWithEOL(node.getContentChars).withAttr(CoreNodeRenderer.CODE_CONTENT).tag("code")
    html.text(node.getContentChars.normalizeEOL)
    html.tag("/code")
    html.tag("/pre").closePre
    html.lineIf(context.getHtmlOptions.htmlBlockCloseTagEol)

    html.closeTag("div")
  })).asJava.asInstanceOf[java.util.Set[NodeRenderingHandler[_]]]
}
