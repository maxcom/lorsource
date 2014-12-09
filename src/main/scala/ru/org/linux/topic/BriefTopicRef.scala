package ru.org.linux.topic

import scala.beans.BeanProperty

case class BriefTopicRef(
  @BeanProperty url:String,
  @BeanProperty title:String,
  @BeanProperty commentCount:Int,
  @BeanProperty group:Option[String]
)

object BriefTopicRef {
  def apply(url:String, title:String, commentCount:Int, group:String):BriefTopicRef = 
    BriefTopicRef(url, title, commentCount, Some(group))

  def apply(url:String, title:String, commentCount:Int):BriefTopicRef =
    BriefTopicRef(url, title, commentCount, None)
  
}