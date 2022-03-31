/*
 * Copyright 1998-2022 Linux.org.ru
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

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props, SupervisorStrategy, Terminated, Timers}
import akka.pattern.ask
import akka.util.Timeout
import com.google.common.collect.ImmutableList
import com.typesafe.scalalogging.StrictLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.security.authentication.RememberMeAuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.web.socket.config.annotation.{EnableWebSocket, WebSocketConfigurer, WebSocketHandlerRegistry}
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.web.socket.{CloseStatus, PingMessage, TextMessage, WebSocketSession}
import ru.org.linux.auth.UserDetailsImpl
import ru.org.linux.comment.{CommentList, CommentReadService}
import ru.org.linux.realtime.RealtimeEventHub._
import ru.org.linux.spring.SiteConfig
import ru.org.linux.topic.{TopicDao, TopicPermissionService}

import java.io.IOException
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

// TODO ignore list support
// TODO fix face conditions on simultaneous posting comment, subscription and missing processing
class RealtimeEventHub extends Actor with ActorLogging with Timers {
  private val topicSubscriptions: mutable.MultiDict[Int, ActorRef] = mutable.MultiDict[Int, ActorRef]()
  private val userSubscriptions: mutable.MultiDict[Int, ActorRef] = mutable.MultiDict[Int, ActorRef]()
  private val sessions = new mutable.HashMap[String, ActorRef]
  private var maxDataSize: Int = 0

  timers.startTimerWithFixedDelay(Tick, Tick, 5.minutes)

  override def supervisorStrategy = SupervisorStrategy.stoppingStrategy

  override def receive: Receive = {
    case SessionStarted(session, user) if !sessions.contains(session.getId) =>
      val actor = context.actorOf(RealtimeSessionActor.props(session))
      context.watch(actor)

      sessions += (session.getId -> actor)

      user.foreach { user =>
        userSubscriptions += (user -> actor)
      }

      val dataSize = context.children.size

      if (dataSize > maxDataSize) {
        maxDataSize = dataSize
      }

      sender() ! Done
    case SubscribeTopic(session, topic) if sessions.contains(session.getId) =>
      val actor = sessions(session.getId)

      topicSubscriptions += (topic -> actor)

      sender() ! Done
    case Terminated(actorRef) =>
      log.debug(s"RealtimeSessionActor $actorRef terminated")

      topicSubscriptions.sets.find(_._2.contains(actorRef)).foreach { case (msgid, _) =>
        topicSubscriptions -= (msgid -> actorRef)
      }

      userSubscriptions.sets.find(_._2.contains(actorRef)).foreach { case (user, _) =>
        userSubscriptions -= (user -> actorRef)
      }

      sessions.find(_._2 == actorRef).foreach { f =>
        log.debug(s"Removed $actorRef")
        sessions.remove(f._1)
      }
    case SessionTerminated(id) =>
      sessions.get(id) foreach { actor =>
        log.debug("Session was terminated, stopping actor")

        actor ! PoisonPill
      }
    case msg@NewComment(msgid, _) =>
      log.debug(s"New comment in topic $msgid")

      topicSubscriptions.sets.getOrElse(msgid, Set.empty).foreach {
        _ ! msg
      }
    case Tick =>
      log.info(s"Realtime hub: maximum number connections was $maxDataSize")
      maxDataSize = 0
  }
}

object RealtimeEventHub {
  case class NewComment(msgid: Int, cid: Int)
  case object Tick

  case class SessionStarted(session: WebSocketSession, user: Option[Int])
  case class SubscribeTopic(session: WebSocketSession, topic: Int)
  case class SessionTerminated(session: String)

  def props: Props = Props(new RealtimeEventHub())

  def notifyComment(session: WebSocketSession, comment: Int): Unit = {
    session.sendMessage(new TextMessage(s"comment $comment"))
  }
}

class RealtimeSessionActor(session: WebSocketSession) extends Actor with ActorLogging with Timers {
  private implicit val ec: ExecutionContext = context.dispatcher
  timers.startTimerWithFixedDelay(Tick, Tick, initialDelay = 5.seconds, delay = 1.minute)

  override def receive: Receive = {
    case NewComment(_, cid) =>
      try {
        notifyComment(session, cid)
      } catch handleExceptions

    case Tick =>
      log.debug("Sending keepalive")
      try {
        session.sendMessage(new PingMessage())
      } catch handleExceptions
  }

  private def handleExceptions: PartialFunction[Throwable, Unit] = {
    case ex: IOException =>
      log.debug(s"Terminated by IOException ${ex.toString}")
      context.stop(self)
  }

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    session.close()
  }
}

object RealtimeSessionActor {
  def props(session: WebSocketSession) = Props(new RealtimeSessionActor(session))
}

@Service
class RealtimeWebsocketHandler(@Qualifier("realtimeHubWS") hub: ActorRef,
                               topicDao: TopicDao, commentService: CommentReadService) extends TextWebSocketHandler
  with StrictLogging {

  private implicit val Timeout: Timeout = 30.seconds

  override def afterConnectionEstablished(session: WebSocketSession): Unit = {
    try {
      val currentUser =
        Option(session.getPrincipal)
          .collect { case token: RememberMeAuthenticationToken if token.isAuthenticated => token.getPrincipal }
          .collect { case user: UserDetailsImpl => user.getUser }

      logger.debug(s"Connected! currentUser=${currentUser.map(_.getNick)}")

      val result = hub ? SessionStarted(session, currentUser.map(_.getId))

      Await.result(result, 10.seconds)
    } catch {
      case NonFatal(e) =>
        logger.warn("WS request failed", e)
        session.close(CloseStatus.SERVER_ERROR)
    }
  }

  override def handleTextMessage(session: WebSocketSession, message: TextMessage): Unit = {
    try {
      val request = message.getPayload

      logger.debug(s"Got request: $request")

      val (topicId, maybeComment) = request.split(" ", 2) match {
        case Array(t) =>
          t.toInt -> None
        case Array(t, comment) =>
          t.toInt -> Some(comment.toInt)
      }

      val topic = topicDao.getById(topicId)

      val last = maybeComment.getOrElse(0)

      val comments = if (topic.getPostscore != TopicPermissionService.POSTSCORE_HIDE_COMMENTS) {
        commentService.getCommentList(topic, false)
      } else {
        new CommentList(ImmutableList.of(), 0)
      }

      val missed = comments.getList.asScala.map(_.getId).dropWhile(_ <= last).toVector

      missed.foreach { cid =>
        logger.debug(s"Sending missed comment $cid")
        notifyComment(session, cid)
      }

      val result = hub ? SubscribeTopic(session, topic.getId)

      Await.result(result, 10.seconds)
    } catch {
      case NonFatal(e) =>
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
  def hub: ActorRef = actorSystem.actorOf(RealtimeEventHub.props)
}

@Configuration
@EnableWebSocket
class RealtimeConfigurationWS(handler: RealtimeWebsocketHandler, config: SiteConfig) extends WebSocketConfigurer {
  override def registerWebSocketHandlers(registry: WebSocketHandlerRegistry): Unit = {
    registry.addHandler(handler, "/ws").setAllowedOrigins(config.getSecureUrl)
  }
}