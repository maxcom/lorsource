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

import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.typographic.TypographicExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.{NodeVisitor, VisitHandler}
import com.vladsch.flexmark.util.options.MutableDataSet
import org.jsoup.Jsoup
import org.jsoup.internal.StringUtil
import org.jsoup.nodes.{CDataNode, Document, Element, Node, TextNode}
import org.jsoup.select.{NodeTraversor, NodeVisitor as JsoupNodeVisitor}
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import ru.org.linux.comment.CommentDao
import ru.org.linux.spring.SiteConfig
import ru.org.linux.topic.TopicDao
import ru.org.linux.user.{User, UserService}
import ru.org.linux.util.formatter.ToHtmlFormatter

import javax.annotation.Nullable
import java.lang.StringBuilder as JStringBuilder
import scala.jdk.CollectionConverters.*
import scala.collection.mutable

@Service
@Qualifier("flexmark")
class FlexmarkMarkdownFormatter(siteConfig: SiteConfig, topicDao: TopicDao, commentDao: CommentDao,
                                userService: UserService, toHtmlFormatter: ToHtmlFormatter) extends MarkdownFormatter {
  private def options(nofollow: Boolean, minimizeCut: Boolean, cutUrl: Option[String] = None) = {
    val options = new MutableDataSet

    val extensions = Seq(TablesExtension.create, StrikethroughExtension.create, AutolinkExtension.create(),
      TypographicExtension.create(), new SuppressImagesExtension,
      new LorLinkExtension(siteConfig, topicDao, commentDao), new LorUserExtension(userService, toHtmlFormatter),
      new CutExtension, new FencedCodeExtension/*, YouTubeLinkExtension.create()*/)

    val allExtensions = (if (nofollow) {
      extensions :+ new NofollowExtension
    } else {
      extensions
    }).asJava

    options.set(Parser.EXTENSIONS, allExtensions)

    options.set(HtmlRenderer.SUPPRESSED_LINKS, "javascript:.*")
    options.set(HtmlRenderer.SUPPRESS_HTML, Boolean.box(true))
    options.set(HtmlRenderer.FENCED_CODE_NO_LANGUAGE_CLASS, "no-highlight")

    options.set(HtmlRenderer.CODE_STYLE_HTML_OPEN, "<span class=\"code\"><code>")
    options.set(HtmlRenderer.CODE_STYLE_HTML_CLOSE, "</code></span>")

    options.set(TypographicExtension.DOUBLE_QUOTE_OPEN, "&laquo;")
    options.set(TypographicExtension.DOUBLE_QUOTE_CLOSE, "&raquo;")

    options.set(CutExtension.CutCollapsed, minimizeCut)

    cutUrl foreach { url =>
      options.set(CutExtension.CutLink, url)
    }

    // uncomment to convert soft-breaks to hard breaks
    //options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

    options.toImmutable
  }

  private val parser = Parser.builder(options(nofollow = false, minimizeCut = false)).build
  private val renderer = HtmlRenderer.builder(options(nofollow = false, minimizeCut = false)).build
  private val rendererNofollow = HtmlRenderer.builder(options(nofollow = true, minimizeCut = false)).build

  override def renderToHtml(content: String, nofollow: Boolean): String = {
    // You can re-use parser and renderer instances
    val document = parser.parse(content)

    (if (nofollow) {
      rendererNofollow
    } else {
      renderer
    }).render(document)
  }


  override def renderWithMinimizedCut(content: String, nofollow: Boolean, canonicalUrl: String): String = {
    val document = parser.parse(content)

    val renderer = HtmlRenderer.builder(options(nofollow = false, minimizeCut = true, cutUrl = Some(canonicalUrl))).build

    renderer.render(document)
  }

  override def mentions(content: String): Set[User] = {
    val document = parser.parse(content)

    val mentions = mutable.Set[String]()

    val visitor = new NodeVisitor(new VisitHandler[LorUser](classOf[LorUser], (node: LorUser) => {
      mentions.add(node.getChars.subSequence(1).toString)
    }))

    visitor.visit(document)

    mentions.toSet.flatMap(userService.findUserCached)
  }

  def renderToText(content: String): String = {
    text(Jsoup.parse(renderToHtml(content, nofollow = false)))
  }

  private def text(doc: Document): String = {
    val accum = StringUtil.borrowBuilder
    NodeTraversor.traverse(new FormattingVisitor(accum), doc)
    StringUtil.releaseBuilder(accum).trim
  }


  private class FormattingVisitor(accum: JStringBuilder) extends JsoupNodeVisitor {
    private def lastCharIsWhitespace(sb: JStringBuilder) = !sb.isEmpty && sb.charAt(sb.length - 1) == ' '

    private def appendNormalisedText(accum: JStringBuilder, textNode: TextNode): Unit = {
      val text = textNode.getWholeText

      if (preserveWhitespace(textNode.parentNode) || textNode.isInstanceOf[CDataNode]) {
        accum.append(text)
      } else {
        StringUtil.appendNormalisedWhitespace(accum, text, lastCharIsWhitespace(accum))
      }
    }

    private def preserveWhitespace(@Nullable node: Node): Boolean = {
      // looks only at this element and five levels up, to prevent recursion & needless stack searches
      node match {
        case element: Element =>
          var el = element
          var i = 0
          do {
            if (el.tag.preserveWhitespace) {
              return true
            }

            el = el.parent
            i += 1
          } while (i < 6 && el != null)
        case _ =>
      }
      false
    }

    override def head(node: Node, depth: Int): Unit = {
      node match {
        case textNode: TextNode =>
          appendNormalisedText(accum, textNode)
        case element: Element =>
          if (element.nameIs("blockquote")) {
            accum.append('«')
          } else if (!accum.isEmpty && (element.isBlock || element.nameIs("br")) && !lastCharIsWhitespace(accum)) {
            accum.append(' ')
          }
        case _ =>
      }
    }

    override def tail(node: Node, depth: Int): Unit = {
      // make sure there is a space between block tags and immediately following text nodes or inline elements <div>One</div>Two should be "One Two".
      node match {
        case element: Element =>
          val next = node.nextSibling
          if (element.nameIs("blockquote")) {
            accum.append("» ")
          } else if (element.nameIs("a")) {
            val link = node.attr("href")

            if (accum.length() < link.length || !accum.subSequence(accum.length() - link.length, accum.length()).equals(link)) {
              accum.append(" " + link + " ")
            }
          } else if (element.isBlock &&
            (next.isInstanceOf[TextNode] || next.isInstanceOf[Element] && !next.asInstanceOf[Element].tag.formatAsBlock) &&
            !lastCharIsWhitespace(accum)) {
              accum.append(' ')
          }
        case _ =>
      }
    }
  }
}