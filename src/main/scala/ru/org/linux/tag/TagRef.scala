package ru.org.linux.tag

import scala.collection.JavaConversions._

case class TagRef(name:String, url:Option[String])

object TagRef {
  def names(list: Seq[TagRef]):String = list.map(_.name).mkString(",")

  def names(list:java.util.List[TagRef]):String = names(list.toSeq)
}
