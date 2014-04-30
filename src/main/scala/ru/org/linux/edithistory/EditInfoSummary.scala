package ru.org.linux.edithistory

import java.util.Date

case class EditInfoSummary(
  editCount:Int,
  lastEditInfo:Option[BriefEditInfo]
)

object EditInfoSummary {
  val NoEdits = EditInfoSummary(0, None)

  // TODO fetch brief info from database
  def apply(editCount:Int, info:BriefEditInfo) = new EditInfoSummary(editCount, Some(info))
}

case class BriefEditInfo(
  editdate:Date,
  editor:Int
)
