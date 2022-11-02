/*
 * Copyright 1998-2022 Linux.org.ru
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
package ru.org.linux.site

import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import java.util.Locale

object DateFormats {
  private val RussianLocale = new Locale("ru")
  private val Default = DateTimeFormat.forPattern("dd.MM.yy HH:mm:ss z").withLocale(RussianLocale)
  private val Short = DateTimeFormat.forPattern("dd.MM.yy HH:mm").withLocale(RussianLocale)
  private val Time = DateTimeFormat.forPattern("HH:mm").withLocale(RussianLocale)
  private val Date = DateTimeFormat.forPattern("dd.MM.yy").withLocale(RussianLocale)

  val Iso8601: DateTimeFormatter = ISODateTimeFormat.dateTime
  val Rfc822: DateTimeFormatter = DateTimeFormat.forPattern("EEE, d MMM yyyy HH:mm:ss Z").withLocale(Locale.US)

  def getDefault(tz: DateTimeZone): DateTimeFormatter = Default.withZone(tz)

  def getShort(tz: DateTimeZone): DateTimeFormatter = Short.withZone(tz)

  def time(tz: DateTimeZone): DateTimeFormatter = Time.withZone(tz)

  def date(tz: DateTimeZone): DateTimeFormatter = Date.withZone(tz)
}
