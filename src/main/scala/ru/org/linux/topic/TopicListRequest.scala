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

case class TopicListRequest(offset: Int, yearMonth: Option[(Int, Int)]) {
  def getMonth = yearMonth.map(_._2)
  def getYear = yearMonth.map(_._1)
}

object TopicListRequest {
  def ofOffset(offset: Int) = TopicListRequest(TopicListService.fixOffset(offset), None)

  def orYearMonth(year: Int, month: Int) = TopicListRequest(0, Some((year, month)))
}