package ru.org.linux.edithistory

import java.util.Date

case class EditInfoSummary(
  editCount:Int, // > 0
  editdate:Date,
  editor:Int
)

object EditInfoSummary {
  // TODO fetch brief info from database
  def apply(editCount:Int, info:BriefEditInfo) = new EditInfoSummary(editCount, info.editdate, info.editor)
}

case class BriefEditInfo(
  editdate:Date,
  editor:Int
)
