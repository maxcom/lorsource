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

sealed trait Permission[+T]

sealed trait RestrictionChain[+T] extends PermissionChain[T]:
  def restrict(condition: => Boolean, reason: String): RestrictionChain[T]
  def restrict[K](permission: => RestrictionChain[K]): RestrictionChain[K]

sealed trait PermissionChain[+T]:
  def permit(condition: => Boolean): PermissionChain[T]
  def restrict(condition: => Boolean, reason: String): PermissionChain[T]

case class Unrestricted[+T](v: T) extends PermissionChain[T] with RestrictionChain[T]:
  override def permit(condition: => Boolean): PermissionChain[T] =
    if condition then
      Permitted(v)
    else
      this

  override def restrict(condition: => Boolean, reason: String): RestrictionChain[T] =
    if condition then
      Restricted(reason)
    else
      this

  def restrict[K](permission: => RestrictionChain[K]): RestrictionChain[K] =
    permission match
      case u@Unrestricted(_) => u
      case r: Restricted => r

object Unrestricted:
  val unit: Unrestricted[Unit] = Unrestricted[Unit](())

case class Permitted[+T](v: T) extends PermissionChain[T] with Permission[T]:
  override def permit(condition: => Boolean): Permitted[T] = this
  override def restrict(condition: => Boolean, reason: String): Permitted[T] = this

case class Restricted(reason: String)
    extends PermissionChain[Nothing]
    with Permission[Nothing]
    with RestrictionChain[Nothing]:
  override def permit(condition: => Boolean): Restricted = this
  override def restrict(condition: => Boolean, reason: String): Restricted = this
  override def restrict[K](permission: => RestrictionChain[K]): RestrictionChain[K] = this

extension [T](p: PermissionChain[T])
  def seal: Permission[T] =
    p match
      case Unrestricted(v) =>
        Permitted(v)
      case p: Permitted[T] =>
        p
      case r: Restricted =>
        r

extension [T](p: RestrictionChain[T])
  def seal: Permission[T] =
    p match
      case Unrestricted(v) =>
        Permitted(v)
      case r: Restricted =>
        r

  def map[K](f: T => K): RestrictionChain[K] =
    p match
      case Unrestricted(v) => Unrestricted(f(v))
      case Restricted(r) => Restricted(r)

object Permission:
  extension [T](p: Permission[T])
    def permitted: Boolean =
      p match
        case Permitted(_) =>
          true
        case Restricted(_) =>
          false

    def restricted: Boolean = !permitted

    def checkOrThrow(prefix: String = "Ограничение"): Unit =
      p match
        case Restricted(reason) =>
          throw new AccessViolationException(s"$prefix: $reason")
        case _ =>

    def checkOrThrowCtx[V](prefix: String = "Ограничение")(f: T => V): V =
      p match
        case Restricted(reason) =>
          throw new AccessViolationException(s"$prefix: $reason")
        case Permitted(v) =>
          f(v)

    def checkOrError(b: Errors, prefix: String = "Ограничение"): Unit =
      p match
        case Restricted(reason) =>
          b.reject(null, s"$prefix: $reason")
        case _ =>

    def checkOrErrorCtx[V](b: Errors, denyValue: T, prefix: String = "Ограничение")(f: T => V): V =
      p match
        case Restricted(reason) =>
          b.reject(null, s"$prefix: $reason")
          f(denyValue)
        case Permitted(v) =>
          f(v)

    def reason: String =
      p match
        case Restricted(reason) =>
          reason
        case _ =>
          ""
