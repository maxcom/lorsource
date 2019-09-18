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
import java.util.regex.Pattern

import com.vladsch.flexmark.parser.InlineParser
import com.vladsch.flexmark.parser.block._
import com.vladsch.flexmark.util.ast.{Block, BlockContent}
import com.vladsch.flexmark.util.options.DataHolder
import com.vladsch.flexmark.util.sequence.BasedSequence
import scala.jdk.CollectionConverters._

object LorCutParser {
  private val CutStart = Pattern.compile(">>>(\\s*$)")
  private val CutEnd = Pattern.compile("<<<(\\s*$)")

  class Factory extends CustomBlockParserFactory {
    override def getAfterDependents: util.Set[Class[_ <: CustomBlockParserFactory]] = null

    override def getBeforeDependents: util.Set[Class[_ <: CustomBlockParserFactory]] = null

    override def affectsGlobalScope = false

    override def create(options: DataHolder) = new LorCutParser.BlockFactory(options)
  }

  class BlockFactory(options: DataHolder) extends AbstractBlockParserFactory(options) {
    private def haveBlockQuoteParser(state: ParserState): Boolean = {
      val parsers = state.getActiveBlockParsers

      parsers.asScala.reverseIterator.exists(_.isInstanceOf[LorCutParser])
    }

    override def tryStart(state: ParserState, matchedBlockParser: MatchedBlockParser): BlockStart = {
      if (!haveBlockQuoteParser(state)) {
        val line = state.getLineWithEOL
        val matcher = CutStart.matcher(line)
        if (matcher.matches) {
          BlockStart.of(new LorCutParser(line.subSequence(0, 3), line.subSequence(matcher.start(1), matcher.end(1)))).atIndex(state.getLineEndIndex)
        } else {
          BlockStart.none
        }
      } else {
        BlockStart.none
      }
    }
  }

}

class LorCutParser private[markdown](val openMarker: BasedSequence, val openTrailing: BasedSequence) extends AbstractBlockParser {
  final private val block = new CutNode

  block.setOpeningMarker(openMarker)
  block.setOpeningTrailing(openTrailing)

  private var content = new BlockContent
  private var hadClose = false

  override def getBlock: Block = block

  override def tryContinue(state: ParserState): BlockContinue = {
    if (hadClose) return BlockContinue.none
    val index = state.getIndex
    val line = state.getLineWithEOL
    val matcher = LorCutParser.CutEnd.matcher(line.subSequence(index))
    if (!matcher.matches) BlockContinue.atIndex(index)
    else {
      val lastChild = block.getLastChild
      lastChild match {
        case cutNode: CutNode =>
          val parser = state.getActiveBlockParser(cutNode)

          parser match {
            case cutParser: LorCutParser if !cutParser.hadClose =>
              return BlockContinue.atIndex(index)
            case _ =>
          }
        case _ =>
      }
      hadClose = true
      block.setClosingMarker(state.getLine.subSequence(index, index + 3))
      block.setClosingTrailing(state.getLineWithEOL.subSequence(matcher.start(1), matcher.end(1)))
      BlockContinue.atIndex(state.getLineEndIndex)
    }
  }

  override def addLine(state: ParserState, line: BasedSequence): Unit = content.add(line, state.getIndent)

  override def closeBlock(state: ParserState): Unit = {
    block.setContent(content)
    block.setCharsFromContent()
    content = null
  }

  override def isContainer = true

  override def canContain(state: ParserState, blockParser: BlockParser, block: Block) = true

  override def parseInlines(inlineParser: InlineParser): Unit = {}
}