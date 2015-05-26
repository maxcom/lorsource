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

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import ru.org.linux.markdown.MarkdownRenderActor.{RenderFailure, RenderResult, RenderedText}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class MarkdownRenderService(renderActor:ActorRef) {
  private implicit val askTimeout:Timeout = 2.seconds

  def render(text:String):Future[String] = (renderActor ? MarkdownRenderActor.Render(text)).mapTo[RenderResult].flatMap {
    case RenderedText(rendered) ⇒ Future.successful(rendered)
    case RenderFailure(ex)      ⇒ Future.failed(ex)
  }
}