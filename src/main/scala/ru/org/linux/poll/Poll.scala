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
package ru.org.linux.poll

import io.circe.{Decoder, Encoder}

import java.util
import io.circe.generic.semiauto.*

import scala.beans.{BeanProperty, BooleanBeanProperty}
import scala.collection.immutable.TreeMap
import scala.jdk.CollectionConverters.*

object Poll {
  val MaxPollSize = 15
  val OrderId = 1
  val OrderVotes = 2

  def apply(id: Int, topic: Int, multiSelect: Boolean, variants: java.util.List[PollVariant]): Poll =
    Poll(id, topic, multiSelect, variants.asScala.toSeq)

  implicit val encoder: Encoder[Poll] = deriveEncoder[Poll]
  implicit val decoder: Decoder[Poll] = deriveDecoder[Poll]
}

case class Poll(@BeanProperty id: Int, @BeanProperty topic: Int, @BooleanBeanProperty multiSelect: Boolean,
                variants: Seq[PollVariant]) {
  def getVariants: java.util.List[PollVariant] = variants.asJava

  def isUserVotePossible: Boolean = {
    for (p <- variants) {
      if (p.getUserVoted>0) return false
    }
    true
  }
}

object PollVariant {
  def toMap(list: java.lang.Iterable[PollVariant]): util.Map[Integer, String] =
    list.asScala.map(v => Integer.valueOf(v.id) -> v.label).to(TreeMap).asJava

  implicit val encoder: Encoder[PollVariant] = deriveEncoder[PollVariant]
  implicit val decoder: Decoder[PollVariant] = deriveDecoder[PollVariant]
}

case class PollVariant(@BeanProperty id: Int, @BeanProperty label: String,@BeanProperty userVoted: Int)