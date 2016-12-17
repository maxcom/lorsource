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

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props, SupervisorStrategy, Terminated}
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.http.MediaType
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import ru.org.linux.realtime.RealtimeEventHub.{GetEmmiterForTopic, NewComment, Tick}

import scala.collection.mutable
import scala.concurrent.duration._

/*
  TODO: load messages from last id
  TODO: support for ignore list
  TODO: support for old cached pages
 */

class RealtimeEventHub extends Actor with ActorLogging {
  private val data = new mutable.HashMap[Int, mutable.Set[ActorRef]] with mutable.MultiMap[Int, ActorRef]

  override def supervisorStrategy = SupervisorStrategy.stoppingStrategy

  override def receive: Receive = {
    case msg@GetEmmiterForTopic(msgid) ⇒
      val actor = context.actorOf(TopicEmitterActor.props(msgid))

      context.watch(actor)

      data.addBinding(msgid, actor)

      actor.forward(msg)
    case Terminated(actorRef) ⇒
      log.debug(s"TopicEmmitterActor $actorRef terminated")

      data.find(_._2.contains(actorRef)) match {
        case Some((msgid, _)) ⇒
          log.debug(s"Removed $actorRef")
          data.removeBinding(msgid, actorRef)
        case None ⇒
          log.warning(s"Unknown actor was terminated $actorRef")
      }
    case msg@NewComment(msgid, _) ⇒
      log.debug(s"New comment in topic $msgid")

      data.getOrElse(msgid, Set.empty).foreach {
        _ ! msg
      }
  }
}

object RealtimeEventHub {
  case class GetEmmiterForTopic(msgid: Int)
  case class NewComment(msgid: Int, cid: Int)
  case object Tick

  def props = Props(classOf[RealtimeEventHub])
}

class TopicEmitterActor(msgid: Int) extends Actor with ActorLogging {
  private val emitter = new SseEmitter(30.minutes.toMillis) {
    override def extendResponse(outputMessage: ServerHttpResponse) = {
      super.extendResponse(outputMessage)

      val headers = outputMessage.getHeaders

      headers.add("Connection", "close")
      headers.add("X-Accel-Buffering", "no")
    }
  }

  implicit val ec = context.dispatcher

  val schedule = context.system.scheduler.schedule(5.seconds, 15.seconds, self, Tick)

  context.setReceiveTimeout(30.minutes)

  emitter.onCompletion(() ⇒ {
    log.debug(s"Emitter done, sending poison pill to $self")
    self ! PoisonPill
  })

  override def receive: Receive = {
    case GetEmmiterForTopic(_) ⇒
      context.sender() ! emitter

    case NewComment(_, cid) ⇒
      try {
        emitter.send(SseEmitter.event().name("comment").id(cid.toString).data(cid.toString, MediaType.TEXT_PLAIN))
      } catch handleExceptions

    case Tick ⇒
      try {
        emitter.send(SseEmitter.event().comment("keepalive"))
      } catch handleExceptions
  }

  private def handleExceptions: PartialFunction[Throwable, Unit] = {
    case ex: IOException ⇒
      log.debug(s"Terminated by IOException ${ex.toString}")
      context.stop(self)
    case ex: IllegalStateException ⇒
      log.debug(s"Terminated by ISE ${ex.toString}")
      context.stop(self)
  }

  override def postStop() = {
    log.debug("Terminated")
    emitter.complete()
    schedule.cancel()
  }
}

object TopicEmitterActor {
  def props(msgid: Int) = Props(classOf[TopicEmitterActor], msgid)
}

@Configuration
class RealtimeConfiguration(actorSystem: ActorSystem) {
  @Bean(Array("realtimeHub"))
  def realtimeHub() = actorSystem.actorOf(RealtimeEventHub.props)

}