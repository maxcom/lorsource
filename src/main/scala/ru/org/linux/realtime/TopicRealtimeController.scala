/*
 * Copyright 1998-2016 Linux.org.ru
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
package ru.org.linux.realtime

import java.io.IOException

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ExceptionHandler, PathVariable, RequestMapping}
import org.springframework.web.context.request.async.DeferredResult
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import ru.org.linux.realtime.RealtimeEventHub.GetEmmiterForTopic
import ru.org.linux.util.RichFuture._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

@Controller
class TopicRealtimeController(@Qualifier("realtimeHub") hub: ActorRef) {
  private implicit val Timeout: Timeout = 30.seconds

  @RequestMapping(Array("/{section:(?:forum)|(?:news)|(?:polls)|(?:gallery)}/{group}/{id}/realtime"))
  def subscribe(@PathVariable id: Int): DeferredResult[SseEmitter] = {
    (hub ? GetEmmiterForTopic(id)).mapTo[SseEmitter].toDeferredResult
  }

  @ExceptionHandler(Array(classOf[IOException]))
  def exceptionHandler(e: IOException): Object = {
    /* spring is crazy sometimes
       http://mtyurt.net/2016/04/18/spring-how-to-handle-ioexception-broken-pipe/
    */
    null
  }
}
