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

package ru.org.linux.util.markdown

import com.vladsch.flexmark.ast.*
import com.vladsch.flexmark.html.renderer.*
import com.vladsch.flexmark.html.{HtmlRenderer, HtmlWriter}
import com.vladsch.flexmark.util.options.MutableDataHolder
import org.apache.commons.httpclient.URIException
import ru.org.linux.comment.CommentDao
import ru.org.linux.site.MessageNotFoundException
import ru.org.linux.spring.SiteConfig
import ru.org.linux.topic.TopicDao
import ru.org.linux.util.LorURL

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.RichOptional

class LorLinkExtension(siteConfig: SiteConfig, topicDao: TopicDao, commentDao: CommentDao) extends HtmlRenderer.HtmlRendererExtension {
  override def rendererOptions(options: MutableDataHolder): Unit = {}

  // TODO поддержка Link, а не только AutoLink
  override def extend(rendererBuilder: HtmlRenderer.Builder, rendererType: String): Unit = {
    if (rendererBuilder.isRendererType("HTML")) {
      rendererBuilder.nodeRendererFactory(_ => new LorLinkRenderer(siteConfig, topicDao, commentDao))
    }
  }
}


class LorLinkRenderer(siteConfig: SiteConfig, topicDao: TopicDao, commentDao: CommentDao) extends NodeRenderer {
  override def getNodeRenderingHandlers = Set(new NodeRenderingHandler[AutoLink](classOf[AutoLink], (node, ctx, html) => {
    try {
      val url: LorURL = new LorURL(siteConfig.getMainURI, node.getUrl.toString)

      if (url.isTrueLorUrl && !ctx.isDoNotRenderLinks) {
        renderLorUrl(node, html, url, ctx)
      } else {
        ctx.delegateRender()
      }
    } catch {
      case _: URIException =>
        ctx.delegateRender()
    }
  })).asJava.asInstanceOf[java.util.Set[NodeRenderingHandler[_]]]

  private def renderLorUrl(node: AutoLink, html: HtmlWriter, url: LorURL, ctx: NodeRendererContext): Unit = {
    val canonical = url.canonize(siteConfig.getSecureURI)
    val resolvedLink = ctx.resolveLink(LinkType.LINK, canonical, null)

    def renderLink(): Unit = {
      html.srcPos(node.getText)
        .attr("href", canonical)
        .withAttr(resolvedLink)
        .tag("a", false, false, () => html.text(url.toString))
    }

    if (url.isMessageUrl) {
      topicDao.findById(url.getMessageId).toScala match {
        case Some(message) =>
          val deleted = if (url.isCommentUrl && !message.deleted) {
            try {
              commentDao.getById(url.getCommentId).deleted
            } catch {
              case _: MessageNotFoundException =>
                false
            }
          } else {
            message.deleted
          }

          html.srcPos(node.getText)

          if (deleted) {
            html.tag("s")
          }

          val text = if (deleted) {
            canonical
          } else {
             if (url.isCommentUrl) {
              message.getTitleUnescaped + " (комментарий)"
            } else {
              message.getTitleUnescaped
            }
          }

          html.attr("href", canonical)
          html.withAttr(resolvedLink)
          html.tag("a", false, false, () => html.text(text))

          if (deleted) {
            html.closeTag("s")
          }
        case None =>
          renderLink()
      }
    } else {
      renderLink()
    }
  }
}
