/*
 * Copyright 1998-2015 Linux.org.ru
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

package ru.org.linux.markdown

import akka.actor.{Actor, Props}
import org.pegdown.{Extensions, PegDownProcessor}
import ru.org.linux.markdown.MarkdownRenderActor._

class MarkdownRenderActor extends Actor {
  // processor with HTML support - not for user input
  private lazy val nonsafeProcessor =
    new PegDownProcessor(Extensions.STRIKETHROUGH, PegDownProcessor.DEFAULT_MAX_PARSING_TIME)

  override def receive: Receive = {
    // TODO handle exception of PegDownProcessor
    case Render(text) ⇒ sender() ! nonsafeProcessor.markdownToHtml(text)
  }
}

object MarkdownRenderActor {
  case class Render(text:String)

  def props = Props[MarkdownRenderActor]()
}
