package ru.org.linux.topic

import java.util.Date

case class PreparedEditInfoSummary(
  lastEditor: String,
  editCount: Int,
  lastEditDate: Date
)