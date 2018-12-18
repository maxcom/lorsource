package ru.org.linux.util.markdown

import java.util
import java.util.regex.Pattern

import com.vladsch.flexmark.ast.{CustomNode, DoNotDecorate, Node}
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.renderer.{LinkType, NodeRenderer, NodeRenderingHandler}
import com.vladsch.flexmark.parser.{InlineParser, InlineParserExtension, InlineParserExtensionFactory, Parser}
import com.vladsch.flexmark.util.options.MutableDataHolder
import com.vladsch.flexmark.util.sequence.BasedSequence
import ru.org.linux.user.{UserNotFoundException, UserService}
import ru.org.linux.util.formatter.ToHtmlFormatter

import scala.collection.JavaConverters._

class LorUserExtension(userService: UserService, toHtmlFormatter: ToHtmlFormatter) extends Parser.ParserExtension with HtmlRenderer.HtmlRendererExtension {
  override def parserOptions(options: MutableDataHolder): Unit = {}

  override def extend(parserBuilder: Parser.Builder): Unit = {
    parserBuilder.customInlineParserExtensionFactory(new LorUserParserExtension.Factory)
  }

  override def rendererOptions(options: MutableDataHolder): Unit = {}

  override def extend(rendererBuilder: HtmlRenderer.Builder, rendererType: String): Unit = {
    if (rendererBuilder.isRendererType("HTML")) {
      rendererBuilder.nodeRendererFactory(_ ⇒ new LorUserRenderer(userService, toHtmlFormatter))
    }
  }
}

object LorUserParserExtension {
  val LorUser: Pattern = Pattern.compile("^(@)([a-z\\d](?:[a-z\\d]|-(?=[a-z\\d])){0,80})\\b", Pattern.CASE_INSENSITIVE)

  class Factory extends InlineParserExtensionFactory {
    override def getAfterDependents = null

    override def getCharacters = "@"

    override def getBeforeDependents = null

    override def create(inlineParser: InlineParser) = new LorUserParserExtension(inlineParser)

    override def affectsGlobalScope = false
  }
}

class LorUser(openingMarker: BasedSequence, text: BasedSequence)
  extends CustomNode(Node.spanningChars(openingMarker, text)) with DoNotDecorate {

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
  }
}

class LorUserRenderer(userService: UserService, toHtmlFormatter: ToHtmlFormatter) extends NodeRenderer {
  override def getNodeRenderingHandlers: util.Set[NodeRenderingHandler[_ <: Node]] = {
    Set(new NodeRenderingHandler[LorUser](classOf[LorUser], (node, ctx, html) => {
      val nick = node.getChars.subSequence(1).toString

      val maybeUser = try {
        Some(userService.getUserCached(nick))
      } catch {
        case _: UserNotFoundException ⇒
          None
      }

      maybeUser match {
        case Some(user) ⇒
          val resolvedLink = ctx.resolveLink(LinkType.LINK, toHtmlFormatter.memberURL(user), null)

          if (user.isBlocked) {
            html.tag("s")
          }

          html
            .attr("href", resolvedLink.getUrl)
            .withAttr(resolvedLink)
            .tag("a", false, false, () ⇒ html.text(node.getChars.toString))

          if (user.isBlocked) {
            html.closeTag("s")
          }
        case _ ⇒
          html.tag("s")
          html.text(node.getChars.toString)
          html.closeTag("s")
      }
    })).asJava.asInstanceOf[java.util.Set[NodeRenderingHandler[_]]]
  }
}