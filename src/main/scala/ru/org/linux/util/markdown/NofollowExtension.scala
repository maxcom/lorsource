package ru.org.linux.util.markdown

import com.vladsch.flexmark.ast.{AutoLink, Link, Node}
import com.vladsch.flexmark.html.renderer.{AttributablePart, LinkResolverContext}
import com.vladsch.flexmark.html.{AttributeProvider, HtmlRenderer, IndependentAttributeProviderFactory}
import com.vladsch.flexmark.util.html.Attributes
import com.vladsch.flexmark.util.options.MutableDataHolder

class NofollowExtension extends HtmlRenderer.HtmlRendererExtension {
  override def rendererOptions(options: MutableDataHolder): Unit = {}

  override def extend(rendererBuilder: HtmlRenderer.Builder, rendererType: String): Unit = {
    rendererBuilder.attributeProviderFactory(new IndependentAttributeProviderFactory {
      override def create(context: LinkResolverContext): AttributeProvider =
        new NofollowAttributeProvider
    })
  }
}

class NofollowAttributeProvider extends AttributeProvider {
  override def setAttributes(node: Node, part: AttributablePart, attributes: Attributes): Unit = {
    node match {
      case _: Link if part == AttributablePart.LINK ⇒
        attributes.addValue("rel", "nofollow")
      case _: AutoLink if part == AttributablePart.LINK ⇒
        attributes.addValue("rel", "nofollow")
      case _ ⇒
    }
  }
}