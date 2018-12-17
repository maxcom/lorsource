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

package ru.org.linux.util.markdown

import com.vladsch.flexmark.ast._
import com.vladsch.flexmark.html.{HtmlRenderer, HtmlWriter}
import com.vladsch.flexmark.html.renderer._
import com.vladsch.flexmark.util.options.MutableDataHolder
import ru.org.linux.spring.SiteConfig
import ru.org.linux.util.LorURL

import scala.collection.JavaConverters._

class LorLinkExtension(siteConfig: SiteConfig) extends HtmlRenderer.HtmlRendererExtension {
  override def rendererOptions(options: MutableDataHolder): Unit = {}

  // TODO поддержка Link, а не только AutoLink
  override def extend(rendererBuilder: HtmlRenderer.Builder, rendererType: String): Unit = {
    if (rendererBuilder.isRendererType("HTML")) {
      rendererBuilder.nodeRendererFactory(_ ⇒ new LorLinkRenderer(siteConfig))
    }
  }
}


class LorLinkRenderer(siteConfig: SiteConfig) extends NodeRenderer {
  override def getNodeRenderingHandlers = Set(new NodeRenderingHandler[AutoLink](classOf[AutoLink], (node, ctx, html) => {
    val url: LorURL = new LorURL(siteConfig.getMainURI, node.getUrl.toString)

    if (url.isTrueLorUrl && !ctx.isDoNotRenderLinks) {
      renderLorUrl(node, html, url, ctx)
    } else {
      ctx.delegateRender()
    }
  })).asJava.asInstanceOf[java.util.Set[NodeRenderingHandler[_]]]

  private def renderLorUrl(node: AutoLink, html: HtmlWriter, url: LorURL, ctx: NodeRendererContext): Unit = {
    val canonical = url.canonize(siteConfig.getSecureURI)

    val resolvedLink = ctx.resolveLink(LinkType.LINK, canonical, null)

    // TODO process topic and comment links

    html.srcPos(node.getText)
      .attr("href", canonical)
      .withAttr(resolvedLink)
      .tag("a", false, false, () ⇒ html.text(url.toString))
  }
}
