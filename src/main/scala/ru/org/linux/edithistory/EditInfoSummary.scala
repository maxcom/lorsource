package ru.org.linux.edithistory

import scala.beans.BeanProperty

case class EditInfoSummary(
  @BeanProperty editCount:Int,
  @BeanProperty lastEditInfo:Option[EditHistoryDto] // TODO full Dto is heavy, replace by brief info
)

object EditInfoSummary {
  val NoEdits = EditInfoSummary(0, None)

  def apply(editCount:Int, info:EditHistoryDto) = new EditInfoSummary(editCount, Some(info))
}
