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

import com.vladsch.flexmark.ast.AnchorRefTarget
import com.vladsch.flexmark.ast.Paragraph
import com.vladsch.flexmark.ast.ParagraphContainer
import com.vladsch.flexmark.ast.util.TextCollectingVisitor
import com.vladsch.flexmark.util.ast.{Block, Node}
import com.vladsch.flexmark.util.sequence.BasedSequence

class CutNode extends Block with ParagraphContainer with AnchorRefTarget {
  private var openingMarker = BasedSequence.NULL
  private var openingTrailing = BasedSequence.NULL
  private var closingMarker = BasedSequence.NULL
  private var closingTrailing = BasedSequence.NULL
  private var anchorRefId = ""

  override def getAstExtra(out: java.lang.StringBuilder): Unit = {
    Node.segmentSpanChars(out, openingMarker, "open")
    Node.segmentSpanChars(out, openingTrailing, "openTrail")
    Node.segmentSpanChars(out, closingMarker, "close")
    Node.segmentSpanChars(out, closingTrailing, "closeTrail")
  }

  override def getSegments: Array[BasedSequence] = Array[BasedSequence](openingMarker, openingTrailing, closingMarker, closingTrailing)

  override def isParagraphEndWrappingDisabled(node: Paragraph): Boolean = (node eq getLastChild) || node.getNext.isInstanceOf[CutNode]

  override def isParagraphStartWrappingDisabled(node: Paragraph): Boolean = (node eq getFirstChild) || node.getPrevious.isInstanceOf[CutNode]

  def setOpeningMarker(openingMarker: BasedSequence): Unit = this.openingMarker = openingMarker

  def setClosingMarker(closingMarker: BasedSequence): Unit = this.closingMarker = closingMarker

  def setOpeningTrailing(openingTrailing: BasedSequence): Unit = this.openingTrailing = openingTrailing

  def setClosingTrailing(closingTrailing: BasedSequence): Unit = this.closingTrailing = closingTrailing

  override def getAnchorRefText: String = "cut"

  override def getAnchorRefSegments: Array[BasedSequence] = new TextCollectingVisitor().collectAndGetSegments(this)

  override def getAnchorRefId: String = anchorRefId

  override def setAnchorRefId(anchorRefId: String): Unit = this.anchorRefId = anchorRefId
}