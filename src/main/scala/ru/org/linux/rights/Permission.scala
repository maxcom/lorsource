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

package ru.org.linux.rights

import org.springframework.validation.Errors
import ru.org.linux.auth.AccessViolationException

sealed trait Permission

sealed trait RestrictionChain extends PermissionChain:
  def restrict(condition: => Boolean, reason: String): RestrictionChain
  def restrict(permission: => RestrictionChain): RestrictionChain

sealed trait PermissionChain:
  def permit(condition: => Boolean): PermissionChain
  def restrict(condition: => Boolean, reason: String): PermissionChain

case object Unrestricted extends PermissionChain with RestrictionChain:
  override def permit(condition: => Boolean): PermissionChain =
    if condition then
      Permitted
    else
      this

  override def restrict(condition: => Boolean, reason: String): RestrictionChain =
    if condition then
      Restricted(reason)
    else
      this

  override def restrict(permission: => RestrictionChain): RestrictionChain =
    permission match
      case Unrestricted =>
        Unrestricted
      case r: Restricted =>
        r

case object Permitted extends PermissionChain with Permission:
  override def permit(condition: => Boolean): Permitted.type = this
  override def restrict(condition: => Boolean, reason: String): Permitted.type = this

case class Restricted(reason: String) extends PermissionChain with Permission with RestrictionChain:
  override def permit(condition: => Boolean): Restricted = this
  override def restrict(condition: => Boolean, reason: String): Restricted = this
  override def restrict(permission: => RestrictionChain): RestrictionChain = this

extension (p: PermissionChain)
  def seal: Permission =
    p match
      case Unrestricted | Permitted =>
        Permitted
      case r: Restricted =>
        r

object Permission:
  extension [T](p: Permission)
    def permitted: Boolean =
      p match
        case Permitted =>
          true
        case Restricted(_) =>
          false

    def restricted: Boolean = !permitted

    def checkOrThrow(prefix: String = "Ограничение"): Unit =
      p match
        case Restricted(reason) =>
          throw new AccessViolationException(s"$prefix: $reason")
        case Permitted =>

    def checkOrError(b: Errors, prefix: String = "Ограничение"): Unit =
      p match
        case Restricted(reason) =>
          b.reject(null, s"$prefix: $reason")
        case Permitted =>

    def reason: String =
      p match
        case Restricted(reason) =>
          reason
        case Permitted =>
          ""
