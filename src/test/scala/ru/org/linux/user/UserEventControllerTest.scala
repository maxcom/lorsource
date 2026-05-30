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
package ru.org.linux.user

import org.junit.Assert.{assertFalse, assertTrue}
import org.junit.Test

import java.sql.Timestamp

class UserEventControllerTest:
  private def event(id: Int, eventType: UserEventFilterEnum, unread: Boolean, topicId: Int, commentId: Int): UserEvent =
    UserEvent(
      cid = commentId,
      commentAuthor = 1,
      groupId = 1,
      subj = "subject",
      topicId = topicId,
      eventType = eventType,
      eventMessage = "",
      eventDate = new Timestamp(0),
      unread = unread,
      topicAuthor = 1,
      id = id,
      originUserId = 2,
      reaction = "+",
      closedWarning = false,
      userId = 1
    )

  @Test
  def testValidReactionRange(): Unit =
    val firstEvent = event(id = 10, UserEventFilterEnum.REACTION, unread = true, topicId = 100, commentId = 200)
    val lastEvent = event(id = 12, UserEventFilterEnum.REACTION, unread = true, topicId = 100, commentId = 200)

    assertTrue(UserEventController.isValidClickRange(firstEvent, lastEvent))

  @Test
  def testInvalidReactionRangeForDifferentComment(): Unit =
    val firstEvent = event(id = 10, UserEventFilterEnum.REACTION, unread = true, topicId = 100, commentId = 200)
    val lastEvent = event(id = 12, UserEventFilterEnum.REACTION, unread = true, topicId = 100, commentId = 201)

    assertFalse(UserEventController.isValidClickRange(firstEvent, lastEvent))

  @Test
  def testInvalidSingleEventRange(): Unit =
    val firstEvent = event(id = 10, UserEventFilterEnum.TAG, unread = true, topicId = 100, commentId = 0)
    val lastEvent = event(id = 12, UserEventFilterEnum.TAG, unread = true, topicId = 100, commentId = 0)

    assertFalse(UserEventController.isValidClickRange(firstEvent, lastEvent))
