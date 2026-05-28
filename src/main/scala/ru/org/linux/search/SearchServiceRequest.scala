/*
 * Copyright 1998-2026 Linux.org.ru
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

package ru.org.linux.search

import ru.org.linux.search.SearchEnums.{SearchInterval, SearchRange}
import ru.org.linux.user.User

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.{Instant, ZoneId}
import java.time.temporal.ChronoUnit
import scala.beans.{BeanProperty, BooleanBeanProperty}

class SearchServiceRequest:
  @BeanProperty
  var q: String = ""
  @BooleanBeanProperty
  var usertopic: Boolean = false
  @BeanProperty
  var user: User = null
  @BeanProperty
  var section: String = null
  @BeanProperty
  var sort: SearchOrder = SearchOrder.Relevance
  @BeanProperty
  var group: String = _
  @BeanProperty
  var interval: SearchInterval = SearchInterval.ALL
  @BeanProperty
  var range: SearchRange = SearchRange.ALL
  @BeanProperty
  var offset: Int = 0
  @BeanProperty
  var dt: Long = 0L

  def isInitial: Boolean = q.isEmpty && user == null && !isDateSelected

  def isDateSelected: Boolean = dt > 0

  def atEndOfDaySelected(tz: ZoneId): Long =
    Instant.ofEpochMilli(dt).atZone(tz).plusDays(1).truncatedTo(ChronoUnit.DAYS).toInstant.toEpochMilli

  def atStartOfDaySelected(tz: ZoneId): Long =
    Instant.ofEpochMilli(dt).atZone(tz).truncatedTo(ChronoUnit.DAYS).toInstant.toEpochMilli

  def getQuery(newOffset: Int): String =
    val params = Seq.newBuilder[(String, String)]

    if q != null && q.nonEmpty then
      params += (("q", q))
      params += (("oldQ", q))

    if range != SearchRange.ALL then
      params += (("range", range.toString))

    if interval != SearchInterval.ALL then
      params += (("interval", interval.toString))

    if user != null then
      params += (("user", user.nick))

    if usertopic then
      params += (("usertopic", "true"))

    if sort != SearchOrder.Relevance then
      params += (("sort", sort.id))

    if section != null && section.nonEmpty then
      params += (("section", section))

    if group != null then
      params += (("group", group))

    if newOffset != 0 then
      params += (("offset", newOffset.toString))

    buildParams(params.result())

  private def buildParams(params: Seq[(String, String)]): String =
    params
      .map { (k, v) =>
        s"${URLEncoder.encode(k, StandardCharsets.UTF_8)}=${URLEncoder.encode(v, StandardCharsets.UTF_8)}"
      }
      .mkString("&")
