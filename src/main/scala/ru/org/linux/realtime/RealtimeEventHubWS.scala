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

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props, SupervisorStrategy, Terminated}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.StrictLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.stereotype.Service
import org.springframework.web.socket.config.annotation.{EnableWebSocket, WebSocketConfigurer, WebSocketHandlerRegistry}
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.web.socket.{CloseStatus, PingMessage, TextMessage, WebSocketSession}
import ru.org.linux.comment.CommentService
import ru.org.linux.realtime.RealtimeEventHub.{NewComment, Tick}
import ru.org.linux.realtime.RealtimeEventHubWS.{SessionTerminated, Subscribe}
import ru.org.linux.spring.SiteConfig
import ru.org.linux.topic.TopicDao

import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.control.NonFatal

// TODO ignore list support
// TODO fix face conditions on simultaneous posting comment, subscription and missing processing
class RealtimeEventHubWS extends Actor with ActorLogging {
  private val data = new mutable.HashMap[Int, mutable.Set[ActorRef]] with mutable.MultiMap[Int, ActorRef]
  private val sessions = new mutable.HashMap[String, ActorRef]
  private var maxDataSize: Int = 0

  private implicit val ec = context.dispatcher

  context.system.scheduler.schedule(5.minutes, 5.minutes, self, Tick)

  override def supervisorStrategy = SupervisorStrategy.stoppingStrategy

  override def receive: Receive = {
    case Subscribe(session, _) if sessions.contains(session.getId) ⇒
      log.warning(s"Session ${session.getId} already subscribed")
    case Subscribe(session, topic) ⇒
      val actor = context.actorOf(RealtimeSessionActor.props(session))

      context.watch(actor)

      data.addBinding(topic, actor)
      sessions += (session.getId -> actor)

      val dataSize = context.children.size

      if (dataSize > maxDataSize) {
        maxDataSize = dataSize
      }

      sender() ! NotUsed
    case Terminated(actorRef) ⇒
      log.debug(s"RealtimeSessionActor $actorRef terminated")

      data.find(_._2.contains(actorRef)) match {
        case Some((msgid, _)) ⇒
          log.debug(s"Removed $actorRef")
          data.removeBinding(msgid, actorRef)
        case None ⇒
          log.warning(s"Unknown actor was terminated $actorRef")
      }

      sessions.find(_._2 == actorRef).foreach { f ⇒
        sessions.remove(f._1)
      }
    case SessionTerminated(id) ⇒
      sessions.get(id) foreach { actor ⇒
        log.debug("Session was terminated, stopping actor")

        actor ! PoisonPill
      }
    case msg@NewComment(msgid, _) ⇒
      log.debug(s"New comment in topic $msgid")

      data.getOrElse(msgid, Set.empty).foreach {
        _ ! msg
      }
    case Tick ⇒
      log.info(s"Realtime hub: maximum number connections was $maxDataSize")
      maxDataSize = 0
  }
}

object RealtimeEventHubWS {
  case class Subscribe(session: WebSocketSession, topic: Int)
  case class SessionTerminated(session: String)

  def props = Props(classOf[RealtimeEventHubWS])
}

class RealtimeSessionActor(session: WebSocketSession) extends Actor with ActorLogging {
  private implicit val ec = context.dispatcher
  private val schedule = context.system.scheduler.schedule(5.seconds, 1.minute, self, Tick)

  private def notifyComment(comment: Int) = {
    session.sendMessage(new TextMessage(comment.toString))
  }

  override def receive: Receive = {
    case NewComment(_, cid) ⇒
      try {
        notifyComment(cid)
      } catch handleExceptions

    case Tick ⇒
      log.debug("Sending keepalive")
      try {
        session.sendMessage(new PingMessage())
      } catch handleExceptions
  }

  private def handleExceptions: PartialFunction[Throwable, Unit] = {
    case ex: IOException ⇒
      log.debug(s"Terminated by IOException ${ex.toString}")
      context.stop(self)
  }

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    schedule.cancel()
    session.close()
  }
}

object RealtimeSessionActor {
  def props(session: WebSocketSession) = Props(classOf[RealtimeSessionActor], session)
}

@Service
class RealtimeWebsocketHandler(@Qualifier("realtimeHubWS") hub: ActorRef,
                               topicDao: TopicDao, commentService: CommentService) extends TextWebSocketHandler
  with StrictLogging {

  private implicit val Timeout: Timeout = 30.seconds

  override def afterConnectionEstablished(session: WebSocketSession): Unit = {
    logger.debug(s"Connected!")
  }

  override def handleTextMessage(session: WebSocketSession, message: TextMessage): Unit = {
    try {
      val request = message.getPayload

      logger.debug(s"Got request: $request")

      val (topicId, maybeComment) = request.split(" ", 2) match {
        case Array(t) ⇒
          t.toInt -> None
        case Array(t, comment) ⇒
          t.toInt -> Some(comment.toInt)
      }

      val topic = topicDao.getById(topicId)

      val last = maybeComment.getOrElse(0)

      val comments = commentService.getCommentList(topic, false)

      val missed = comments.getList.asScala.map(_.getId).dropWhile(_ <= last).toVector

      missed.foreach { cid ⇒
        logger.debug(s"Sending missed comment $cid")
        session.sendMessage(new TextMessage(cid.toString))
      }

      val result = hub ? Subscribe(session, topic.getId)

      Await.result(result, 10.seconds)
    } catch {
      case NonFatal(e) ⇒
        logger.warn("WS request failed", e)
        session.close(CloseStatus.SERVER_ERROR)
    }
  }

  override def afterConnectionClosed(session: WebSocketSession, status: CloseStatus): Unit = {
    logger.debug(s"Session terminated with status $status")

    hub ! SessionTerminated(session.getId)
  }
}

@Configuration
class RealtimeConfigurationBeans(actorSystem: ActorSystem) {
  @Bean(Array("realtimeHubWS"))
  def hub = actorSystem.actorOf(RealtimeEventHubWS.props)
}

@Configuration
@EnableWebSocket
class RealtimeConfigurationWS(handler: RealtimeWebsocketHandler, config: SiteConfig) extends WebSocketConfigurer {
  override def registerWebSocketHandlers(registry: WebSocketHandlerRegistry): Unit = {
    registry.addHandler(handler, "/ws").setAllowedOrigins(config.getMainUrl, config.getSecureUrl)
  }
}