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

import org.springframework.validation.BindingResult
import ru.org.linux.auth.AccessViolationException

sealed trait Permission

sealed trait PermissionChain:
  def permit(condition: => Boolean): PermissionChain
  def restrict(condition: => Boolean, reason: String): PermissionChain
  def restrict(permission: => Unrestricted.type | Restricted): PermissionChain

case object Unrestricted extends PermissionChain:
  override def permit(condition: => Boolean): PermissionChain =
    if condition then
      Permitted
    else
      this

  override def restrict(condition: => Boolean, reason: String): Unrestricted.type | Restricted =
    if condition then
      Restricted(reason)
    else
      this

  override def restrict(permission: => Unrestricted.type | Restricted): Unrestricted.type | Restricted =
    permission match
      case Unrestricted => this
      case r: Restricted => r

case object Permitted extends PermissionChain with Permission:
  override def permit(condition: => Boolean): Permitted.type = this
  override def restrict(condition: => Boolean, reason: String): Permitted.type = this
  override def restrict(permission: => Unrestricted.type | Restricted): Permitted.type = this

case class Restricted(reason: String) extends PermissionChain with Permission:
  override def permit(condition: => Boolean): Restricted = this
  override def restrict(condition: => Boolean, reason: String): Restricted = this
  override def restrict(permission: => Unrestricted.type | Restricted): Restricted = this

extension (p: PermissionChain)
  def seal: Permission =
    p match
      case Unrestricted =>
        Permitted
      case Permitted =>
        Permitted
      case r: Restricted =>
        r

object Permission:
  extension (p: Permission)
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
        case _ =>

    def checkOrError(b: BindingResult, prefix: String = "Ограничение"): Unit =
      p match
        case Restricted(reason) =>
          b.reject(null, s"$prefix: $reason")
        case _ =>

    def reason: String =
      p match
        case Restricted(reason) =>
          reason
        case _ =>
          ""
