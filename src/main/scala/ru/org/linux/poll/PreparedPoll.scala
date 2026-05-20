/*
 * Copyright 1998-2026 Linux.org.ru
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
package ru.org.linux.poll

import ru.org.linux.util.StringUtil

import scala.beans.{BeanProperty, BooleanBeanProperty}
import scala.jdk.CollectionConverters.*

case class PreparedPoll(
    @BeanProperty
    poll: Poll,
    @BeanProperty
    totalOfVotesPerson: Int,
    @BeanProperty
    totalVotes: Int,
    @BooleanBeanProperty
    userVoted: Boolean,
    variants: Seq[PreparedPollVariantResult]):

  def getVariants: java.util.List[PreparedPollVariantResult] = variants.asJava

  def renderPoll: String =
    val sb = new StringBuilder("<table>")
    var total = 0
    for variant <- variants do
      val row = s"<tr><td>${StringUtil.escapeHtml(variant.label)}</td><td>${variant.votes}</td></tr>"
      sb.append(row)
      total += variant.votes
    sb.append(s"<tr><td colspan=2>Всего голосов: $total</td></tr>")
    if poll.multiSelect then
      sb.append(s"<tr><td colspan=2>Всего проголосовавших: $totalOfVotesPerson</td></tr>")
    sb.append("</table>")
    sb.toString

object PreparedPoll:
  def apply(poll: Poll, totalOfVotesPerson: Int, variants1: java.util.List[PollVariantResult]): PreparedPoll =
    val scalaVariants = variants1.asScala.toSeq
    apply(poll, totalOfVotesPerson, scalaVariants)

  def apply(poll: Poll, totalOfVotesPerson: Int, variants1: Seq[PollVariantResult]): PreparedPoll =
    val userVoted = variants1.exists(_.userVoted)

    val totalVotes = variants1.map(_.votes).sum
    val maxVotes = variants1.map(_.votes).maxOption.getOrElse(0)

    val divisor =
      if totalOfVotesPerson != 0 then
        totalOfVotesPerson
      else
        totalVotes

    val preparedVariants = variants1.map { variant =>
      val (variantWidth, variantPercent, percentage) =
        if divisor != 0 then
          val vw = 320 * variant.votes / maxVotes
          val vp = vw / 16 * 16 * 100 / 320
          val pct = Math.round(100.0 * variant.votes / divisor).toInt
          (vw, vp, pct)
        else
          (0, 0, 0)

      PreparedPollVariantResult(
        id = variant.id,
        label = variant.label,
        votes = variant.votes,
        userVoted = variant.userVoted,
        percentage = percentage,
        width = variantWidth,
        penguinPercent = variantPercent,
        alt = "*" * variantWidth
      )
    }

    PreparedPoll(poll, totalOfVotesPerson, totalVotes, userVoted, preparedVariants)
