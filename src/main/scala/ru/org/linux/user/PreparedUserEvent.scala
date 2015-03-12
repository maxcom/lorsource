package ru.org.linux.user

import ru.org.linux.group.Group

import scala.beans.BeanProperty

case class PreparedUserEvent(
  @BeanProperty event: UserEvent,
  messageText: Option[String],
  topicAuthor: User,
  commentAuthor: Option[User],
  bonus: Option[Int],
  @BeanProperty group: Group
) {
  def getAuthor = commentAuthor getOrElse topicAuthor

  def getMessageText = messageText.orNull

  def getBonus = bonus.getOrElse(0)
}