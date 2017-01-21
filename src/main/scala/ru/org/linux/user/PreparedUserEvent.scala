/*
 * Copyright 1998-2017 Linux.org.ru
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

import ru.org.linux.group.Group
import ru.org.linux.section.Section

import scala.beans.BeanProperty
import scala.collection.JavaConverters._

case class PreparedUserEvent(
  @BeanProperty event: UserEvent,
  messageText: Option[String],
  topicAuthor: User,
  commentAuthor: Option[User],
  bonus: Option[Int],
  @BeanProperty section: Section,
  group: Group,
  tags: Seq[String]
) {
  def getAuthor = commentAuthor getOrElse topicAuthor

  def getMessageText = messageText.orNull

  def getBonus = bonus.getOrElse(0)

  def getTags:java.util.List[String] = tags.asJava

  def getLink:String = {
    if (event.getType==UserEventFilterEnum.DELETED) {
      s"${group.getUrl}${event.getTopicId}"
    } else {
      if (event.getCid>0) {
        s"${group.getUrl}${event.getTopicId}?cid=${event.getCid}"
      } else {
        s"${group.getUrl}${event.getTopicId}"
      }
    }
  }
}
