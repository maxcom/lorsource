/*
 * Copyright 1998-2024 Linux.org.ru
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

import java.sql.Timestamp
import scala.beans.{BeanProperty, BooleanBeanProperty}

/**
 * Элемент списка уведомлений
 */
case class UserEvent(@BeanProperty cid: Int, commentAuthor: Int, groupId: Int,
                     @BeanProperty subj: String, @BeanProperty topicId: Int,
                     @BeanProperty eventType: UserEventFilterEnum, @BeanProperty eventMessage: String,
                     @BeanProperty eventDate: Timestamp, @BeanProperty unread: Boolean, topicAuthor: Int, id: Int,
                     originUserId: Int, @BeanProperty reaction: String, @BooleanBeanProperty closedWarning: Boolean) {
  def isComment: Boolean = cid > 0
}

object UserEvent {
  val NoReaction = "X"
}