package ru.org.linux.tag

import scala.collection.JavaConversions._
import scala.beans.BeanProperty

case class TagRef(@BeanProperty name:String, @BeanProperty url:Option[String]) extends Ordered[TagRef] {
  def compare(that: TagRef): Int = name.compareTo(that.name)
}

object TagRef {
  def names(list: Seq[TagRef]):String = list.map(_.name).mkString(",")

  def names(list:java.util.List[TagRef]):String = names(list.toSeq)
}
