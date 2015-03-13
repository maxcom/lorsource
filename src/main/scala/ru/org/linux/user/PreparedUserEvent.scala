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