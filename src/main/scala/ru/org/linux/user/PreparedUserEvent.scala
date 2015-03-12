package ru.org.linux.user

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
  tags: Seq[String]
) {
  def getAuthor = commentAuthor getOrElse topicAuthor

  def getMessageText = messageText.orNull

  def getBonus = bonus.getOrElse(0)

  def getTags:java.util.List[String] = tags.asJava

  def getLink:String = {
    if (event.getType==UserEventFilterEnum.DELETED) {
      s"view-message.jsp?msgid=${event.getTopicId}"
    } else {
      if (event.getCid>0) {
        s"jump-message.jsp?msgid=${event.getTopicId}&cid=${event.getCid}"
      } else {
        s"jump-message.jsp?msgid=${event.getTopicId}"
      }
    }
  }
}