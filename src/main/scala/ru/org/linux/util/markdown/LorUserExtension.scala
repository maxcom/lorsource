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

import java.util
import java.util.regex.Pattern

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.renderer.{LinkType, NodeRenderer, NodeRenderingHandler}
import com.vladsch.flexmark.parser.{InlineParser, InlineParserExtension, InlineParserExtensionFactory, Parser}
import com.vladsch.flexmark.util.ast.{DoNotDecorate, Node}
import com.vladsch.flexmark.util.options.MutableDataHolder
import com.vladsch.flexmark.util.sequence.BasedSequence
import ru.org.linux.user.UserService
import ru.org.linux.util.formatter.ToHtmlFormatter

import scala.jdk.CollectionConverters.*

class LorUserExtension(userService: UserService, toHtmlFormatter: ToHtmlFormatter) extends Parser.ParserExtension with HtmlRenderer.HtmlRendererExtension {
  override def parserOptions(options: MutableDataHolder): Unit = {}

  override def extend(parserBuilder: Parser.Builder): Unit = {
    parserBuilder.customInlineParserExtensionFactory(new LorUserParserExtension.Factory)
  }

  override def rendererOptions(options: MutableDataHolder): Unit = {}

  override def extend(rendererBuilder: HtmlRenderer.Builder, rendererType: String): Unit = {
    if (rendererBuilder.isRendererType("HTML")) {
      rendererBuilder.nodeRendererFactory(_ => new LorUserRenderer(userService, toHtmlFormatter))
    }
  }
}

object LorUserParserExtension {
  val LorUser: Pattern = Pattern.compile("^(@)([a-z][a-z_\\d-]{0,80})", Pattern.CASE_INSENSITIVE)

  class Factory extends InlineParserExtensionFactory {
    override def getAfterDependents: util.Set[? <: Class[?]] = null

    override def getCharacters: String = "@"

    override def getBeforeDependents: util.Set[? <: Class[?]] = null

    override def create(inlineParser: InlineParser): LorUserParserExtension = new LorUserParserExtension(inlineParser)

    override def affectsGlobalScope = false
  }
}

class LorUser(openingMarker: BasedSequence, text: BasedSequence)
  extends Node(Node.spanningChars(openingMarker, text)) with DoNotDecorate {

  override def getSegments: Array[BasedSequence] = { //return EMPTY_SEGMENTS;
    Array[BasedSequence](openingMarker, text)
  }

  override def getAstExtra(out: java.lang.StringBuilder): Unit = {
    Node.delimitedSegmentSpanChars(out, openingMarker, text, BasedSequence.NULL, "text")
  }
}

class LorUserParserExtension(val inlineParser: InlineParser) extends InlineParserExtension {
  override def finalizeDocument(inlineParser: InlineParser): Unit = {
  }

  override def finalizeBlock(inlineParser: InlineParser): Unit = {
  }

  override def parse(inlineParser: InlineParser): Boolean = {
    val index = inlineParser.getIndex

    val possible = index == 0 || {
      val c = inlineParser.getInput.charAt(index - 1)
      c == ' '
    }

    if (possible) {
      val matches = inlineParser.matchWithGroups(LorUserParserExtension.LorUser)

      if (matches != null) {
        inlineParser.flushTextNode
        val openMarker = matches(1)
        val text = matches(2)
        val gitHubIssue = new LorUser(openMarker, text)
        inlineParser.getBlock.appendChild(gitHubIssue)
        true
      } else {
        false
      }
    } else {
      false
    }
  }
}

class LorUserRenderer(userService: UserService, toHtmlFormatter: ToHtmlFormatter) extends NodeRenderer {
  override def getNodeRenderingHandlers: util.Set[NodeRenderingHandler[? <: Node]] = {
    Set(new NodeRenderingHandler[LorUser](classOf[LorUser], (node, ctx, html) => {
      val nick = node.getChars.subSequence(1).toString

      val maybeUser = userService.findUserCached(nick)

      maybeUser match {
        case Some(user) =>
          val resolvedLink = ctx.resolveLink(LinkType.LINK, toHtmlFormatter.memberURL(user), null)
          val tuxLink = ctx.resolveLink(LinkType.LINK, "/img/tuxlor.png", null)

          html
            .withAttr()
            .attr("style", "white-space: nowrap")
            .tag("span")

          html
            .attr("src", "/img/tuxlor.png")
            .withAttr(tuxLink)
            .attr("alt", "@")
            .attr("title", "@")
            .attr("width", "7")
            .attr("height", "16")
            .tagVoid("img")

          if (user.isBlocked) {
            html.tag("s")
          }

          html
            .attr("style", "text-decoration: none")
            .attr("href", resolvedLink.getUrl)
            .withAttr(resolvedLink)
            .tag("a", false, false, () => html.text(nick))

          if (user.isBlocked) {
            html.closeTag("s")
          }

          html.closeTag("span")
        case _ =>
          html.tag("s")
          html.text(node.getChars.toString)
          html.closeTag("s")
      }
    })).asJava.asInstanceOf[java.util.Set[NodeRenderingHandler[?]]]
  }
}