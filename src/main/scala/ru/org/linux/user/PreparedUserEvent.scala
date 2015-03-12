package ru.org.linux.user

import ru.org.linux.group.Group

import scala.beans.BeanProperty

case class PreparedUserEvent(
  @BeanProperty event: UserEvent,
  @BeanProperty messageText: String,
  topicAuthor: User,
  commentAuthor: User,
  @BeanProperty bonus: Int,
  @BeanProperty group: Group
) {
  @BeanProperty
  val author = Option(commentAuthor) getOrElse topicAuthor
}