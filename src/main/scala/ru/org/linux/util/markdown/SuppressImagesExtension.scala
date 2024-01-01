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

package ru.org.linux.util.markdown

import com.vladsch.flexmark.ast.{Image, ImageRef}
import com.vladsch.flexmark.ast.util.TextCollectingVisitor
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.renderer.{NodeRenderer, NodeRenderingHandler}
import com.vladsch.flexmark.util.options.MutableDataHolder

import scala.jdk.CollectionConverters._

class SuppressImagesExtension extends HtmlRenderer.HtmlRendererExtension {
  override def rendererOptions(options: MutableDataHolder): Unit = {}

  override def extend(rendererBuilder: HtmlRenderer.Builder, rendererType: String): Unit = {
    if (rendererBuilder.isRendererType("HTML")) {
      rendererBuilder.nodeRendererFactory(_ => new SuppressImagesRenderer)
    }
  }
}

class SuppressImagesRenderer extends NodeRenderer {
  override def getNodeRenderingHandlers = {
    Set(new NodeRenderingHandler[Image](classOf[Image], (node, _, html) => {
      val altText = new TextCollectingVisitor().collectAndGetText(node)

      html
        .withAttr()
        .attr("href", node.getUrl)
        .attr("rel", "nofollow")
        .tag("a")
        .text(altText)
        .closeTag("a")
    }), new NodeRenderingHandler[ImageRef](classOf[ImageRef], (node, _, html) => {
      val altText = new TextCollectingVisitor().collectAndGetText(node)

      html.text(altText)
    })
    ).asJava.asInstanceOf[java.util.Set[NodeRenderingHandler[_]]]
  }
}

