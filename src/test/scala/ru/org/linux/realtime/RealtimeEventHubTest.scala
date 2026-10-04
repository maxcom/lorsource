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
import org.mockito.Mockito.{mock, never, timeout, verify, when}
import org.springframework.web.socket.WebSocketSession
import ru.org.linux.user.IgnoreListDao

import scala.concurrent.Await
import scala.concurrent.duration.*

/** Юнит-тесты закрытия websocket-соединений пользователя ([[RealtimeEventHub.CloseUserSessions]]). */
class RealtimeEventHubTest extends FunSuite:
  private given Timeout: Timeout = 10.seconds

  private def startHub(name: String): ActorSystem[RealtimeEventHub.Protocol] =
    ActorSystem(RealtimeEventHub.behavior(mock(classOf[IgnoreListDao])), name)

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
