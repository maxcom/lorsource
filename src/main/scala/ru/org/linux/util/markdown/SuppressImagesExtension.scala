package ru.org.linux.util.markdown

import com.vladsch.flexmark.ast.{Image, ImageRef}
import com.vladsch.flexmark.ast.util.TextCollectingVisitor
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.renderer.{NodeRenderer, NodeRenderingHandler}
import com.vladsch.flexmark.util.options.MutableDataHolder

import scala.collection.JavaConverters._

class SuppressImagesExtension extends HtmlRenderer.HtmlRendererExtension {
  override def rendererOptions(options: MutableDataHolder): Unit = {}

  override def extend(rendererBuilder: HtmlRenderer.Builder, rendererType: String): Unit = {
    if (rendererBuilder.isRendererType("HTML")) {
      rendererBuilder.nodeRendererFactory(_ ⇒ new SuppressImagesRenderer)
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

