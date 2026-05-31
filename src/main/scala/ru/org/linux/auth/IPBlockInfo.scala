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

package ru.org.linux.auth

import java.sql.Timestamp
import java.util.Date
import javax.annotation.Nullable
import scala.beans.BeanProperty
import scala.beans.BooleanBeanProperty
import scalikejdbc.WrappedResultSet

case class IPBlockInfo(
  @BooleanBeanProperty initialized: Boolean,
  @BeanProperty ip: String,
  @BeanProperty @Nullable reason: String,
  @BeanProperty @Nullable banDate: Timestamp,
  @BeanProperty @Nullable originalDate: Timestamp,
  @BeanProperty moderator: Int,
  allowPosting: Boolean,
  @BooleanBeanProperty captchaRequired: Boolean
) {
  def isBlocked: Boolean = initialized && (banDate == null || banDate.after(new Date))

  def isAllowRegisteredPosting: Boolean = !isBlocked || allowPosting
}

object IPBlockInfo {
  def apply(ip: String): IPBlockInfo = IPBlockInfo(
    initialized = false,
    ip = ip,
    reason = null,
    banDate = null,
    originalDate = null,
    moderator = 0,
    allowPosting = false,
    captchaRequired = false)

  def fromWrappedResultSet(rs: WrappedResultSet): IPBlockInfo = IPBlockInfo(
    initialized = true,
    ip = rs.string("ip"),
    reason = rs.stringOpt("reason").orNull,
    banDate = rs.timestampOpt("ban_date").orNull,
    originalDate = rs.timestampOpt("date").orNull,
    moderator = rs.int("mod_id"),
    allowPosting = rs.boolean("allow_posting"),
    captchaRequired = rs.boolean("captcha_required"))
}
