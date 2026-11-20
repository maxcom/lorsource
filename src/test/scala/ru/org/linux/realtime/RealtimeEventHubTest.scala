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

package ru.org.linux.realtime

import munit.FunSuite
import org.apache.pekko.actor.typed.{ActorSystem, Scheduler}
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.util.Timeout
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, never, timeout, times, verify, when}
import org.springframework.web.socket.{TextMessage, WebSocketSession}
import ru.org.linux.user.IgnoreListDao

import scala.concurrent.Await
import scala.concurrent.duration.*

/** Юнит-тесты realtime-хаба: закрытие сессий пользователя и доставка пропущенных комментариев. */
class RealtimeEventHubTest extends FunSuite:
  private given Timeout: Timeout = 10.seconds

  private def startHub(name: String, ignoreListDao: IgnoreListDao = mock(classOf[IgnoreListDao])):
    ActorSystem[RealtimeEventHub.Protocol] =
    ActorSystem(RealtimeEventHub.behavior(ignoreListDao), name)

  private def stopHub(hub: ActorSystem[RealtimeEventHub.Protocol]): Unit =
    hub.terminate()
    Await.ready(hub.whenTerminated, 10.seconds)

  private def mockSession(id: String): WebSocketSession =
    val session = mock(classOf[WebSocketSession])
    when(session.getId).thenReturn(id)
    session

  test("closeUserSessions closes all sessions of the user"):
    val hub = startHub("close-user-sessions-test")
    try
      given Scheduler = hub.scheduler

      val session1 = mockSession("session-1")
      val session2 = mockSession("session-2")

      Await.result(hub.ask(RealtimeEventHub.SessionStarted(session1, Some(42), _)), 10.seconds)
      Await.result(hub.ask(RealtimeEventHub.SessionStarted(session2, Some(42), _)), 10.seconds)

      RealtimeEventHub.closeUserSessions(hub, 42)

      verify(session1, timeout(10000)).close()
      verify(session2, timeout(10000)).close()
    finally
      stopHub(hub)

  test("closeUserSessions does not close sessions of other users"):
    val hub = startHub("close-user-sessions-other-test")
    try
      given Scheduler = hub.scheduler

      val target = mockSession("target")
      val other = mockSession("other")

      Await.result(hub.ask(RealtimeEventHub.SessionStarted(target, Some(42), _)), 10.seconds)
      Await.result(hub.ask(RealtimeEventHub.SessionStarted(other, Some(43), _)), 10.seconds)

      RealtimeEventHub.closeUserSessions(hub, 42)

      verify(target, timeout(10000)).close()
      verify(other, never()).close()
    finally
      stopHub(hub)

  test("closeUserSessions is safe when the user has no sessions"):
    val hub = startHub("close-user-sessions-empty-test")
    try
      given Scheduler = hub.scheduler

      hub ! RealtimeEventHub.CloseUserSessions(999)

      val session = mockSession("session-1")
      Await.result(hub.ask(RealtimeEventHub.SessionStarted(session, Some(1), _)), 10.seconds)

      verify(session, never()).close()
    finally
      stopHub(hub)

  test("anonymous sessions are not affected"):
    val hub = startHub("close-user-sessions-anonymous-test")
    try
      given Scheduler = hub.scheduler

      val anonymous = mockSession("anonymous")

      Await.result(hub.ask(RealtimeEventHub.SessionStarted(anonymous, None, _)), 10.seconds)

      RealtimeEventHub.closeUserSessions(hub, 42)

      verify(anonymous, never()).close()
    finally
      stopHub(hub)

  test("missed comments are delivered as single aggregate frame"):
    val hub = startHub("missed-comments-aggregate-test")
    try
      given Scheduler = hub.scheduler

      val session = mockSession("aggregate")

      Await.result(hub.ask(RealtimeEventHub.SessionStarted(session, None, _)), 10.seconds)
      Await.result(hub.ask(RealtimeEventHub.SubscribeTopic(session, 1, Vector(5, 6, 7), _)), 10.seconds)

      verify(session, timeout(10000)).sendMessage(new TextMessage("comments 5 3"))
      verify(session, times(1)).sendMessage(any[TextMessage]())
    finally
      stopHub(hub)

  test("missed comments aggregate excludes ignored comments"):
    val ignoreListDao = mock(classOf[IgnoreListDao])
    when(ignoreListDao.isIgnored(42, 6)).thenReturn(true)

    val hub = startHub("missed-comments-ignored-test", ignoreListDao)
    try
      given Scheduler = hub.scheduler

      val session = mockSession("ignored")

      Await.result(hub.ask(RealtimeEventHub.SessionStarted(session, Some(42), _)), 10.seconds)
      Await.result(hub.ask(RealtimeEventHub.SubscribeTopic(session, 1, Vector(5, 6, 7), _)), 10.seconds)

      verify(session, timeout(10000)).sendMessage(new TextMessage("comments 5 2"))
      verify(session, times(1)).sendMessage(any[TextMessage]())
    finally
      stopHub(hub)

  test("subscribe without missed comments sends nothing"):
    val hub = startHub("missed-comments-empty-test")
    try
      given Scheduler = hub.scheduler

      val session = mockSession("empty")

      Await.result(hub.ask(RealtimeEventHub.SessionStarted(session, None, _)), 10.seconds)
      Await.result(hub.ask(RealtimeEventHub.SubscribeTopic(session, 1, Vector.empty, _)), 10.seconds)

      verify(session, never()).sendMessage(any[TextMessage]())
    finally
      stopHub(hub)

  test("live comment duplicated in backlog is suppressed"):
    val hub = startHub("missed-comments-duplicate-test")
    try
      given Scheduler = hub.scheduler

      val session = mockSession("duplicate")

      Await.result(hub.ask(RealtimeEventHub.SessionStarted(session, None, _)), 10.seconds)
      Await.result(hub.ask(RealtimeEventHub.SubscribeTopic(session, 1, Vector(5, 6), _)), 10.seconds)

      verify(session, timeout(10000)).sendMessage(new TextMessage("comments 5 2"))

      hub ! RealtimeEventHub.NewComment(1, 6)
      hub ! RealtimeEventHub.NewComment(1, 7)

      verify(session, timeout(10000)).sendMessage(new TextMessage("comment 7"))
      verify(session, never()).sendMessage(new TextMessage("comment 6"))
      verify(session, times(2)).sendMessage(any[TextMessage]())
    finally
      stopHub(hub)
