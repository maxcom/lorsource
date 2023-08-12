/*
 * Copyright 1998-2023 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package ru.org.linux.topic

import ru.org.linux.topic.TopicListController.ForumFilter

import scala.beans.BeanProperty

case class TopicListRequest(@BeanProperty offset: Int, yearMonth: Option[(Int, Int)], filter: Option[ForumFilter]) {
  def getMonth: Option[Int] = yearMonth.map(_._2)
  def getYear: Option[Int] = yearMonth.map(_._1)

  // for jsp
  def getFilter = filter.map(_.id).getOrElse("")
}

object TopicListRequest {
  def ofOffset(offset: Int): TopicListRequest = TopicListRequest(TopicListService.fixOffset(offset), None, None)
  def orYearMonth(year: Int, month: Int): TopicListRequest = TopicListRequest(0, Some((year, month)), None)
}